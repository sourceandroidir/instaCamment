package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.MessageQueue
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageQueueDao {
    @Query("SELECT * FROM message_queue ORDER BY id ASC")
    fun getAllQueueItems(): Flow<List<MessageQueue>>

    @Query("SELECT * FROM message_queue WHERE campaignId = :campaignId")
    fun getQueueByCampaign(campaignId: Long): Flow<List<MessageQueue>>

    @Query("SELECT * FROM message_queue WHERE campaignId = :campaignId")
    suspend fun getQueueByCampaignSync(campaignId: Long): List<MessageQueue>

    @Query("SELECT * FROM message_queue WHERE status = 'PENDING' OR status = 'RETRY' ORDER BY id ASC LIMIT :limit")
    suspend fun getPendingItems(limit: Int = 10): List<MessageQueue>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<MessageQueue>)

    @Update
    suspend fun updateQueueItem(item: MessageQueue)

    @Query("UPDATE message_queue SET status = :status WHERE campaignId = :campaignId")
    suspend fun updateCampaignQueueStatus(campaignId: Long, status: String)

    @Query("DELETE FROM message_queue WHERE campaignId = :campaignId AND status = 'PENDING'")
    suspend fun clearPendingByCampaign(campaignId: Long)

    @Query("SELECT COUNT(*) FROM message_queue WHERE status = 'SENT'")
    fun getSentCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM message_queue WHERE status = 'PENDING' OR status = 'PROCESSING'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM message_queue WHERE status = 'FAILED'")
    fun getFailedCount(): Flow<Int>
}
