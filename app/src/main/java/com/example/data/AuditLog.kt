package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String, // "ADD_CONTACT", "DELETE_CONTACT", "UPDATE_CONTACT", "ADD_USER", "DELETE_USER", "UPDATE_USER"
    val itemName: String,
    val details: String,
    val performedBy: String
)
