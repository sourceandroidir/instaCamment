package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_queue")
data class MessageQueue(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campaignId: Long,
    val userId: Long,
    val accountId: Long? = null,
    val messageText: String,
    val status: String = "PENDING", // PENDING, PROCESSING, SENT, FAILED, RETRY, SKIPPED, RATE_LIMITED
    val attemptCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val sentAt: Long? = null,
    val errorMessage: String? = null
)
