package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "instagram_users",
    indices = [Index(value = ["username"], unique = true)]
)
data class InstagramUser(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val instagramUserId: String = "",
    val displayName: String = "",
    val profileUrl: String = "",
    val profilePicUrl: String = "",
    val sourcePostId: Long = 0,
    val firstSeenAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis(),
    val messageStatus: String = "NONE", // NONE, PENDING, SENT, FAILED, SKIPPED
    val messageSentAt: Long? = null,
    val assignedAccountId: Long? = null
)
