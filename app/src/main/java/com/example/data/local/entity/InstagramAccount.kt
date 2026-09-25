package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "instagram_accounts")
data class InstagramAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val displayName: String = "",
    val accessTokenEncrypted: String = "",
    val cookiesEncrypted: String = "",
    val csrfToken: String = "",
    val tokenExpiresAt: Long = 0L,
    val status: String = "CONNECTED", // CONNECTED, DISCONNECTED, TOKEN_EXPIRED, PERMISSION_ERROR, RATE_LIMITED, DISABLED
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis()
)
