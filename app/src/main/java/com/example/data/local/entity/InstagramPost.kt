package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "instagram_posts")
data class InstagramPost(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postUrl: String,
    val instagramMediaId: String,
    val caption: String = "",
    val thumbnailUrl: String = "",
    val videoUrl: String = "",
    val isVideo: Boolean = false,
    val authorUsername: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val totalComments: Int = 0,
    val uniqueUsers: Int = 0,
    val status: String = "PROCESSING" // PROCESSING, COMPLETED, FAILED, PAUSED
)
