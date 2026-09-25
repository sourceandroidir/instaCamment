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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

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

            while (hasMore && pageIteration < 15) { // Safety iteration limit
                pageIteration++
                try {
                    val result = parser.fetchCommentsFromWebPage(
                        shortcode = shortcode,
                        cursor = cursor,
                        cookies = cookies.ifBlank { null },
                        csrfToken = csrfToken.ifBlank { null }
                    )

                    lastMetadata = result.postMetadata
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
                            // Update last seen timestamp
                            db.userDao().updateUser(existingUser.copy(lastSeenAt = System.currentTimeMillis()))
                        } else {
                            val isBlacklisted = blacklistedUsernames.contains(cleanUsername)
                            val newUser = InstagramUser(
                                username = cleanUsername,
                                instagramUserId = rawUser.userId,
                                displayName = rawUser.displayName.ifEmpty { cleanUsername },
                                profileUrl = rawUser.profileUrl,
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
                    if (cursor.isNullOrBlank()) {
                        hasMore = false
                    }
                } catch (e: Exception) {
                    errorCount++
                    onProgress(totalFetched, uniqueCount, duplicateCount, errorCount)
                    break
                }
            }

            val currentPostCount = db.userDao().getUserCountByPost(post.id)
            val finalComments = if (lastMetadata.commentCount > 0) lastMetadata.commentCount else totalFetched
            val updatedPost = post.copy(
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
}
