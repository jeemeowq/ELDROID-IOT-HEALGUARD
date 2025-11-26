package com.example.healguard.utils

import android.util.Patterns

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidUsername(username: String): Boolean {
        return username.isNotEmpty() && username.length >= 3
    }

    fun isValidPassword(password: String): Boolean {
        return password.isNotEmpty() && password.length >= 8
    }

    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }
}
