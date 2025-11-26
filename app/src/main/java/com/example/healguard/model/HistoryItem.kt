package com.example.healguard.model


data class HistoryItem(
    var id: String = "",
    val date: String = "",
    val time: String = "",
    val action: String = "",
    val medicineName: String = "",
    val dosage: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)