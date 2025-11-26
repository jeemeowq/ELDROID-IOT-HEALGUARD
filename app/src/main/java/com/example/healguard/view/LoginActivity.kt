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
import com.example.healguard.utils.PrefsManager

class LoginActivity : Activity(), IAuthView {

    private lateinit var presenter: AuthPresenter
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var passwordVisibilityIcon: ImageView
    private lateinit var loginButton: Button
    private lateinit var registerButton: TextView
    private lateinit var forgotPasswordButton: TextView
    private lateinit var prefsManager: PrefsManager

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        prefsManager = PrefsManager(this)
        presenter = AuthPresenter(this)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        emailInput = findViewById(R.id.edittext_Email)
        passwordInput = findViewById(R.id.edittext_Password)
        passwordVisibilityIcon = findViewById(R.id.imageview_PasswordVisibility)
        loginButton = findViewById(R.id.button_SignIn)
        registerButton = findViewById(R.id.textview_SignUp)
        forgotPasswordButton = findViewById(R.id.textview_ForgotPassword)
    }

    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            presenter.login(email, password)
        }

        registerButton.setOnClickListener {
            navigateToRegister()
        }

        forgotPasswordButton.setOnClickListener {
            navigateToFindAccount()
        }

        passwordVisibilityIcon.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordInput.transformationMethod = PasswordTransformationMethod.getInstance()
            passwordVisibilityIcon.setImageResource(R.drawable.ic_visibility_off)
        } else {
            passwordInput.transformationMethod = HideReturnsTransformationMethod.getInstance()
            passwordVisibilityIcon.setImageResource(R.drawable.ic_visibility)
        }
        isPasswordVisible = !isPasswordVisible
        passwordInput.setSelection(passwordInput.text.length)
    }

    override fun onSuccess(message: String, user: User?) {
        user?.let {
            prefsManager.saveUserData(it.username, it.email ?: "", it.id)

            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("user_id", it.id)
            intent.putExtra("username", it.username)
            intent.putExtra("email", it.email)
            startActivity(intent)
            finish()
        } ?: run {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onError(message: String) {
        // Error handled by AuthPresenter
    }

    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToFindAccount() {
        val intent = Intent(this, FindAccountActivity::class.java)
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}