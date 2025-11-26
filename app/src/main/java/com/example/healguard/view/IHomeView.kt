package com.example.healguard.view

import com.example.healguard.model.Medicine

interface IHomeView {
    fun showMedicines(medicines: List<Medicine>)
}
