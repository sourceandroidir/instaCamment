package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.repository.InstagramRepository

class CommentExtractorWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val repository = InstagramRepository(context)
    private val channelId = "insta_extraction_channel"
    private val notificationId = 1001

    override suspend fun doWork(): Result {
        val postUrl = inputData.getString("POST_URL") ?: return Result.failure()

        createNotificationChannel()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val initialBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("استخراج کامنت‌های اینستاگرام")
            .setContentText("در حال آماده‌سازی...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        notificationManager.notify(notificationId, initialBuilder.build())

        val result = repository.extractCommentsFromPost(postUrl) { fetched, unique, _, errors ->
            val updatedBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("استخراج کامنت‌ها")
                .setContentText("دریافت شده: $fetched | کاربر یکتا: $unique | خطا: $errors")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)

            notificationManager.notify(notificationId, updatedBuilder.build())
        }

        notificationManager.cancel(notificationId)

        return if (result.isSuccess) {
            Result.success()
        } else {
            Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "استخراج کامنت‌ها"
            val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
