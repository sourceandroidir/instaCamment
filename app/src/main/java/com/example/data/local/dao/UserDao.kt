package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.InstagramUser
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM instagram_users ORDER BY lastSeenAt DESC")
    fun getAllUsers(): Flow<List<InstagramUser>>

    @Query("SELECT * FROM instagram_users WHERE sourcePostId = :postId")
    fun getUsersByPostId(postId: Long): Flow<List<InstagramUser>>

    @Query("SELECT * FROM instagram_users WHERE sourcePostId = :postId")
    suspend fun getUsersByPostIdSync(postId: Long): List<InstagramUser>

    @Query("SELECT * FROM instagram_users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): InstagramUser?

    @Query("SELECT * FROM instagram_users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): InstagramUser?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: InstagramUser): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUsers(users: List<InstagramUser>): List<Long>

    @Update
    suspend fun updateUser(user: InstagramUser)

    @Query("DELETE FROM instagram_users WHERE id = :id")
    suspend fun deleteUserById(id: Long)

    @Query("SELECT COUNT(*) FROM instagram_users")
    fun getTotalUserCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM instagram_users WHERE sourcePostId = :postId")
    suspend fun getUserCountByPost(postId: Long): Int
}
