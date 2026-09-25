package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.repository.InstagramRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

class MessageQueueWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = AppDatabase.getDatabase(context)
    private val settingsDataStore = SettingsDataStore(context)
    private val repository = InstagramRepository(context)
    private val channelId = "insta_messaging_channel"
    private val notificationId = 1002

    override suspend fun doWork(): Result {
        val campaignId = inputData.getLong("CAMPAIGN_ID", 0L)
        if (campaignId == 0L) return Result.failure()

        createNotificationChannel()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val delaySeconds = settingsDataStore.rateLimitDelay.first().coerceAtLeast(5)

        val pendingItems = db.messageQueueDao().getQueueByCampaignSync(campaignId)
            .filter { it.status == "PENDING" || it.status == "RETRY" }

        val total = pendingItems.size
        var sentCount = 0
        var failedCount = 0

        for ((index, item) in pendingItems.withIndex()) {
            val progressText = "${index + 1} / $total (ارسال: $sentCount | خطا: $failedCount)"

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("کمپین ارسال پیام")
                .setContentText(progressText)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()

            notificationManager.notify(notificationId, notification)

            db.messageQueueDao().updateQueueItem(
                item.copy(status = "PROCESSING", lastAttemptAt = System.currentTimeMillis())
            )

            try {
                // Rate limit delay
                if (index > 0) {
                    delay(delaySeconds * 1000L)
                }

                val targetUser = db.userDao().getUserById(item.userId)
                val assignedAccount = if (item.accountId != null) db.accountDao().getAccountById(item.accountId) else null

                if (targetUser == null || targetUser.username.isBlank()) {
                    db.messageQueueDao().updateQueueItem(
                        item.copy(status = "FAILED", errorMessage = "کاربر در دیتابیس یافت نشد")
                    )
                    failedCount++
                    continue
                }

                val sendResult = repository.sendDirectMessage(
                    targetUsername = targetUser.username,
                    messageText = item.messageText,
                    account = assignedAccount
                )

                if (sendResult.isSuccess) {
                    db.messageQueueDao().updateQueueItem(
                        item.copy(
                            status = "SENT",
                            sentAt = System.currentTimeMillis(),
                            errorMessage = null
                        )
                    )
                    sentCount++
                } else {
                    val err = sendResult.exceptionOrNull()?.message ?: "خطای ارسال به اینستاگرام"
                    db.messageQueueDao().updateQueueItem(
                        item.copy(
                            status = "FAILED",
                            errorMessage = err
                        )
                    )
                    failedCount++
                }
            } catch (e: Exception) {
                db.messageQueueDao().updateQueueItem(
                    item.copy(
                        status = "FAILED",
                        errorMessage = e.message ?: "خطا در پردازش پیام"
                    )
                )
                failedCount++
            }
        }

        notificationManager.cancel(notificationId)

        // Update campaign final status
        val campaign = db.campaignDao().getCampaignById(campaignId)
        if (campaign != null) {
            db.campaignDao().updateCampaign(
                campaign.copy(
                    status = "COMPLETED",
                    sentCount = campaign.sentCount + sentCount,
                    failedCount = campaign.failedCount + failedCount,
                    pendingCount = 0
                )
            )
        }

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ارسال پیام کمپین"
            val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
