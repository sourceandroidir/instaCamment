package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.InstagramPost
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM instagram_posts ORDER BY createdAt DESC")
    fun getAllPosts(): Flow<List<InstagramPost>>

    @Query("SELECT * FROM instagram_posts WHERE id = :id")
    suspend fun getPostById(id: Long): InstagramPost?

    @Query("SELECT * FROM instagram_posts WHERE postUrl = :url LIMIT 1")
    suspend fun getPostByUrl(url: String): InstagramPost?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: InstagramPost): Long

    @Update
    suspend fun updatePost(post: InstagramPost)

    @Delete
    suspend fun deletePost(post: InstagramPost)

    @Query("SELECT COUNT(*) FROM instagram_posts")
    fun getPostCount(): Flow<Int>
}
