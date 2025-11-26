package com.example.healguard.utils

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val sharedPref: SharedPreferences = context.getSharedPreferences("HealGuardPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_USER_ID = "user_id"
    }

    fun saveUserData(username: String, email: String, userId: String) {
        with(sharedPref.edit()) {
            putString(KEY_USERNAME, username)
            putString(KEY_EMAIL, email)
            putString(KEY_USER_ID, userId)
            apply()
        }
    }

    fun getUsername(): String {
        return sharedPref.getString(KEY_USERNAME, "User") ?: "User"
    }

    fun getEmail(): String {
        return sharedPref.getString(KEY_EMAIL, "") ?: ""
    }

    fun getUserId(): String {
        return sharedPref.getString(KEY_USER_ID, "") ?: ""
    }

    fun clearUserData() {
        with(sharedPref.edit()) {
            clear()
            apply()
        }
    }
}