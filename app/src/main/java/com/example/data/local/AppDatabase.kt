package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        InstagramPost::class,
        InstagramUser::class,
        InstagramAccount::class,
        MessageCampaign::class,
        MessageQueue::class,
        BlacklistUser::class,
        ExtractionLog::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun postDao(): PostDao
    abstract fun userDao(): UserDao
    abstract fun accountDao(): AccountDao
    abstract fun campaignDao(): CampaignDao
    abstract fun messageQueueDao(): MessageQueueDao
    abstract fun blacklistDao(): BlacklistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "insta_manager_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
