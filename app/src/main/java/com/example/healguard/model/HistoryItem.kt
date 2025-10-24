package com.example.healguard.model

import java.util.*

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val date: String = "",
    val time: String = "",
    val action: String = "",
    val medicineName: String = "",
    val dosage: String = "",
    val message: String = "", // ✅ Added message field
    val timestamp: Long = System.currentTimeMillis()
)