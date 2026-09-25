package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.local.entity.*
import com.example.data.remote.InstagramParser
import com.example.security.KeystoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class InstagramRepository(
    private val context: Context,
    private val db: AppDatabase = AppDatabase.getDatabase(context),
    private val keystoreManager: KeystoreManager = KeystoreManager(context),
    private val parser: InstagramParser = InstagramParser(),
    private val settingsDataStore: SettingsDataStore = SettingsDataStore(context)
) {

    val allPosts: Flow<List<InstagramPost>> = db.postDao().getAllPosts()
    val allUsers: Flow<List<InstagramUser>> = db.userDao().getAllUsers()
    val allAccounts: Flow<List<InstagramAccount>> = db.accountDao().getAllAccounts()
    val allCampaigns: Flow<List<MessageCampaign>> = db.campaignDao().getAllCampaigns()
    val allQueueItems: Flow<List<MessageQueue>> = db.messageQueueDao().getAllQueueItems()
    val allBlacklist: Flow<List<BlacklistUser>> = db.blacklistDao().getAllBlacklistedUsers()

    val totalPostCount: Flow<Int> = db.postDao().getPostCount()
    val totalUserCount: Flow<Int> = db.userDao().getTotalUserCount()
    val connectedAccountCount: Flow<Int> = db.accountDao().getConnectedAccountCount()
    val sentMessageCount: Flow<Int> = db.messageQueueDao().getSentCount()
    val pendingMessageCount: Flow<Int> = db.messageQueueDao().getPendingCount()
    val failedMessageCount: Flow<Int> = db.messageQueueDao().getFailedCount()

    val sessionCookies: Flow<String> = settingsDataStore.instagramCookies
    val sessionCsrfToken: Flow<String> = settingsDataStore.instagramCsrfToken
    val sessionUser: Flow<String> = settingsDataStore.instagramSessionUser
    val appThemeMode: Flow<String> = settingsDataStore.themeMode

    suspend fun setAppTheme(mode: String) {
        settingsDataStore.setThemeMode(mode)
    }

    // 1. EXTRACT COMMENTS FROM POST (PURE HTML & COOKIES PARSING)
    suspend fun extractCommentsFromPost(
        postUrl: String,
        onProgress: (fetched: Int, unique: Int, duplicates: Int, errors: Int) -> Unit
    ): Result<InstagramPost> = withContext(Dispatchers.IO) {
        try {
            val shortcode = parser.extractShortcode(postUrl)

            // Check active session cookies
            var cookies = settingsDataStore.instagramCookies.first()
            var csrfToken = settingsDataStore.instagramCsrfToken.first()

            if (cookies.isBlank()) {
                val activeAccounts = db.accountDao().getActiveAccounts()
                val accWithCookie = activeAccounts.firstOrNull { it.cookiesEncrypted.isNotEmpty() }
                if (accWithCookie != null) {
                    cookies = keystoreManager.decrypt(accWithCookie.cookiesEncrypted)
                    csrfToken = accWithCookie.csrfToken
                }
            }

            // Check or create post entity
            var post = db.postDao().getPostByUrl(postUrl)
            if (post == null) {
                val newPostId = db.postDao().insertPost(
                    InstagramPost(
                        postUrl = postUrl,
                        instagramMediaId = shortcode,
                        status = "PROCESSING"
                    )
                )
                post = db.postDao().getPostById(newPostId)!!
            } else {
                db.postDao().updatePost(post.copy(status = "PROCESSING"))
            }

            val blacklistedUsernames = db.blacklistDao().getAllBlacklistedUsernames().toSet()

            var cursor: String? = null
            var totalFetched = 0
            var uniqueCount = 0
            var duplicateCount = 0
            var errorCount = 0

            var hasMore = true
            var pageIteration = 0

            var lastMetadata = com.example.data.remote.PostMetadata()
            var lastMediaId = shortcode

            while (hasMore && pageIteration < 200) { // Support fetching up to 10,000+ comments
                pageIteration++
                try {
                    val result = parser.fetchCommentsFromWebPage(
                        shortcode = shortcode,
                        cursor = cursor,
                        cookies = cookies.ifBlank { null },
                        csrfToken = csrfToken.ifBlank { null }
                    )

                    lastMetadata = result.postMetadata
                    if (result.mediaId.isNotBlank()) {
                        lastMediaId = result.mediaId
                    }
                    duplicateCount += result.duplicateCount
                    val rawUsers = result.users
                    totalFetched += rawUsers.size + result.duplicateCount

                    for (rawUser in rawUsers) {
                        val cleanUsername = rawUser.username.lowercase().trim()
                        if (cleanUsername.isEmpty()) continue

                        // Check duplicate in global users database
                        val existingUser = db.userDao().getUserByUsername(cleanUsername)
                        if (existingUser != null) {
                            duplicateCount++
                            // Update last seen + fill userId & profilePic if they were empty before
                            val updatedId = if (existingUser.instagramUserId.isBlank() && rawUser.userId.isNotBlank()) {
                                rawUser.userId
                            } else existingUser.instagramUserId
                            val updatedPic = if (existingUser.profilePicUrl.isBlank() && rawUser.profilePicUrl.isNotBlank()) {
                                rawUser.profilePicUrl
                            } else existingUser.profilePicUrl

                            db.userDao().updateUser(
                                existingUser.copy(
                                    lastSeenAt = System.currentTimeMillis(),
                                    instagramUserId = updatedId,
                                    profilePicUrl = updatedPic
                                )
                            )
                        } else {
                            val isBlacklisted = blacklistedUsernames.contains(cleanUsername)
                            val newUser = InstagramUser(
                                username = cleanUsername,
                                instagramUserId = rawUser.userId,
                                displayName = rawUser.displayName.ifEmpty { cleanUsername },
                                profileUrl = rawUser.profileUrl,
                                profilePicUrl = rawUser.profilePicUrl,
                                sourcePostId = post.id,
                                messageStatus = if (isBlacklisted) "SKIPPED" else "NONE"
                            )
                            val insertedId = db.userDao().insertUser(newUser)
                            if (insertedId > 0) {
                                uniqueCount++
                            } else {
                                duplicateCount++
                            }
                        }
                    }

                    onProgress(totalFetched, uniqueCount, duplicateCount, errorCount)

                    hasMore = result.hasNextPage
                    cursor = result.endCursor
                    if (cursor.isNullOrBlank() || rawUsers.isEmpty()) {
                        hasMore = false
                    }
                } catch (e: Exception) {
                    errorCount++
                    onProgress(totalFetched, uniqueCount, duplicateCount, errorCount)
                    if (totalFetched == 0) {
                        throw e
                    }
                    break
                }
            }

            val currentPostCount = db.userDao().getUserCountByPost(post.id)
            val finalComments = if (lastMetadata.commentCount > 0) lastMetadata.commentCount else totalFetched
            val updatedPost = post.copy(
                instagramMediaId = lastMediaId,
                caption = lastMetadata.caption.ifEmpty { post.caption },
                thumbnailUrl = lastMetadata.thumbnailUrl.ifEmpty { post.thumbnailUrl },
                videoUrl = lastMetadata.videoUrl.ifEmpty { post.videoUrl },
                isVideo = lastMetadata.isVideo || post.isVideo,
                authorUsername = lastMetadata.authorUsername.ifEmpty { post.authorUsername },
                totalComments = finalComments,
                uniqueUsers = currentPostCount,
                status = "COMPLETED"
            )
            db.postDao().updatePost(updatedPost)

            Result.success(updatedPost)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. BROWSER LOGIN & COOKIES SAVING
    suspend fun saveBrowserSession(cookies: String, csrfToken: String, username: String) {
        val encCookies = keystoreManager.encrypt(cookies)
        val cleanUser = username.removePrefix("@").trim().ifEmpty { "instagram_user" }
        settingsDataStore.saveSessionCookies(cookies, csrfToken, cleanUser)

        val activeAccounts = db.accountDao().getActiveAccounts()
        val existing = activeAccounts.find { it.username.equals(cleanUser, ignoreCase = true) }
        if (existing != null) {
            db.accountDao().updateAccount(
                existing.copy(
                    cookiesEncrypted = encCookies,
                    csrfToken = csrfToken,
                    status = "CONNECTED",
                    lastUsedAt = System.currentTimeMillis()
                )
            )
        } else {
            db.accountDao().insertAccount(
                InstagramAccount(
                    username = cleanUser,
                    displayName = cleanUser,
                    cookiesEncrypted = encCookies,
                    csrfToken = csrfToken,
                    status = "CONNECTED"
                )
            )
        }
    }

    suspend fun clearBrowserSession() {
        settingsDataStore.clearSessionCookies()
    }

    // 3. ACCOUNT MANAGEMENT
    suspend fun addAccount(username: String, displayName: String, rawCookies: String, csrfToken: String) {
        val encryptedCookies = keystoreManager.encrypt(rawCookies)
        val cleanUser = username.removePrefix("@").trim()
        val newAccount = InstagramAccount(
            username = cleanUser,
            displayName = displayName.ifEmpty { cleanUser },
            cookiesEncrypted = encryptedCookies,
            csrfToken = csrfToken,
            status = "CONNECTED"
        )
        db.accountDao().insertAccount(newAccount)

        if (settingsDataStore.instagramCookies.first().isBlank()) {
            settingsDataStore.saveSessionCookies(rawCookies, csrfToken, cleanUser)
        }
    }

    suspend fun updateAccountStatus(accountId: Long, status: String) {
        val account = db.accountDao().getAccountById(accountId) ?: return
        db.accountDao().updateAccount(account.copy(status = status, lastUsedAt = System.currentTimeMillis()))
    }

    suspend fun deleteAccount(account: InstagramAccount) {
        db.accountDao().deleteAccount(account)
    }

    // 4. CAMPAIGN CREATION & QUEUE GENERATION
    suspend fun createCampaign(
        name: String,
        messageTemplate: String,
        sourcePostId: Long,
        selectedAccountIds: List<Long> = emptyList()
    ): Result<MessageCampaign> = withContext(Dispatchers.IO) {
        try {
            val targetUsers = if (sourcePostId == 0L) {
                emptyList()
            } else {
                db.userDao().getUsersByPostIdSync(sourcePostId)
            }

            val blacklisted = db.blacklistDao().getAllBlacklistedUsernames().toSet()

            val activeAccounts = if (selectedAccountIds.isNotEmpty()) {
                selectedAccountIds.mapNotNull { db.accountDao().getAccountById(it) }
            } else {
                db.accountDao().getActiveAccounts()
            }

            val campaign = MessageCampaign(
                name = name,
                messageText = messageTemplate,
                sourcePostId = sourcePostId,
                status = "RUNNING",
                totalRecipients = targetUsers.size,
                pendingCount = targetUsers.size
            )

            val campaignId = db.campaignDao().insertCampaign(campaign)

            val queueItems = mutableListOf<MessageQueue>()
            var accountIndex = 0

            for (user in targetUsers) {
                val isBlacklisted = blacklisted.contains(user.username.lowercase())
                val formattedText = messageTemplate
                    .replace("{username}", user.username)
                    .replace("{display_name}", user.displayName.ifEmpty { user.username })

                val assignedAccount = if (activeAccounts.isNotEmpty()) {
                    val acc = activeAccounts[accountIndex % activeAccounts.size]
                    accountIndex++
                    acc
                } else null

                queueItems.add(
                    MessageQueue(
                        campaignId = campaignId,
                        userId = user.id,
                        accountId = assignedAccount?.id,
                        messageText = formattedText,
                        status = if (isBlacklisted) "SKIPPED" else "PENDING",
                        errorMessage = if (isBlacklisted) "در لیست سیاه قرار دارد" else null
                    )
                )
            }

            db.messageQueueDao().insertQueueItems(queueItems)

            val createdCampaign = db.campaignDao().getCampaignById(campaignId)!!
            Result.success(createdCampaign)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 5. QUEUE CONTROLS
    suspend fun pauseCampaign(campaignId: Long) {
        val campaign = db.campaignDao().getCampaignById(campaignId) ?: return
        db.campaignDao().updateCampaign(campaign.copy(status = "PAUSED"))
        db.messageQueueDao().updateCampaignQueueStatus(campaignId, "PAUSED")
    }

    suspend fun resumeCampaign(campaignId: Long) {
        val campaign = db.campaignDao().getCampaignById(campaignId) ?: return
        db.campaignDao().updateCampaign(campaign.copy(status = "RUNNING"))
        db.messageQueueDao().updateCampaignQueueStatus(campaignId, "PENDING")
    }

    suspend fun retryFailedQueue(campaignId: Long) {
        val items = db.messageQueueDao().getQueueByCampaignSync(campaignId)
        for (item in items) {
            if (item.status == "FAILED" || item.status == "RATE_LIMITED") {
                db.messageQueueDao().updateQueueItem(
                    item.copy(status = "PENDING", errorMessage = null)
                )
            }
        }
    }

    // 6. BLACKLIST MANAGEMENT
    suspend fun addToBlacklist(username: String, reason: String = "توسط کاربر") {
        val clean = username.removePrefix("@").trim().lowercase()
        if (clean.isNotEmpty()) {
            db.blacklistDao().insertBlacklistUser(
                BlacklistUser(username = clean, reason = reason)
            )
        }
    }

    suspend fun removeFromBlacklist(username: String) {
        db.blacklistDao().deleteByUsername(username.removePrefix("@").trim().lowercase())
    }

    // 7. EXPORT USERS (CSV, JSON, TXT)
    suspend fun exportUsers(users: List<InstagramUser>, format: String): File = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val timestamp = System.currentTimeMillis()
        val file = when (format.lowercase()) {
            "json" -> {
                val f = File(exportDir, "users_export_$timestamp.json")
                val jsonArray = JSONArray()
                for (u in users) {
                    val obj = JSONObject().apply {
                        put("username", u.username)
                        put("display_name", u.displayName)
                        put("profile_url", u.profileUrl)
                        put("message_status", u.messageStatus)
                    }
                    jsonArray.put(obj)
                }
                f.writeText(jsonArray.toString(2), Charsets.UTF_8)
                f
            }
            "txt" -> {
                val f = File(exportDir, "users_export_$timestamp.txt")
                val builder = StringBuilder()
                for (u in users) {
                    builder.append("@").append(u.username).append("\n")
                }
                f.writeText(builder.toString(), Charsets.UTF_8)
                f
            }
            else -> { // CSV
                val f = File(exportDir, "users_export_$timestamp.csv")
                val builder = StringBuilder()
                builder.append("username,display_name,profile_url,message_status\n")
                for (u in users) {
                    builder.append("\"").append(u.username).append("\",")
                        .append("\"").append(u.displayName).append("\",")
                        .append("\"").append(u.profileUrl).append("\",")
                        .append("\"").append(u.messageStatus).append("\"\n")
                }
                f.writeText(builder.toString(), Charsets.UTF_8)
                f
            }
        }
        file
    }

    // 8. REAL DIRECT MESSAGE SENDING TO INSTAGRAM (FIXED)
    suspend fun sendDirectMessage(
        targetUsername: String,
        messageText: String,
        account: InstagramAccount? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanTarget = targetUsername.removePrefix("@").trim()
            if (cleanTarget.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("نام کاربری مقصد مشخص نشده است."))
            }
            if (messageText.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("متن پیام خالی است."))
            }

            val rawCookies: String
            val csrfToken: String
            val senderUser: String

            if (account != null && account.cookiesEncrypted.isNotEmpty()) {
                rawCookies = keystoreManager.decrypt(account.cookiesEncrypted)
                csrfToken = account.csrfToken
                senderUser = account.username
            } else {
                rawCookies = settingsDataStore.instagramCookies.first()
                csrfToken = settingsDataStore.instagramCsrfToken.first()
                senderUser = settingsDataStore.instagramSessionUser.first().ifEmpty { "حساب متصل" }
            }

            if (rawCookies.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("هیچ نشست فعالی با کوکی اینستاگرام یافت نشد. لطفاً ابتدا از بخش «حساب‌ها» وارد حساب خود شوید.")
                )
            }

            val effectiveCsrf = if (csrfToken.isNotBlank()) csrfToken else extractCookieValue(rawCookies, "csrftoken")
            if (effectiveCsrf.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("csrftoken یافت نشد. لطفاً دوباره وارد حساب اینستاگرام شوید.")
                )
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()

            // ---------- Resolve target user PK (numeric ID) ----------
            var targetUserId = ""

            // First try from local DB if we already have it
            val localUser = db.userDao().getUserByUsername(cleanTarget.lowercase())
            if (localUser != null && localUser.instagramUserId.isNotBlank()) {
                targetUserId = localUser.instagramUserId
            }

            if (targetUserId.isBlank()) {
                val profileUrl = "https://www.instagram.com/api/v1/users/web_profile_info/?username=$cleanTarget"
                val profileReq = Request.Builder()
                    .url(profileUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "*/*")
                    .header("X-IG-App-ID", "936619743392459")
                    .header("X-CSRFToken", effectiveCsrf)
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Cookie", rawCookies)
                    .header("Referer", "https://www.instagram.com/$cleanTarget/")
                    .header("Origin", "https://www.instagram.com")
                    .build()

                try {
                    val profileResp = client.newCall(profileReq).execute()
                    val bodyStr = profileResp.body?.string() ?: ""
                    if (profileResp.isSuccessful && bodyStr.isNotBlank()) {
                        val json = JSONObject(bodyStr)
                        val data = json.optJSONObject("data")
                        val user = data?.optJSONObject("user")
                        targetUserId = user?.optString("id")?.ifBlank { null }
                            ?: user?.optString("pk")?.ifBlank { null }
                            ?: ""
                    }
                } catch (_: Exception) {
                    // continue to next method
                }
            }

            // Fallback: public profile page scrape
            if (targetUserId.isBlank()) {
                try {
                    val pageReq = Request.Builder()
                        .url("https://www.instagram.com/$cleanTarget/")
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                        .header("Accept", "text/html")
                        .header("Cookie", rawCookies)
                        .header("X-CSRFToken", effectiveCsrf)
                        .build()
                    val pageResp = client.newCall(pageReq).execute()
                    val html = pageResp.body?.string() ?: ""
                    // Look for "profilePage_123456789" or "\"id\":\"123456789\"" near username
                    val idPattern = java.util.regex.Pattern.compile(
                        "\"id\"\\s*:\\s*\"(\\d{5,})\"[^}]{0,80}\"username\"\\s*:\\s*\"${Regex.escape(cleanTarget)}\"|\"username\"\\s*:\\s*\"${Regex.escape(cleanTarget)}\"[^}]{0,80}\"id\"\\s*:\\s*\"(\\d{5,})\""
                    )
                    val m = idPattern.matcher(html)
                    if (m.find()) {
                        targetUserId = (m.group(1) ?: m.group(2) ?: "").trim()
                    }
                    if (targetUserId.isBlank()) {
                        val pkPattern = java.util.regex.Pattern.compile("\"pk\"\\s*:\\s*\"?(\\d{5,})\"?")
                        val pkM = pkPattern.matcher(html)
                        if (pkM.find()) {
                            targetUserId = pkM.group(1) ?: ""
                        }
                    }
                } catch (_: Exception) {
                }
            }

            if (targetUserId.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("نتوانستیم شناسه عددی (pk) کاربر @$cleanTarget را پیدا کنیم. کوکی را تازه کنید یا یوزرنیم را بررسی کنید.")
                )
            }

            // Update local DB with the resolved ID if we have the user
            if (localUser != null && localUser.instagramUserId.isBlank()) {
                try {
                    db.userDao().updateUser(localUser.copy(instagramUserId = targetUserId))
                } catch (_: Exception) {
                }
            }

            // ---------- Send the actual DM ----------
            val clientContext = java.util.UUID.randomUUID().toString().replace("-", "")
            val sendUrl = "https://www.instagram.com/api/v1/direct_v2/threads/broadcast/text/"

            val formBody = FormBody.Builder()
                .add("recipient_users", "[[\"$targetUserId\"]]")
                .add("text", messageText)
                .add("client_context", clientContext)
                .add("action", "send_item")
                .build()

            val sendReq = Request.Builder()
                .url(sendUrl)
                .post(formBody)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                .header("Accept", "*/*")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("X-CSRFToken", effectiveCsrf)
                .header("X-IG-App-ID", "936619743392459")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Cookie", rawCookies)
                .header("Origin", "https://www.instagram.com")
                .header("Referer", "https://www.instagram.com/direct/inbox/")
                .build()

            val sendResp = client.newCall(sendReq).execute()
            val respBody = sendResp.body?.string() ?: ""

            if (!sendResp.isSuccessful) {
                val errorMsg = try {
                    val errJson = JSONObject(respBody)
                    errJson.optString("message", "کد خطا ${sendResp.code}")
                } catch (_: Exception) {
                    "پاسخ سرور اینستاگرام (${sendResp.code})"
                }
                return@withContext Result.failure(
                    IllegalStateException("ارسال به @$cleanTarget از طریق اکانت @$senderUser ناموفق بود: $errorMsg")
                )
            }

            val jsonResult = JSONObject(respBody)
            val status = jsonResult.optString("status", "")
            if (status == "ok") {
                val payload = jsonResult.optJSONObject("payload")
                val itemId = payload?.optString("item_id", "") ?: ""
                Result.success("پیام واقعی با موفقیت به دایرکت @$cleanTarget ارسال شد (شناسه: ${if (itemId.isNotEmpty()) itemId else "موفق"}).")
            } else {
                val msg = jsonResult.optString("message", "اینستاگرام پیام را ارسال نکرد.")
                Result.failure(IllegalStateException("خطای اینستاگرام: $msg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(post: InstagramPost) = withContext(Dispatchers.IO) {
        db.postDao().deletePost(post)
    }

    suspend fun deleteAllPosts() = withContext(Dispatchers.IO) {
        val posts = db.postDao().getAllPosts().first()
        for (p in posts) {
            db.postDao().deletePost(p)
        }
    }

    private fun extractCookieValue(cookieHeader: String, key: String): String {
        return cookieHeader.split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("$key=") }
            ?.removePrefix("$key=")
            ?: ""
    }
}
