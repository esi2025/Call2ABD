package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val jobTitle: String,
    val department: String,
    val shortCode: String, // 5-digit code
    val mobileNumber: String, // hidden from user UI, but used in calling intents
    val announcedNumber: String = "" // New column "شماره اعلام شده"
)
