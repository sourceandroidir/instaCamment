package com.example.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ExtractedCommentUser(
    val username: String,
    val userId: String = "",
    val displayName: String = "",
    val profileUrl: String = "",
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
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val excludedKeywords = setOf(
        "instagram", "explore", "reels", "direct", "stories", "accounts",
        "developer", "about", "help", "press", "api", "jobs", "privacy",
        "terms", "locations", "meta", "login", "signup", "settings"
    )

    fun extractShortcode(url: String): String {
        val cleanUrl = url.trim()
        val regex = "instagram\\.com/(?:p|reel|tv)/([^/?#&]+)".toRegex()
        val match = regex.find(cleanUrl)
        return match?.groupValues?.get(1)
            ?: throw IllegalArgumentException("لینک معتبر اینستاگرام یافت نشد (باید شامل /p/ یا /reel/ باشد)")
    }

    suspend fun fetchCommentsFromWebPage(
        shortcode: String,
        cursor: String? = null,
        cookies: String? = null,
        csrfToken: String? = null
    ): ExtractionResult {
        // Strategy 1: If cursor is provided or cookies are active, attempt web GraphQL comment query
        if (cursor != null && !cookies.isNullOrBlank()) {
            try {
                return fetchViaGraphqlQuery(shortcode, cursor, cookies, csrfToken)
            } catch (e: Exception) {
                // Fallback to HTML page fetch
            }
        }

        // Strategy 2: Fetch the full HTML page of the post directly using Chrome browser headers and session cookies
        val pageUrl = "https://www.instagram.com/p/$shortcode/"
        val requestBuilder = Request.Builder()
            .url(pageUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "fa-IR,fa;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Sec-Ch-Ua", "\"Chromium\";v=\"128\", \"Google Chrome\";v=\"128\"")
            .header("Sec-Ch-Ua-Mobile", "?1")
            .header("Sec-Ch-Ua-Platform", "\"Android\"")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "same-origin")
            .header("Upgrade-Insecure-Requests", "1")

        if (!cookies.isNullOrBlank()) {
            requestBuilder.header("Cookie", cookies)
        }
        if (!csrfToken.isNullOrBlank()) {
            requestBuilder.header("X-CSRFToken", csrfToken)
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            throw IllegalStateException("Rate Limited: اینستاگرام درخواست‌های بیش از حد دریافت کرده است. لطفاً کمی صبر کنید.")
        }
        if (response.code == 404) {
            throw IllegalArgumentException("پست مورد نظر پیدا نشد یا صفحه خصوصی است.")
        }
        if (!response.isSuccessful) {
            throw IllegalStateException("خطا در ارتباط با اینستاگرام (کد وضعیت: ${response.code})")
        }

        val html = response.body?.string() ?: ""

        // Multi-level HTML parser with post metadata extraction:
        return parseHtmlContent(html, shortcode)
    }

    private fun fetchViaGraphqlQuery(
        shortcode: String,
        cursor: String,
        cookies: String,
        csrfToken: String?
    ): ExtractionResult {
        val targetUrl = "https://www.instagram.com/graphql/query/?query_hash=b96016d71b80066d16e322fe03f837e6&variables={\"shortcode\":\"$shortcode\",\"first\":50,\"after\":\"$cursor\"}"
        val reqBuilder = Request.Builder()
            .url(targetUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
            .header("Accept", "*/*")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", "https://www.instagram.com/p/$shortcode/")
            .header("Cookie", cookies)

        if (!csrfToken.isNullOrBlank()) {
            reqBuilder.header("X-CSRFToken", csrfToken)
        }

        val resp = client.newCall(reqBuilder.build()).execute()
        if (resp.isSuccessful) {
            val body = resp.body?.string() ?: ""
            return parseInstagramJsonResponse(body, shortcode)
        } else {
            throw IllegalStateException("خطا در استعلام کامنت‌ها: ${resp.code}")
        }
    }

    fun parseHtmlContent(html: String, shortcode: String): ExtractionResult {
        val rawUsers = mutableListOf<ExtractedCommentUser>()
        var hasNextPage = false
        var endCursor: String? = null
        var metadata = extractMetadataFromHtml(html)

        // 1. Check for embedded JSON in script tags: <script type="application/json" ...> or <script type="text/json" ...>
        val scriptPattern = Pattern.compile("<script[^>]*type=[\"'](?:application|text)/(?:json|javascript)[\"'][^>]*>(.*?)</script>", Pattern.DOTALL)
        val matcher = scriptPattern.matcher(html)

        while (matcher.find()) {
            val scriptContent = matcher.group(1)?.trim() ?: continue
            if (scriptContent.contains("shortcode_media") ||
                scriptContent.contains("edge_media_to_parent_comment") ||
                scriptContent.contains("xdt_shortcode_media") ||
                scriptContent.contains("edge_media_to_comment")
            ) {
                try {
                    val result = parseInstagramJsonResponse(scriptContent, shortcode)
                    if (result.users.isNotEmpty()) {
                        rawUsers.addAll(result.users)
                        if (result.hasNextPage) {
                            hasNextPage = true
                            endCursor = result.endCursor
                        }
                    }
                    if (result.postMetadata.thumbnailUrl.isNotBlank() || result.postMetadata.caption.isNotBlank()) {
                        metadata = result.postMetadata
                    }
                } catch (_: Exception) {
                    extractUsernamesFromString(scriptContent, rawUsers)
                }
            }
        }

        // 2. Direct regex scan over the entire HTML for comment owners and usernames
        if (rawUsers.isEmpty()) {
            extractUsernamesFromString(html, rawUsers)
        }

        // 3. Deduplicate: Separate duplicates and keep ONLY unique user IDs / usernames
        val uniqueMap = linkedMapOf<String, ExtractedCommentUser>()
        var duplicates = 0

        for (user in rawUsers) {
            val key = user.username.lowercase().trim()
            if (key.length < 2 || excludedKeywords.contains(key)) continue

            if (uniqueMap.containsKey(key)) {
                duplicates++
            } else {
                uniqueMap[key] = user
            }
        }

        val finalCommentCount = if (metadata.commentCount > 0) metadata.commentCount else (uniqueMap.size + duplicates)

        return ExtractionResult(
            mediaId = shortcode,
            users = uniqueMap.values.toList(),
            duplicateCount = duplicates,
            hasNextPage = hasNextPage,
            endCursor = endCursor,
            postMetadata = metadata.copy(commentCount = finalCommentCount)
        )
    }

    private fun extractMetadataFromHtml(html: String): PostMetadata {
        var caption = ""
        var thumbnailUrl = ""
        var videoUrl = ""
        var isVideo = false
        var authorUsername = ""
        var commentCount = 0

        // Meta tags extraction
        val ogImageMatch = Pattern.compile("<meta[^>]*property=[\"']og:image[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(html)
        if (ogImageMatch.find()) {
            thumbnailUrl = ogImageMatch.group(1)?.replace("&amp;", "&") ?: ""
        }

        val ogVideoMatch = Pattern.compile("<meta[^>]*property=[\"']og:video[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(html)
        if (ogVideoMatch.find()) {
            videoUrl = ogVideoMatch.group(1)?.replace("&amp;", "&") ?: ""
            isVideo = true
        }

        val ogDescMatch = Pattern.compile("<meta[^>]*property=[\"']og:description[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(html)
        if (ogDescMatch.find()) {
            val desc = ogDescMatch.group(1) ?: ""
            // Description often comes as: "123 likes, 45 comments - username on date: 'Caption text'"
            caption = desc.substringAfter(":", desc).trim().removePrefix("\"").removeSuffix("\"")
            val commentMatch = Pattern.compile("(\\d+)\\s+comments?", Pattern.CASE_INSENSITIVE).matcher(desc)
            if (commentMatch.find()) {
                commentCount = commentMatch.group(1)?.toIntOrNull() ?: 0
            }
        }

        val ogTitleMatch = Pattern.compile("<meta[^>]*property=[\"']og:title[\"'][^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(html)
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

        val cleanedThumb = cleanUrl(thumbnailUrl)
        val cleanedVideo = cleanUrl(videoUrl)

        return PostMetadata(
            caption = caption,
            thumbnailUrl = cleanedThumb,
            videoUrl = cleanedVideo,
            isVideo = isVideo || cleanedVideo.isNotEmpty(),
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

    private fun parseInstagramJsonResponse(jsonStr: String, shortcode: String): ExtractionResult {
        val root = JSONObject(jsonStr)
        val extractedUsers = mutableListOf<ExtractedCommentUser>()
        var mediaId = shortcode
        var hasNextPage = false
        var endCursor: String? = null

        var caption = ""
        var thumbnailUrl = ""
        var videoUrl = ""
        var isVideo = false
        var authorUsername = ""
        var commentCount = 0

        val mediaObj = when {
            root.has("graphql") -> root.getJSONObject("graphql").optJSONObject("shortcode_media")
            root.has("items") -> root.getJSONArray("items").optJSONObject(0)
            root.has("data") -> {
                val dataObj = root.getJSONObject("data")
                dataObj.optJSONObject("xdt_shortcode_media")
                    ?: dataObj.optJSONObject("shortcode_media")
            }
            else -> root.optJSONObject("shortcode_media")
        }

        if (mediaObj != null) {
            mediaId = mediaObj.optString("id", shortcode)
            thumbnailUrl = cleanUrl(
                mediaObj.optString("display_url").ifEmpty {
                    mediaObj.optString("thumbnail_src")
                }
            )
            videoUrl = cleanUrl(mediaObj.optString("video_url"))
            isVideo = mediaObj.optBoolean("is_video", videoUrl.isNotEmpty())

            // Extract caption
            val captionObj = mediaObj.optJSONObject("edge_media_to_caption")
            if (captionObj != null) {
                val edges = captionObj.optJSONArray("edges")
                if (edges != null && edges.length() > 0) {
                    caption = edges.getJSONObject(0).optJSONObject("node")?.optString("text", "") ?: ""
                }
            } else if (mediaObj.has("caption")) {
                val cap = mediaObj.optJSONObject("caption")
                caption = cap?.optString("text", "") ?: mediaObj.optString("caption", "")
            }

            // Extract post author
            val owner = mediaObj.optJSONObject("owner")
            if (owner != null) {
                authorUsername = owner.optString("username")
                if (authorUsername.isNotEmpty()) {
                    extractedUsers.add(
                        ExtractedCommentUser(
                            username = authorUsername,
                            userId = owner.optString("id"),
                            displayName = owner.optString("full_name", authorUsername),
                            profileUrl = "https://www.instagram.com/$authorUsername/",
                            commentText = "نویسنده پست"
                        )
                    )
                }
            }

            // Extract comments container
            val edgeComments = mediaObj.optJSONObject("edge_media_to_parent_comment")
                ?: mediaObj.optJSONObject("edge_media_to_comment")

            if (edgeComments != null) {
                commentCount = edgeComments.optInt("count", 0)
                val pageInfo = edgeComments.optJSONObject("page_info")
                if (pageInfo != null) {
                    hasNextPage = pageInfo.optBoolean("has_next_page", false)
                    endCursor = pageInfo.optString("end_cursor", null)
                }

                val edges = edgeComments.optJSONArray("edges")
                if (edges != null) {
                    for (i in 0 until edges.length()) {
                        val node = edges.getJSONObject(i).optJSONObject("node") ?: continue
                        val text = node.optString("text", "")
                        val ownerObj = node.optJSONObject("owner")
                        if (ownerObj != null) {
                            val username = ownerObj.optString("username")
                            if (username.isNotEmpty()) {
                                extractedUsers.add(
                                    ExtractedCommentUser(
                                        username = username,
                                        userId = ownerObj.optString("id"),
                                        displayName = ownerObj.optString("full_name", username),
                                        profileUrl = "https://www.instagram.com/$username/",
                                        commentText = text
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        val uniqueMap = linkedMapOf<String, ExtractedCommentUser>()
        var dupCount = 0
        for (u in extractedUsers) {
            val key = u.username.lowercase().trim()
            if (key.length < 2 || excludedKeywords.contains(key)) continue
            if (uniqueMap.containsKey(key)) {
                dupCount++
            } else {
                uniqueMap[key] = u
            }
        }

        return ExtractionResult(
            mediaId = mediaId,
            users = uniqueMap.values.toList(),
            duplicateCount = dupCount,
            hasNextPage = hasNextPage,
            endCursor = endCursor,
            postMetadata = PostMetadata(
                caption = caption,
                thumbnailUrl = thumbnailUrl,
                videoUrl = videoUrl,
                isVideo = isVideo,
                authorUsername = authorUsername,
                commentCount = if (commentCount > 0) commentCount else (uniqueMap.size + dupCount)
            )
        )
    }

    private fun extractUsernamesFromString(rawText: String, targetList: MutableList<ExtractedCommentUser>) {
        val ownerPattern = Pattern.compile("\"owner\"\\s*:\\s*\\{\\s*\"id\"\\s*:\\s*\"(\\d+)\"[^}]*\"username\"\\s*:\\s*\"([a-zA-Z0-9._]+)\"")
        val ownerMatcher = ownerPattern.matcher(rawText)
        while (ownerMatcher.find()) {
            val id = ownerMatcher.group(1) ?: ""
            val username = ownerMatcher.group(2) ?: ""
            if (username.isNotBlank()) {
                targetList.add(
                    ExtractedCommentUser(
                        username = username,
                        userId = id,
                        displayName = username,
                        profileUrl = "https://www.instagram.com/$username/",
                        commentText = "کامنت اینستاگرام"
                    )
                )
            }
        }

        val userPattern = Pattern.compile("\"user\"\\s*:\\s*\\{[^}]*\"pk\"\\s*:\\s*\"?(\\d+)\"?[^}]*\"username\"\\s*:\\s*\"([a-zA-Z0-9._]+)\"")
        val userMatcher = userPattern.matcher(rawText)
        while (userMatcher.find()) {
            val id = userMatcher.group(1) ?: ""
            val username = userMatcher.group(2) ?: ""
            if (username.isNotBlank()) {
                targetList.add(
                    ExtractedCommentUser(
                        username = username,
                        userId = id,
                        displayName = username,
                        profileUrl = "https://www.instagram.com/$username/",
                        commentText = "کامنت اینستاگرام"
                    )
                )
            }
        }

        val simpleUserPattern = Pattern.compile("\"username\"\\s*:\\s*\"([a-zA-Z0-9._]{3,30})\"")
        val simpleMatcher = simpleUserPattern.matcher(rawText)
        while (simpleMatcher.find()) {
            val username = simpleMatcher.group(1) ?: ""
            if (username.isNotBlank() && !excludedKeywords.contains(username.lowercase())) {
                targetList.add(
                    ExtractedCommentUser(
                        username = username,
                        profileUrl = "https://www.instagram.com/$username/",
                        commentText = "کاربر کامنت‌گذار"
                    )
                )
            }
        }

        val anchorPattern = Pattern.compile("<a[^>]*href=\"/([a-zA-Z0-9._]{3,30})/\"[^>]*>")
        val anchorMatcher = anchorPattern.matcher(rawText)
        while (anchorMatcher.find()) {
            val username = anchorMatcher.group(1) ?: ""
            if (username.isNotBlank() && !excludedKeywords.contains(username.lowercase())) {
                targetList.add(
                    ExtractedCommentUser(
                        username = username,
                        profileUrl = "https://www.instagram.com/$username/",
                        commentText = "کاربر کامنت‌گذار"
                    )
                )
            }
        }
    }
}
