package com.example.healguard.presenter

import android.util.Log
import com.example.healguard.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class AuthPresenter(private val view: IAuthView) : IAuthPresenter {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance() // ✅ Use Firestore

    override fun register(username: String, email: String, password: String, confirmPassword: String) {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            view.onError("All fields are required")
            return
        }

        if (password != confirmPassword) {
            view.onError("Passwords do not match")
            return
        }

        if (password.length < 6) {
            view.onError("Password must be at least 6 characters")
            return
        }

        if (!isEmailValid(email)) {
            view.onError("Please enter a valid email address")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(username)
                            .build()

                        firebaseUser.updateProfile(profileUpdates)
                            .addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {

                                    val user = User(
                                        id = firebaseUser.uid,
                                        username = username,
                                        email = email,
                                        createdAt = System.currentTimeMillis()
                                    )

                                    saveUserToFirestore(user) // ✅ Save to Firestore
                                } else {
                                    view.onError("Failed to update user profile: ${profileTask.exception?.message}")
                                }
                            }
                    }
                } else {
                    val errorMessage = getFirebaseErrorMessage(task.exception?.message)
                    view.onError(errorMessage)
                }
            }
    }

    override fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            view.onError("Email and password are required")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        retrieveUserFromFirestore(firebaseUser.uid) // ✅ Fetch from Firestore
                    }
                } else {
                    val errorMessage = getFirebaseErrorMessage(task.exception?.message)
                    view.onError(errorMessage)
                }
            }
    }

    // ✅ NEW: Save user to Firestore
    private fun saveUserToFirestore(user: User) {
        db.collection("users").document(user.id)
            .set(user)
            .addOnSuccessListener {
                Log.d("AuthPresenter", "User data saved successfully to Firestore")
                view.onSuccess("Registration successful! Please login.", user)
            }
            .addOnFailureListener { e ->
                Log.e("AuthPresenter", "Failed to save user data: ${e.message}")
                view.onError("Registration successful but failed to save user data")
            }
    }

    // ✅ NEW: Retrieve user from Firestore
    private fun retrieveUserFromFirestore(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    view.onSuccess("Login successful!", user)
                } else {
                    view.onError("User data not found")
                }
            }
            .addOnFailureListener { e ->
                view.onError("Login successful but failed to load user data")
            }
    }

    private fun isEmailValid(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex())
    }

    private fun getFirebaseErrorMessage(error: String?): String {
        return when {
            error == null -> "An unknown error occurred"
            error.contains("email address is badly formatted") -> "Please enter a valid email address"
            error.contains("password is invalid") -> "Password is invalid"
            error.contains("email address is already in use") -> "This email is already registered"
            error.contains("network error") -> "Network error. Please check your internet connection"
            error.contains("too many attempts") -> "Too many attempts. Please try again later"
            else -> error
        }
    }
}