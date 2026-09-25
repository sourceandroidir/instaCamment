package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blacklist_users",
    indices = [Index(value = ["username"], unique = true)]
)
data class BlacklistUser(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val addedAt: Long = System.currentTimeMillis(),
    val reason: String = "توسط کاربر"
)
