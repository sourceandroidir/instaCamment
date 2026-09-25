package com.example.data.remote

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ExtractedCommentUser(
    val username: String,
    val userId: String = "",
    val displayName: String = "",
    val profileUrl: String = "",
    val profilePicUrl: String = "",
    val commentText: String = ""
)

data class PostMetadata(
    val caption: String = "",
    val thumbnailUrl: String = "",
    val videoUrl: String = "",
    val isVideo: Boolean = false,
    val authorUsername: String = "",
    val commentCount: Int = 0
)

data class ExtractionResult(
    val mediaId: String,
    val users: List<ExtractedCommentUser>,
    val duplicateCount: Int = 0,
    val hasNextPage: Boolean = false,
    val endCursor: String? = null,
    val postMetadata: PostMetadata = PostMetadata()
)

class InstagramParser {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val excludedKeywords = setOf(
        "instagram", "explore", "reels", "direct", "stories", "accounts",
        "developer", "about", "help", "press", "api", "jobs", "privacy",
        "terms", "locations", "meta", "login", "signup", "settings",
        "p", "reel", "tv", "tagged", "saved", "following", "followers"
    )

    fun extractShortcode(url: String): String {
        val cleanUrl = url.trim()
        val regex = "instagram\\.com/(?:p|reel|tv)/([^/?#&]+)".toRegex()
        val match = regex.find(cleanUrl)
        return match?.groupValues?.get(1)
            ?: throw IllegalArgumentException("لینک معتبر اینستاگرام یافت نشد (باید شامل /p/ یا /reel/ باشد)")
    }

