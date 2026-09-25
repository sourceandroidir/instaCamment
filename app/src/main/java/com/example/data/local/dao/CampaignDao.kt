package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.MessageCampaign
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {
    @Query("SELECT * FROM message_campaigns ORDER BY createdAt DESC")
    fun getAllCampaigns(): Flow<List<MessageCampaign>>

    @Query("SELECT * FROM message_campaigns WHERE id = :id")
    suspend fun getCampaignById(id: Long): MessageCampaign?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: MessageCampaign): Long

    @Update
    suspend fun updateCampaign(campaign: MessageCampaign)

    @Delete
    suspend fun deleteCampaign(campaign: MessageCampaign)
}
