package com.example.healguard.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.healguard.R

class EmailMessageLinkActivity : Activity() {

    private lateinit var backArrow: ImageView
    private lateinit var goToLoginButton: Button
    private lateinit var usernameText: TextView
    private lateinit var emailText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emailmessagelink)

        initializeViews()
        setupUserData()
        setupClickListeners()
    }

    private fun initializeViews() {
        backArrow = findViewById(R.id.backArrow)
        goToLoginButton = findViewById(R.id.buttonGoLogin)
        usernameText = findViewById(R.id.edittext_Username)
        emailText = findViewById(R.id.textSubtitle)
    }

    private fun setupUserData() {
        val username = intent.getStringExtra("USERNAME") ?: "User"
        val email = intent.getStringExtra("EMAIL") ?: "your email"

        // This will now show "Hi Mainit!" instead of "Hi User!"
        usernameText.text = "Hi $username!"

        val formattedText = "We've sent a password reset link to:<br><b>$email</b><br><br>Please check your email to continue."

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            emailText.text = Html.fromHtml(formattedText, Html.FROM_HTML_MODE_LEGACY)
        } else {
            emailText.text = Html.fromHtml(formattedText)
        }

        // Debug output
        println("DEBUG: EmailMessageLink - Username: $username")
        println("DEBUG: EmailMessageLink - Email: $email")
    }

    private fun setupClickListeners() {
        backArrow.setOnClickListener {
            navigateToLogin()
        }

        goToLoginButton.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        navigateToLogin()
    }
}