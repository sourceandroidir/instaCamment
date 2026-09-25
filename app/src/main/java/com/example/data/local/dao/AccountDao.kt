package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.InstagramAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM instagram_accounts ORDER BY createdAt DESC")
    fun getAllAccounts(): Flow<List<InstagramAccount>>

    @Query("SELECT * FROM instagram_accounts WHERE status = 'CONNECTED'")
    suspend fun getActiveAccounts(): List<InstagramAccount>

    @Query("SELECT * FROM instagram_accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): InstagramAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: InstagramAccount): Long

    @Update
    suspend fun updateAccount(account: InstagramAccount)

    @Delete
    suspend fun deleteAccount(account: InstagramAccount)

    @Query("SELECT COUNT(*) FROM instagram_accounts WHERE status = 'CONNECTED'")
    fun getConnectedAccountCount(): Flow<Int>
}
