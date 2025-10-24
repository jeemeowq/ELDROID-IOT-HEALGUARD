package com.example.healguard.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.healguard.R
import com.example.healguard.model.User
import com.example.healguard.presenter.AuthPresenter
import com.example.healguard.presenter.IAuthView

class RegisterActivity : Activity(), IAuthView {

    private lateinit var presenter: AuthPresenter

    private lateinit var editUsername: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var editConfirmPassword: EditText
    private lateinit var passwordVisibilityIcon: ImageView
    private lateinit var confirmPasswordVisibilityIcon: ImageView
    private lateinit var buttonSignUp: Button
    private lateinit var buttonSignIn: TextView

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        presenter = AuthPresenter(this)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        editUsername = findViewById(R.id.edittext_Username)
        editEmail = findViewById(R.id.edittext_Email)
        editPassword = findViewById(R.id.edittext_Password)
        editConfirmPassword = findViewById(R.id.edittext_ConfirmPassword)
        passwordVisibilityIcon = findViewById(R.id.imageview_PasswordVisibility)
        confirmPasswordVisibilityIcon = findViewById(R.id.imageview_ConfirmPasswordVisibility)
        buttonSignUp = findViewById(R.id.button_SignUp)
        buttonSignIn = findViewById(R.id.edittext_SignIn)
    }

    private fun setupClickListeners() {
        buttonSignUp.setOnClickListener {
            val username = editUsername.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val password = editPassword.text.toString().trim()
            val confirmPassword = editConfirmPassword.text.toString().trim()

            clearErrors()

            if (validateInputs(username, email, password, confirmPassword)) {
                buttonSignUp.isEnabled = false
                buttonSignUp.text = "Creating Account..."

                Log.d("RegisterActivity", "Attempting registration for: $email")

                presenter.register(username, email, password, confirmPassword)
            }
        }

        buttonSignIn.setOnClickListener {
            navigateToLogin()
        }

        passwordVisibilityIcon.setOnClickListener {
            togglePasswordVisibility()
        }

        confirmPasswordVisibilityIcon.setOnClickListener {
            toggleConfirmPasswordVisibility()
        }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            editPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            passwordVisibilityIcon.setImageResource(R.drawable.ic_visibility_off)
        } else {
            editPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            passwordVisibilityIcon.setImageResource(R.drawable.ic_visibility)
        }
        isPasswordVisible = !isPasswordVisible
        editPassword.setSelection(editPassword.text.length)
    }

    private fun toggleConfirmPasswordVisibility() {
        if (isConfirmPasswordVisible) {
            editConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            confirmPasswordVisibilityIcon.setImageResource(R.drawable.ic_visibility_off)
        } else {
            editConfirmPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            confirmPasswordVisibilityIcon.setImageResource(R.drawable.ic_visibility)
        }
        isConfirmPasswordVisible = !isConfirmPasswordVisible
        editConfirmPassword.setSelection(editConfirmPassword.text.length)
    }

    private fun clearErrors() {
        editUsername.error = null
        editEmail.error = null
        editPassword.error = null
        editConfirmPassword.error = null
    }

    private fun validateInputs(username: String, email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true

        if (username.isEmpty()) {
            editUsername.error = "Username is required"
            editUsername.requestFocus()
            isValid = false
        } else if (username.length < 3) {
            editUsername.error = "Username must be at least 3 characters"
            editUsername.requestFocus()
            isValid = false
        }

        if (email.isEmpty()) {
            editEmail.error = "Email is required"
            editEmail.requestFocus()
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.error = "Please enter a valid email address"
            editEmail.requestFocus()
            isValid = false
        }

        if (password.isEmpty()) {
            editPassword.error = "Password is required"
            editPassword.requestFocus()
            isValid = false
        } else if (password.length < 6) {
            editPassword.error = "Password must be at least 6 characters"
            editPassword.requestFocus()
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            editConfirmPassword.error = "Please confirm your password"
            editConfirmPassword.requestFocus()
            isValid = false
        } else if (password != confirmPassword) {
            editConfirmPassword.error = "Passwords do not match"
            editConfirmPassword.requestFocus()
            isValid = false
        }

        return isValid
    }

    override fun onSuccess(message: String, user: User?) {
        buttonSignUp.isEnabled = true
        buttonSignUp.text = "Sign Up"

        Log.d("RegisterActivity", "Registration successful: $message")
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        navigateToLogin()
    }

    override fun onError(message: String) {
        buttonSignUp.isEnabled = true
        buttonSignUp.text = "Sign Up"

        Log.e("RegisterActivity", "Registration error: $message")
        Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}