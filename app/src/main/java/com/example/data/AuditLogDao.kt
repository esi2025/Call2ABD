package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLog)

    @Query("DELETE FROM audit_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM audit_logs WHERE actionType LIKE 'ERROR_%'")
    suspend fun clearErrorLogs()
}
