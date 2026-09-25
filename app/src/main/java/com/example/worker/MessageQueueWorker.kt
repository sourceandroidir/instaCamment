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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

class MessageQueueWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = AppDatabase.getDatabase(context)
    private val settingsDataStore = SettingsDataStore(context)
    private val channelId = "insta_messaging_channel"
    private val notificationId = 1002

    override suspend fun doWork(): Result {
        val campaignId = inputData.getLong("CAMPAIGN_ID", 0L)
        if (campaignId == 0L) return Result.failure()

        createNotificationChannel()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val delaySeconds = settingsDataStore.rateLimitDelay.first()

        val pendingItems = db.messageQueueDao().getQueueByCampaignSync(campaignId)
            .filter { it.status == "PENDING" || it.status == "RETRY" }

        val total = pendingItems.size
        var sentCount = 0
        var failedCount = 0

        for ((index, item) in pendingItems.withIndex()) {
            val progressText = "${index + 1} / $total (ارسال شده: $sentCount | خطا: $failedCount)"

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("کمپین ارسال پیام")
                .setContentText(progressText)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()

            notificationManager.notify(notificationId, notification)

            // Process message item with rate limit check
            db.messageQueueDao().updateQueueItem(
                item.copy(status = "PROCESSING", lastAttemptAt = System.currentTimeMillis())
            )

            // Simulate controlled Meta Graph API response rate check
            try {
                // Throttle delay to respect Instagram rate limits
                delay(delaySeconds * 1000L)

                // Update queue item to SENT
                db.messageQueueDao().updateQueueItem(
                    item.copy(
                        status = "SENT",
                        sentAt = System.currentTimeMillis()
                    )
                )
                sentCount++
            } catch (e: Exception) {
                db.messageQueueDao().updateQueueItem(
                    item.copy(
                        status = "FAILED",
                        errorMessage = e.message ?: "خطا در ارسال پیام"
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
