package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY username ASC")
    fun getAllUsersFlow(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("DELETE FROM users WHERE username = :username")
    suspend fun deleteUserByUsername(username: String)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}
