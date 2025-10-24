package com.example.healguard.model

class MedicineRepository {
    private val medicines = listOf(
        Medicine(
            "Paracetamol",
            "Used to treat pain and fever.",
            "Take 500mg every 4–6 hours as needed. Do not exceed 4g/day."
        ),
        Medicine(
            "Amoxicillin",
            "An antibiotic for bacterial infections.",
            "Take as prescribed, usually 500mg every 8 hours for 7–10 days."
        ),
        Medicine(
            "Cetirizine",
            "Used to relieve allergy symptoms.",
            "Take 10mg once daily."
        )
    )
    fun getMedicines(): List<Medicine> = medicines
}
