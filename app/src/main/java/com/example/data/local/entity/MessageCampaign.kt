package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_campaigns")
data class MessageCampaign(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val messageText: String,
    val sourcePostId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "DRAFT", // DRAFT, RUNNING, PAUSED, COMPLETED, FAILED
    val totalRecipients: Int = 0,
    val sentCount: Int = 0,
    val failedCount: Int = 0,
    val pendingCount: Int = 0
)
