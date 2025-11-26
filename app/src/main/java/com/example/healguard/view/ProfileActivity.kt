package com.example.healguard.view

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.healguard.R
import com.example.healguard.utils.PrefsManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : Activity() {

    private lateinit var backArrow: ImageView
    private lateinit var editProfileText: TextView
    private lateinit var changePasswordText: TextView
    private lateinit var logoutButton: Button
    private lateinit var textViewName: TextView
    private lateinit var textViewEmail: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var prefsManager: PrefsManager

    companion object {
        private const val REQUEST_CODE_EDIT_PROFILE = 1001
        private const val REQUEST_CODE_CHANGE_PASSWORD = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        prefsManager = PrefsManager(this)

        initializeViews()
        setupClickListeners()
        loadUserData()
    }

    private fun initializeViews() {
        backArrow = findViewById(R.id.backArrow)
        editProfileText = findViewById(R.id.textView_EditProfile)
        changePasswordText = findViewById(R.id.textView_ChangePassword)
        logoutButton = findViewById(R.id.button_Logout)
        textViewName = findViewById(R.id.textView_Name)
        textViewEmail = findViewById(R.id.textView_Email)
    }

    private fun setupClickListeners() {
        backArrow.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }

        editProfileText.setOnClickListener {
            navigateToEditProfile()
        }

        changePasswordText.setOnClickListener {
            navigateToChangePassword()
        }

        logoutButton.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadUserData() {
        val savedUsername = prefsManager.getUsername()
        val savedEmail = prefsManager.getEmail()
        textViewName.text = savedUsername
        textViewEmail.text = savedEmail

        val currentUser = auth.currentUser
        currentUser?.let { user ->
            if (savedEmail.isEmpty()) {
                textViewEmail.text = user.email ?: "No email"
            }
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("username") ?: savedUsername
                        val email = document.getString("email") ?: savedEmail

                        textViewName.text = username
                        textViewEmail.text = email

                        prefsManager.saveUserData(username, email, user.uid)
                    }
                }
                .addOnFailureListener {
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_EDIT_PROFILE -> {
                if (resultCode == Activity.RESULT_OK) {
                    loadUserData()
                    setResult(Activity.RESULT_OK)
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                }
            }

            REQUEST_CODE_CHANGE_PASSWORD -> {
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun navigateToEditProfile() {
        val intent = Intent(this, EditProfileActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_EDIT_PROFILE)
    }

    private fun navigateToChangePassword() {
        val intent = Intent(this, ChangePassActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_CHANGE_PASSWORD)
    }

    private fun showLogoutConfirmation() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.logout_popup, null)
        val logoutBtn = dialogView.findViewById<Button>(R.id.logoutBtn)
        val cancelBtn = dialogView.findViewById<Button>(R.id.cancelBtn)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)

        logoutBtn.setOnClickListener {
            logoutUser()
            dialog.dismiss()
        }

        cancelBtn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun logoutUser() {
        prefsManager.clearUserData()
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }
}
