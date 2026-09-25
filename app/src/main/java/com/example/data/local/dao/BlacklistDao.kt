package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.BlacklistUser
import kotlinx.coroutines.flow.Flow

@Dao
interface BlacklistDao {
    @Query("SELECT * FROM blacklist_users ORDER BY addedAt DESC")
    fun getAllBlacklistedUsers(): Flow<List<BlacklistUser>>

    @Query("SELECT * FROM blacklist_users WHERE username = :username LIMIT 1")
    suspend fun getBlacklistedByUsername(username: String): BlacklistUser?

    @Query("SELECT username FROM blacklist_users")
    suspend fun getAllBlacklistedUsernames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlacklistUser(user: BlacklistUser): Long

    @Query("DELETE FROM blacklist_users WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM blacklist_users WHERE username = :username")
    suspend fun deleteByUsername(username: String)
}
