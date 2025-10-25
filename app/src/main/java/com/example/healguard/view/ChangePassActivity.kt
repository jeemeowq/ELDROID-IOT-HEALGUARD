package com.example.healguard.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.example.healguard.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePassActivity : Activity() {

    private lateinit var backArrow: ImageView
    private lateinit var oldPasswordInput: EditText
    private lateinit var newPasswordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var oldPasswordVisibilityIcon: ImageView
    private lateinit var newPasswordVisibilityIcon: ImageView
    private lateinit var confirmPasswordVisibilityIcon: ImageView
    private lateinit var saveChangesButton: Button

    private lateinit var auth: FirebaseAuth

    private var isOldPasswordVisible = false
    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_changepass)

        auth = FirebaseAuth.getInstance()

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        backArrow = findViewById(R.id.backArrow)
        oldPasswordInput = findViewById(R.id.edittext_oldpassword)
        newPasswordInput = findViewById(R.id.edittext_newpassword)
        confirmPasswordInput = findViewById(R.id.edittext_confirmpassword)
        oldPasswordVisibilityIcon = findViewById(R.id.imageview_oldpassword_visibility)
        newPasswordVisibilityIcon = findViewById(R.id.imageview_newpassword_visibility)
        confirmPasswordVisibilityIcon = findViewById(R.id.imageview_confirmpassword_visibility)
        saveChangesButton = findViewById(R.id.button_SaveChanges)
    }

    private fun setupClickListeners() {
        backArrow.setOnClickListener {
            navigateToProfile()
        }

        saveChangesButton.setOnClickListener {
            changePassword()
        }

        oldPasswordVisibilityIcon.setOnClickListener {
            toggleOldPasswordVisibility()
        }

        newPasswordVisibilityIcon.setOnClickListener {
            toggleNewPasswordVisibility()
        }

        confirmPasswordVisibilityIcon.setOnClickListener {
            toggleConfirmPasswordVisibility()
        }
    }

    private fun toggleOldPasswordVisibility() {
        if (isOldPasswordVisible) {
            oldPasswordInput.transformationMethod = PasswordTransformationMethod.getInstance()
            oldPasswordVisibilityIcon.setImageResource(R.drawable.ic_visibility_off)
        } else {
            oldPasswordInput.transformationMethod = HideReturnsTransformationMethod.getInstance()
            oldPasswordVisibilityIcon.setImageResource(R.drawable.ic_visibility)
        }
        isOldPasswordVisible = !isOldPasswordVisible
        oldPasswordInput.setSelection(oldPasswordInput.text.length)
    }

    private fun toggleNewPasswordVisibility() {
        if (isNewPasswordVisible) {
            newPasswordInput.transformationMethod = PasswordTransformationMethod.getInstance()
            newPasswordVisibilityIcon.setImageResource(R.drawable.ic_visibility_off)
        } else {
            newPasswordInput.transformationMethod = HideReturnsTransformationMethod.getInstance()
            newPasswordVisibilityIcon.setImageResource(R.drawable.ic_visibility)
        }
        isNewPasswordVisible = !isNewPasswordVisible
        newPasswordInput.setSelection(newPasswordInput.text.length)
    }

    private fun toggleConfirmPasswordVisibility() {
        if (isConfirmPasswordVisible) {
            confirmPasswordInput.transformationMethod = PasswordTransformationMethod.getInstance()
            confirmPasswordVisibilityIcon.setImageResource(R.drawable.ic_visibility_off)
        } else {
            confirmPasswordInput.transformationMethod = HideReturnsTransformationMethod.getInstance()
            confirmPasswordVisibilityIcon.setImageResource(R.drawable.ic_visibility)
        }
        isConfirmPasswordVisible = !isConfirmPasswordVisible
        confirmPasswordInput.setSelection(confirmPasswordInput.text.length)
    }

    private fun changePassword() {
        val oldPassword = oldPasswordInput.text.toString().trim()
        val newPassword = newPasswordInput.text.toString().trim()
        val confirmPassword = confirmPasswordInput.text.toString().trim()

        saveChangesButton.isEnabled = false

        if (oldPassword.isEmpty()) {
            oldPasswordInput.error = "Please enter old password"
            saveChangesButton.isEnabled = true
            return
        }

        if (newPassword.isEmpty()) {
            newPasswordInput.error = "Please enter new password"
            saveChangesButton.isEnabled = true
            return
        }

        if (newPassword.length < 6) {
            newPasswordInput.error = "Password must be at least 6 characters"
            saveChangesButton.isEnabled = true
            return
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.error = "Please confirm new password"
            saveChangesButton.isEnabled = true
            return
        }

        if (newPassword != confirmPassword) {
            confirmPasswordInput.error = "Passwords do not match"
            saveChangesButton.isEnabled = true
            return
        }

        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(
                this,
                "User not authenticated. Please sign in again.",
                Toast.LENGTH_SHORT
            ).show()
            saveChangesButton.isEnabled = true
            return
        }

        if (currentUser.email.isNullOrEmpty()) {
            Toast.makeText(
                this,
                "Email not available. Please sign in again.",
                Toast.LENGTH_SHORT
            ).show()
            saveChangesButton.isEnabled = true
            return
        }

        val hasEmailPassword = currentUser.providerData.any { it.providerId == "password" }
        if (!hasEmailPassword) {
            Toast.makeText(
                this,
                "Password change not available for social login accounts.",
                Toast.LENGTH_SHORT
            ).show()
            saveChangesButton.isEnabled = true
            return
        }

        val credential = EmailAuthProvider.getCredential(currentUser.email!!, oldPassword)

        currentUser.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    currentUser.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            saveChangesButton.isEnabled = true

                            if (updateTask.isSuccessful) {
                                Toast.makeText(
                                    this,
                                    "Password changed successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                navigateToSuccessPassword()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Failed to change password: ${updateTask.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                } else {
                    saveChangesButton.isEnabled = true
                    Toast.makeText(
                        this,
                        "Authentication failed: Incorrect old password",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun navigateToProfile() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToSuccessPassword() {
        val intent = Intent(this, SuccessfullyPasswordActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        navigateToProfile()
    }
}