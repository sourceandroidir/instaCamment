package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extraction_logs")
data class ExtractionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val commentsFetched: Int,
    val uniqueCount: Int,
    val duplicateCount: Int,
    val errorCount: Int,
    val lastError: String? = null
)
