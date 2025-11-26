package com.example.healguard.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.example.healguard.R

class SuccessfullyPasswordActivity : Activity() {

    private lateinit var goDashboardButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sucessfullypass)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        goDashboardButton = findViewById(R.id.button_GoDashboard)
    }

    private fun setupClickListeners() {
        goDashboardButton.setOnClickListener {
            navigateToHome()
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
    override fun onBackPressed() {
        navigateToHome()
    }
}