    /**
     * Converts an Instagram base64-like shortcode to numeric media ID
     */
    fun shortcodeToMediaId(shortcode: String): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        var id = BigInteger.ZERO
        for (c in shortcode) {
            val index = alphabet.indexOf(c)
            if (index >= 0) {
                id = id.multiply(BigInteger.valueOf(64)).add(BigInteger.valueOf(index.toLong()))
            }
        }
        return if (id > BigInteger.ZERO) id.toString() else shortcode
    }

    suspend fun fetchCommentsFromWebPage(
        shortcode: String,
        cursor: String? = null,
        cookies: String? = null,
        csrfToken: String? = null
    ): ExtractionResult {
        var mediaId = shortcodeToMediaId(shortcode)
        val effectiveCsrf = if (!csrfToken.isNullOrBlank()) {
            csrfToken
        } else if (!cookies.isNullOrBlank()) {
            extractCookieValue(cookies, "csrftoken")
        } else ""

        var postMetadata = PostMetadata()

        // 1. First fetch post page for metadata and exact numeric media_id (only on first page)
        if (cursor == null) {
            try {
                val pageUrl = "https://www.instagram.com/p/$shortcode/"
                val pageReq = Request.Builder()
                    .url(pageUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                if (!cookies.isNullOrBlank()) pageReq.header("Cookie", cookies)
                if (effectiveCsrf.isNotBlank()) pageReq.header("X-CSRFToken", effectiveCsrf)

                val htmlResp = client.newCall(pageReq.build()).execute()
                if (htmlResp.isSuccessful) {
                    val html = htmlResp.body?.string() ?: ""
                    postMetadata = extractMetadataFromHtml(html)

                    // Find exact numeric media ID from HTML
                    val idMatcher = Pattern.compile("(?:instagram://media\\?id=|\"media_id\"\\s*:\\s*\"?|\"id\"\\s*:\\s*\")(\\d{10,})").matcher(html)
                    if (idMatcher.find()) {
                        val foundId = idMatcher.group(1)
                        if (!foundId.isNullOrBlank()) {
                            mediaId = foundId
                        }
                    }

                    // Check if initial comments are embedded in page scripts
                    val embeddedResult = extractCommentsFromHtmlScripts(html, mediaId)
                    if (embeddedResult.users.isNotEmpty()) {
                        val merged = if (postMetadata.thumbnailUrl.isNotBlank()) postMetadata else embeddedResult.postMetadata
                        return embeddedResult.copy(postMetadata = merged)
                    }
                }
            } catch (_: Exception) {}
        }

        var lastError: String? = null

        // 2. STRATEGY 1: Polaris GraphQL (doc_id = 28169471862682868)
        try {
            val res1 = fetchViaPolarisGraphQL(
                shortcode = shortcode,
                mediaId = mediaId,
                cursor = cursor,
                cookies = cookies,
                csrfToken = effectiveCsrf,
                docId = "28169471862682868"
            )
            if (res1.users.isNotEmpty()) {
                val merged = if (postMetadata.thumbnailUrl.isNotBlank()) postMetadata else res1.postMetadata
                return res1.copy(postMetadata = merged)
            }
        } catch (e: Exception) {
            lastError = e.message
        }

        // 3. STRATEGY 2: Polaris GraphQL alternative doc_id (17888487470747430)
        try {
            val res2 = fetchViaPolarisGraphQL(
                shortcode = shortcode,
                mediaId = mediaId,
                cursor = cursor,
                cookies = cookies,
                csrfToken = effectiveCsrf,
                docId = "17888487470747430"
            )
            if (res2.users.isNotEmpty()) {
                val merged = if (postMetadata.thumbnailUrl.isNotBlank()) postMetadata else res2.postMetadata
                return res2.copy(postMetadata = merged)
            }
        } catch (e: Exception) {
            lastError = e.message
        }

        // 4. STRATEGY 3: REST API v1 Media Comments
        try {
            val res3 = fetchViaV1CommentsApi(
                shortcode = shortcode,
                mediaId = mediaId,
                cursor = cursor,
                cookies = cookies,
                csrfToken = effectiveCsrf
            )
            if (res3.users.isNotEmpty()) {
                val merged = if (postMetadata.thumbnailUrl.isNotBlank()) postMetadata else res3.postMetadata
                return res3.copy(postMetadata = merged)
            }
        } catch (e: Exception) {
            lastError = e.message
        }

        // 5. STRATEGY 4: Legacy query_hash GraphQL
        if (!cookies.isNullOrBlank()) {
            try {
                val res4 = fetchViaLegacyGraphqlQuery(shortcode, cursor, cookies, effectiveCsrf)
                if (res4.users.isNotEmpty()) {
                    val merged = if (postMetadata.thumbnailUrl.isNotBlank()) postMetadata else res4.postMetadata
                    return res4.copy(postMetadata = merged)
                }
            } catch (e: Exception) {
                lastError = e.message
            }
        }

        // If no comments fetched and cookies were empty, explain why
        if (cookies.isNullOrBlank()) {
            throw IllegalStateException(
                "برای استخراج کامل کامنت‌ها به کوکی نشست فعال نیاز است. لطفاً ابتدا از بخش «حساب‌ها» با مرورگر داخلی وارد حساب اینستاگرام شوید."
            )
        }

        if (lastError != null && !lastError.contains("200")) {
            throw IllegalStateException("خطا در ارتباط با وب‌سرویس کامنت‌های اینستاگرام: $lastError")
        }

        return ExtractionResult(
            mediaId = mediaId,
            users = emptyList(),
            duplicateCount = 0,
            hasNextPage = false,
            endCursor = null,
            postMetadata = postMetadata
        )
    }

    private fun fetchViaPolarisGraphQL(
        shortcode: String,
        mediaId: String,
        cursor: String?,
        cookies: String?,
        csrfToken: String,
        docId: String
    ): ExtractionResult {
        val targetUrl = "https://www.instagram.com/api/graphql"
        val av = if (!cookies.isNullOrBlank()) extractCookieValue(cookies, "ds_user_id").ifEmpty { "0" } else "0"

        val variablesObj = JSONObject().apply {
            if (cursor.isNullOrBlank()) {
                put("after", JSONObject.NULL)
            } else {
                put("after", cursor)
            }
            put("before", JSONObject.NULL)
            put("first", 50)
            put("last", JSONObject.NULL)
            put("media_id", mediaId)
            put("sort_order", "popular")
            put("__relay_internal__pv__PolarisIsLoggedInrelayprovider", !cookies.isNullOrBlank())
        }

        val formBody = FormBody.Builder()
            .add("av", av)
            .add("__d", "www")
            .add("__user", "0")
            .add("__a", "1")
            .add("__req", "1")
            .add("__hs", "20721.HYP:instagram_web_pkg.2.1...0")
            .add("dpr", "1")
            .add("__ccg", "POOR")
            .add("__rev", "1048463508")
            .add("__s", "9nqryk:o9oxwd:cz08fj")
            .add("__hsi", "7689508215436639308")
            .add("__dyn", "7xeUjG1mxu1syaxG4Vp41twpUnwgU7SbzEdF8vyUco2qwJyEiw50x609vCwjE1EEc87m0yE462mcw5Mx62G5UswoEcE7O2l0Fwqo5W1yw9O1lwlE-U2zxe2GewGw9a361qw8Xxm16wa-0oa2-azo7u3C2u2J0bS1LyUaUbGxK3R08-269wr84-6o5p389oed6goK10xKi2K7E5y4U7a0EoKmUhw5nyFEaVE4616wAwj83KwRzkbwhU")
            .add("__csr", "gT7M8Qqx5h75iW4MG4RhEHh6iDleZVOlqTexcTAGBOnpJHuN2QvypiWLJaGmFF4LJKqmEymuiQrB9VQj8yNrRGPsLFiFSyp7nZRKV8WAVAqXjOGqFqFpohyFQAgwsyV3aieKF9bJlxiaDBDGp2Ugz98twxxeiqmbxbx7xWdGiF-XgmBy9eEkyoKUlBUGdAzECiXxybxZoTVojzEpxicKXDVVEhhUCUG9yUuw70z8eVE760baw08qy00XQo29w1WC2Ne4ra4gMcAa05Xy09WcAa0vi04u20bC0D43q260HS0ou8ylB7w8-1vw115Cyo7yto7a0Gbh7hy6g0o3y42e4E06Na02avw4ww0Ffw2EU1Bo")
            .add("__comet_req", "7")
            .add("fb_api_caller_class", "RelayModern")
            .add("fb_api_req_friendly_name", "PolarisPostCommentsPaginationQuery")
            .add("server_timestamps", "true")
            .add("doc_id", docId)
            .add("variables", variablesObj.toString())
            .add("lsd", "d010_L_p_MYerQ3HlG9MCB")
            .add("jazoest", "26106")
            .build()

        val reqBuilder = Request.Builder()
            .url(targetUrl)
            .post(formBody)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
            .header("Accept", "*/*")
            .header("X-FB-Friendly-Name", "PolarisPostCommentsPaginationQuery")
            .header("X-CSRFToken", csrfToken)
            .header("X-IG-App-ID", "936619743392459")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("X-ASBD-ID", "129477")
            .header("Origin", "https://www.instagram.com")
            .header("Referer", "https://www.instagram.com/p/$shortcode/")

        if (!cookies.isNullOrBlank()) {
            reqBuilder.header("Cookie", cookies)
        }

        val resp = client.newCall(reqBuilder.build()).execute()
        val rawBody = resp.body?.string() ?: ""

        if (!resp.isSuccessful || rawBody.isBlank()) {
            throw IllegalStateException("GraphQL HTTP error: ${resp.code}")
        }

        return parseCleanedJson(rawBody, mediaId)
    }

    private fun fetchViaV1CommentsApi(
        shortcode: String,
        mediaId: String,
        cursor: String?,
        cookies: String?,
        csrfToken: String
    ): ExtractionResult {
        var url = "https://www.instagram.com/api/v1/media/$mediaId/comments/?can_support_threading=true&permalink_enabled=false"
        if (!cursor.isNullOrBlank()) {
            url += "&min_id=$cursor"
        }

        val reqBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
            .header("Accept", "application/json, text/plain, */*")
            .header("X-CSRFToken", csrfToken)
            .header("X-IG-App-ID", "936619743392459")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", "https://www.instagram.com/p/$shortcode/")

        if (!cookies.isNullOrBlank()) {
            reqBuilder.header("Cookie", cookies)
        }

        val resp = client.newCall(reqBuilder.build()).execute()
        val bodyStr = resp.body?.string() ?: ""

        if (!resp.isSuccessful || bodyStr.isBlank()) {
            throw IllegalStateException("v1 comments failed: ${resp.code}")
        }

        return parseCleanedJson(bodyStr, mediaId)
    }

    private fun fetchViaLegacyGraphqlQuery(
        shortcode: String,
        cursor: String?,
        cookies: String,
        csrfToken: String
    ): ExtractionResult {
        val variables = """{"shortcode":"$shortcode","first":50,"after":${if (cursor != null) "\"$cursor\"" else "null"}}"""
        val targetUrl = "https://www.instagram.com/graphql/query/?query_hash=b96016d71b80066d16e322fe03f837e6&variables=${java.net.URLEncoder.encode(variables, "UTF-8")}"

        val reqBuilder = Request.Builder()
            .url(targetUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
            .header("Accept", "*/*")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("X-IG-App-ID", "936619743392459")
            .header("Referer", "https://www.instagram.com/p/$shortcode/")
            .header("Cookie", cookies)
            .header("X-CSRFToken", csrfToken)

        val resp = client.newCall(reqBuilder.build()).execute()
        val body = resp.body?.string() ?: ""
        if (!resp.isSuccessful || body.isBlank()) {
            throw IllegalStateException("Legacy query failed: ${resp.code}")
        }
        return parseCleanedJson(body, shortcode)
    }

    /**
     * Strips "for (;;);" prefix and parses any Instagram JSON structure
     */
    private fun parseCleanedJson(rawJson: String, mediaId: String): ExtractionResult {
        var clean = rawJson.trim()
        if (clean.startsWith("for (;;);")) {
            clean = clean.substring(8).trim()
        }
        val firstBrace = clean.indexOf('{')
        val lastBrace = clean.lastIndexOf('}')
        if (firstBrace in 0 until lastBrace) {
            clean = clean.substring(firstBrace, lastBrace + 1)
        }

        val root = try {
            JSONObject(clean)
        } catch (_: Exception) {
            return ExtractionResult(mediaId = mediaId, users = emptyList())
        }

        val extractedUsers = mutableListOf<ExtractedCommentUser>()
        var hasNextPage = false
        var endCursor: String? = null

        // 1. Structure: data.xdt_api__v1__media__media_id__comments__connection
        val dataObj = root.optJSONObject("data") ?: root.optJSONObject("graphql")
        if (dataObj != null) {
            var connObj: JSONObject? = null
            val keys = dataObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                if (k.contains("comments__connection") || k == "shortcode_media" || k == "xdt_shortcode_media") {
                    connObj = dataObj.optJSONObject(k)
                    break
                }
            }

            if (connObj != null) {
                val edgeComments = connObj.optJSONObject("edge_media_to_parent_comment")
                    ?: connObj.optJSONObject("edge_media_to_comment")
                    ?: connObj

                val pageInfo = edgeComments.optJSONObject("page_info")
                if (pageInfo != null) {
                    hasNextPage = pageInfo.optBoolean("has_next_page", false)
                    endCursor = pageInfo.optString("end_cursor").takeIf { it.isNotBlank() }
                }

                val edges = edgeComments.optJSONArray("edges") ?: JSONArray()
                for (i in 0 until edges.length()) {
                    val edge = edges.optJSONObject(i) ?: continue
                    val node = edge.optJSONObject("node") ?: continue
                    val userObj = node.optJSONObject("user") ?: node.optJSONObject("owner") ?: continue

                    val username = userObj.optString("username", "").trim()
                    val userId = userObj.optString("pk").ifEmpty { userObj.optString("id") }.trim()
                    val text = node.optString("text", "")
                    val displayName = userObj.optString("full_name", username).ifEmpty { username }
                    val profilePic = cleanUrl(userObj.optString("profile_pic_url", ""))

                    if (username.length >= 2 && !excludedKeywords.contains(username.lowercase())) {
                        extractedUsers.add(
                            ExtractedCommentUser(
                                username = username,
                                userId = userId,
                                displayName = displayName,
                                profileUrl = "https://www.instagram.com/$username/",
                                profilePicUrl = profilePic,
                                commentText = text
                            )
                        )
                    }

                    // Also extract child comments/replies if present!
                    val childConn = node.optJSONObject("edge_threaded_comments")
                    val childEdges = childConn?.optJSONArray("edges")
                    if (childEdges != null) {
                        for (ci in 0 until childEdges.length()) {
                            val cNode = childEdges.optJSONObject(ci)?.optJSONObject("node") ?: continue
                            val cUser = cNode.optJSONObject("user") ?: cNode.optJSONObject("owner") ?: continue
                            val cUsername = cUser.optString("username", "").trim()
                            val cUserId = cUser.optString("pk").ifEmpty { cUser.optString("id") }.trim()
                            val cText = cNode.optString("text", "")
                            val cName = cUser.optString("full_name", cUsername).ifEmpty { cUsername }
                            val cPic = cleanUrl(cUser.optString("profile_pic_url", ""))

                            if (cUsername.length >= 2 && !excludedKeywords.contains(cUsername.lowercase())) {
                                extractedUsers.add(
                                    ExtractedCommentUser(
                                        username = cUsername,
                                        userId = cUserId,
                                        displayName = cName,
                                        profileUrl = "https://www.instagram.com/$cUsername/",
                                        profilePicUrl = cPic,
                                        commentText = cText
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Structure: v1 REST API { "comments": [ ... ], "has_more_comments": true }
        val commentsArr = root.optJSONArray("comments")
        if (commentsArr != null && commentsArr.length() > 0) {
            for (i in 0 until commentsArr.length()) {
                val cObj = commentsArr.optJSONObject(i) ?: continue
                val userObj = cObj.optJSONObject("user") ?: continue
                val username = userObj.optString("username", "").trim()
                val userId = userObj.optString("pk").ifEmpty { userObj.optString("id") }.trim()
                val text = cObj.optString("text", "")
                val displayName = userObj.optString("full_name", username).ifEmpty { username }
                val profilePic = cleanUrl(userObj.optString("profile_pic_url", ""))

                if (username.length >= 2 && !excludedKeywords.contains(username.lowercase())) {
                    extractedUsers.add(
                        ExtractedCommentUser(
                            username = username,
                            userId = userId,
                            displayName = displayName,
                            profileUrl = "https://www.instagram.com/$username/",
                            profilePicUrl = profilePic,
                            commentText = text
                        )
                    )
                }
            }
            hasNextPage = root.optBoolean("has_more_comments", false)
            endCursor = root.optString("next_min_id").ifEmpty { root.optString("next_max_id") }.takeIf { it.isNotBlank() }
        }

        // 3. Fallback: recursive scan if extractedUsers is still empty
        if (extractedUsers.isEmpty()) {
            extractUsersDeep(root, extractedUsers)
        }

        // Deduplicate comments on this page
        val uniqueMap = linkedMapOf<String, ExtractedCommentUser>()
        var duplicates = 0
        for (u in extractedUsers) {
            val k = u.username.lowercase()
            val existing = uniqueMap[k]
            if (existing == null) {
                uniqueMap[k] = u
            } else {
                duplicates++
                if (existing.userId.isBlank() && u.userId.isNotBlank()) {
                    uniqueMap[k] = u
                }
            }
        }

        return ExtractionResult(
            mediaId = mediaId,
            users = uniqueMap.values.toList(),
            duplicateCount = duplicates,
            hasNextPage = hasNextPage,
            endCursor = endCursor
        )
    }

    private fun extractCommentsFromHtmlScripts(html: String, mediaId: String): ExtractionResult {
        val scriptPattern = Pattern.compile("<script[^>]*type=[\"'](?:application|text)/(?:json|javascript)[\"'][^>]*>(.*?)</script>", Pattern.DOTALL)
        val matcher = scriptPattern.matcher(html)
        val allUsers = mutableListOf<ExtractedCommentUser>()

        while (matcher.find()) {
            val scriptContent = matcher.group(1) ?: continue
            if (scriptContent.contains("comments") || scriptContent.contains("xdt_api__v1") || scriptContent.contains("edge_media_to_parent_comment")) {
                val res = parseCleanedJson(scriptContent, mediaId)
                if (res.users.isNotEmpty()) {
                    allUsers.addAll(res.users)
                }
            }
        }

        val uniqueMap = linkedMapOf<String, ExtractedCommentUser>()
        var dups = 0
        for (u in allUsers) {
            val k = u.username.lowercase()
            if (uniqueMap.containsKey(k)) {
                dups++
            } else {
                uniqueMap[k] = u
            }
        }

        return ExtractionResult(
            mediaId = mediaId,
            users = uniqueMap.values.toList(),
            duplicateCount = dups,
            hasNextPage = false,
            endCursor = null
        )
    }

    private fun extractUsersDeep(obj: Any?, target: MutableList<ExtractedCommentUser>) {
        when (obj) {
            is JSONObject -> {
                if (obj.has("username") && (obj.has("id") || obj.has("pk"))) {
                    val username = obj.optString("username").trim()
                    val id = obj.optString("pk").ifEmpty { obj.optString("id") }.trim()
                    val pic = cleanUrl(obj.optString("profile_pic_url", ""))
                    if (username.length >= 2 && !excludedKeywords.contains(username.lowercase())) {
                        target.add(
                            ExtractedCommentUser(
                                username = username,
                                userId = id,
                                displayName = obj.optString("full_name", username),
                                profileUrl = "https://www.instagram.com/$username/",
                                profilePicUrl = pic,
                                commentText = obj.optString("text", "کامنت اینستاگرام")
                            )
                        )
                    }
                }
                val keys = obj.keys()
                while (keys.hasNext()) {
                    extractUsersDeep(obj.opt(keys.next()), target)
                }
            }
            is JSONArray -> {
                for (i in 0 until obj.length()) {
                    extractUsersDeep(obj.opt(i), target)
                }
            }
        }
    }

    private fun extractMetadataFromHtml(html: String): PostMetadata {
        var caption = ""
        var thumbnailUrl = ""
        var videoUrl = ""
        var isVideo = false
        var authorUsername = ""
        var commentCount = 0

        val ogImageMatch = Pattern.compile(
            "<meta[^>]*property=[\"']og:image[\"'][^>]*content=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
        ).matcher(html)
        if (ogImageMatch.find()) {
            thumbnailUrl = ogImageMatch.group(1)?.replace("&amp;", "&") ?: ""
        }

        val ogVideoMatch = Pattern.compile(
            "<meta[^>]*property=[\"']og:video[\"'][^>]*content=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
        ).matcher(html)
        if (ogVideoMatch.find()) {
            videoUrl = ogVideoMatch.group(1)?.replace("&amp;", "&") ?: ""
            isVideo = true
        }

        val ogDescMatch = Pattern.compile(
            "<meta[^>]*property=[\"']og:description[\"'][^>]*content=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
        ).matcher(html)
        if (ogDescMatch.find()) {
            val desc = ogDescMatch.group(1) ?: ""
            caption = desc.substringAfter(":", desc).trim().removePrefix("\"").removeSuffix("\"")
            val commentMatch = Pattern.compile("(\\d+)\\s+comments?", Pattern.CASE_INSENSITIVE).matcher(desc)
            if (commentMatch.find()) {
                commentCount = commentMatch.group(1)?.toIntOrNull() ?: 0
            }
        }

        val ogTitleMatch = Pattern.compile(
            "<meta[^>]*property=[\"']og:title[\"'][^>]*content=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
        ).matcher(html)
        if (ogTitleMatch.find()) {
            val title = ogTitleMatch.group(1) ?: ""
            authorUsername = title.substringBefore("on Instagram", title).replace("@", "").trim()
        }

        if (thumbnailUrl.isBlank()) {
            val displayUrlMatch = Pattern.compile("\"display_url\"\\s*:\\s*\"([^\"]+)\"").matcher(html)
            if (displayUrlMatch.find()) {
                thumbnailUrl = displayUrlMatch.group(1) ?: ""
            }
        }

        if (thumbnailUrl.isBlank()) {
            val thumbSrcMatch = Pattern.compile("\"thumbnail_src\"\\s*:\\s*\"([^\"]+)\"").matcher(html)
            if (thumbSrcMatch.find()) {
                thumbnailUrl = thumbSrcMatch.group(1) ?: ""
            }
        }

        if (videoUrl.isBlank()) {
            val vMatch = Pattern.compile("\"video_url\"\\s*:\\s*\"([^\"]+)\"").matcher(html)
            if (vMatch.find()) {
                videoUrl = vMatch.group(1) ?: ""
                isVideo = true
            }
        }

        return PostMetadata(
            caption = caption,
            thumbnailUrl = cleanUrl(thumbnailUrl),
            videoUrl = cleanUrl(videoUrl),
            isVideo = isVideo || videoUrl.isNotEmpty(),
            authorUsername = authorUsername,
            commentCount = commentCount
        )
    }

    private fun cleanUrl(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace("\\u0026", "&")
            .replace("&amp;", "&")
            .replace("\\/", "/")
            .replace("\"", "")
            .trim()
    }

    private fun extractCookieValue(cookieHeader: String, key: String): String {
        return cookieHeader.split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("$key=") }
            ?.removePrefix("$key=")
            ?: ""
    }
}
