package com.example.healguard.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.example.healguard.R

class CompleteRegisterActivity : Activity() {

    private lateinit var buttonGoLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_completeregister)

        buttonGoLogin = findViewById(R.id.buttonGoLogin)

        buttonGoLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}