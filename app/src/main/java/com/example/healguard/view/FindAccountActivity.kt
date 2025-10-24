package com.example.healguard.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.example.healguard.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FindAccountActivity : Activity() {

    private lateinit var backArrow: ImageView
    private lateinit var emailInput: EditText
    private lateinit var continueButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_findaccount)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        backArrow = findViewById(R.id.backArrow)
        emailInput = findViewById(R.id.edittext_Email)
        continueButton = findViewById(R.id.buttonContinue)
    }

    private fun setupClickListeners() {
        backArrow.setOnClickListener {
            navigateToLogin()
        }

        continueButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (validateEmail(email)) {
                checkEmailAndGetUsername(email)
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        if (email.isEmpty()) {
            emailInput.error = "Email is required"
            emailInput.requestFocus()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Please enter a valid email address"
            emailInput.requestFocus()
            return false
        }

        return true
    }

    private fun checkEmailAndGetUsername(email: String) {
        continueButton.isEnabled = false
        continueButton.text = "Checking..."

        // First update Firestore rules to allow this query without authentication
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                continueButton.isEnabled = true
                continueButton.text = "Continue"

                if (documents.isEmpty) {
                    emailInput.error = "No account found with this email address"
                    emailInput.requestFocus()
                    return@addOnSuccessListener
                }

                // Get the username from Firestore - it should be "Mainit"
                val document = documents.documents.first()
                val username = document.getString("username") ?: "User"

                // Debug output to verify
                println("DEBUG: Found username: $username")
                println("DEBUG: All document data: ${document.data}")

                // Send password reset email with the actual username
                sendPasswordResetEmail(email, username)
            }
            .addOnFailureListener { exception ->
                continueButton.isEnabled = true
                continueButton.text = "Continue"

                println("DEBUG: Firestore error: ${exception.message}")

                if (exception.message?.contains("PERMISSION_DENIED") == true) {
                    Toast.makeText(
                        this,
                        "Database permission denied. Please contact support.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Error: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun sendPasswordResetEmail(email: String, username: String) {
        continueButton.isEnabled = false
        continueButton.text = "Sending..."

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                continueButton.isEnabled = true
                continueButton.text = "Continue"

                if (task.isSuccessful) {
                    // Success - navigate with the actual username "Mainit"
                    val intent = Intent(this, EmailMessageLinkActivity::class.java).apply {
                        putExtra("USERNAME", username)  // This should be "Mainit"
                        putExtra("EMAIL", email)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    val errorMessage = when {
                        task.exception?.message?.contains("user-not-found") == true ->
                            "No account found with this email address"
                        task.exception?.message?.contains("invalid-email") == true ->
                            "Invalid email address format"
                        else ->
                            "Failed to send reset email. Please try again."
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        navigateToLogin()
    }
}