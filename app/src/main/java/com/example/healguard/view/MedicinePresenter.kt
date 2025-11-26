package com.example.healguard.presenter

import com.example.healguard.model.MedicineRepository
import com.example.healguard.view.IHomeView

class MedicinePresenter(private val view: IHomeView) : IMedicinePresenter {

    private val repository = MedicineRepository()

    override fun loadMedicines() {
        val data = repository.getMedicines()
        view.showMedicines(data)
    }
}
