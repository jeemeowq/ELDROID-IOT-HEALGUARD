package com.example.healguard.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
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

            buttonSignUp.isEnabled = false
            buttonSignUp.text = "Creating Account..."

            presenter.register(username, email, password, confirmPassword)
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

    override fun onSuccess(message: String, user: User?) {
        buttonSignUp.isEnabled = true
        buttonSignUp.text = "Sign Up"

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        navigateToCompleteRegister()
    }

    override fun onError(message: String) {
        buttonSignUp.isEnabled = true
        buttonSignUp.text = "Sign Up"

        Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
    }

    private fun navigateToCompleteRegister() {
        startActivity(Intent(this, CompleteRegisterActivity::class.java))
        finish()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}