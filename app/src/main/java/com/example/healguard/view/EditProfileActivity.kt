package com.example.healguard.view

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.healguard.R
import com.example.healguard.utils.PrefsManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : Activity() {

    private lateinit var backArrow: ImageView
    private lateinit var usernameEditText: EditText
    private lateinit var emailTextView: TextView
    private lateinit var saveChangesButton: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var realtimeDb: FirebaseDatabase
    private lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editprofile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        realtimeDb = FirebaseDatabase.getInstance()
        prefsManager = PrefsManager(this)

        initializeViews()
        setupClickListeners()
        loadUserData()
    }

    private fun initializeViews() {
        backArrow = findViewById(R.id.backArrow)
        usernameEditText = findViewById(R.id.edittext_Username)
        emailTextView = findViewById(R.id.edittext_Email) // This will now be a TextView
        saveChangesButton = findViewById(R.id.button_SaveChanges)
    }

    private fun setupClickListeners() {
        backArrow.setOnClickListener {
            finish()
        }

        saveChangesButton.setOnClickListener {
            saveProfileChanges()
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            emailTextView.text = user.email ?: "No email available"
            emailTextView.setTextColor(Color.parseColor("#888888")) // Gray color matching your hint color

            emailTextView.isEnabled = false
            emailTextView.isClickable = false
            emailTextView.isFocusable = false

            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("username") ?: ""
                        usernameEditText.setText(username)
                    } else {
                        loadUsernameFromRealtimeDb(user.uid)
                    }
                }
                .addOnFailureListener {
                    loadUsernameFromRealtimeDb(user.uid)
                }
        }
    }

    private fun loadUsernameFromRealtimeDb(userId: String) {
        realtimeDb.getReference("users").child(userId).child("username")
            .get()
            .addOnSuccessListener { dataSnapshot ->
                val username = dataSnapshot.getValue(String::class.java) ?: ""
                usernameEditText.setText(username)
            }
            .addOnFailureListener {
                val savedUsername = prefsManager.getUsername()
                usernameEditText.setText(savedUsername)
            }
    }

    private fun saveProfileChanges() {
        val username = usernameEditText.text.toString().trim()
        val currentUser = auth.currentUser

        if (username.isEmpty()) {
            usernameEditText.error = "Please enter username"
            return
        }

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        saveChangesButton.isEnabled = false
        saveChangesButton.text = "Saving..."

        val authenticatedEmail = currentUser.email ?: ""
        prefsManager.saveUserData(username, authenticatedEmail, currentUser.uid)
        updateFirebaseAuthDisplayName(currentUser, username, authenticatedEmail)
    }

    private fun updateFirebaseAuthDisplayName(user: com.google.firebase.auth.FirebaseUser, username: String, email: String) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(username)
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { profileTask ->
                if (profileTask.isSuccessful) {
                    saveToFirestore(user.uid, username, email)
                } else {
                    saveToFirestore(user.uid, username, email)
                }
            }
    }

    private fun saveToFirestore(userId: String, username: String, email: String) {
        val userData = hashMapOf(
            "username" to username,
            "email" to email,
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                saveToRealtimeDatabase(userId, username, email)
            }
            .addOnFailureListener { e ->
                saveToRealtimeDatabase(userId, username, email)
            }
    }

    private fun saveToRealtimeDatabase(userId: String, username: String, email: String) {
        val userData = hashMapOf(
            "username" to username,
            "email" to email,
            "updatedAt" to System.currentTimeMillis()
        )

        realtimeDb.getReference("users").child(userId)
            .setValue(userData)
            .addOnSuccessListener {
                onSaveSuccess(username)
            }
            .addOnFailureListener { e ->
                onSavePartialSuccess(username, "Profile saved locally (Cloud sync incomplete)")
            }
    }

    private fun onSaveSuccess(username: String) {
        val intent = Intent(this, SuccessfullyProfileActivity::class.java).apply {
            putExtra("username", username)
            putExtra("message", "Profile updated successfully!")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun onSavePartialSuccess(username: String, message: String) {
        val intent = Intent(this, SuccessfullyProfileActivity::class.java).apply {
            putExtra("username", username)
            putExtra("message", message)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        val currentUsername = usernameEditText.text.toString().trim()
        val originalUsername = prefsManager.getUsername()

        if (currentUsername != originalUsername) {
            Toast.makeText(this, "Changes not saved", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}