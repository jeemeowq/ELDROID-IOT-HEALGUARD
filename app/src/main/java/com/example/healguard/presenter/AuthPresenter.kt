package com.example.healguard.presenter

import com.example.healguard.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class AuthPresenter(private val view: IAuthView) : IAuthPresenter {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun register(username: String, email: String, password: String, confirmPassword: String) {
        when {
            username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                view.onError("All fields are required")
                return
            }
            username.length < 3 -> {
                view.onError("Name should be 3 characters long")
                return
            }
            password.length < 6 -> {
                view.onError("Password should be 6 characters long")
                return
            }
            password != confirmPassword -> {
                view.onError("Password should match")
                return
            }
            !isEmailValid(email) -> {
                view.onError("Please enter a valid email address")
                return
            }
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
                                val user = User(
                                    id = firebaseUser.uid,
                                    username = username,
                                    email = email,
                                    createdAt = System.currentTimeMillis()
                                )

                                view.onSuccess("Registration successful", user)
                                saveUserToFirestoreInBackground(user)
                            }
                    }
                } else {
                    val errorMessage = getFirebaseErrorMessage(task.exception?.message)
                    view.onError(errorMessage)
                }
            }
    }

    private fun saveUserToFirestoreInBackground(user: User) {
        val userData = hashMapOf(
            "id" to user.id,
            "username" to user.username,
            "email" to user.email,
            "createdAt" to user.createdAt
        )

        db.collection("users").document(user.id)
            .set(userData)
            .addOnSuccessListener {
                // Firestore save successful
            }
            .addOnFailureListener { e ->
                // Firestore save failed - continue without error
            }
    }

    override fun login(email: String, password: String) {
        when {
            email.isEmpty() || password.isEmpty() -> {
                view.onError("Email and password are required")
                return
            }
            !isEmailValid(email) -> {
                view.onError("Please enter a valid email address")
                return
            }
            password.length < 6 -> {
                view.onError("Password must be at least 6 characters")
                return
            }
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        retrieveUserFromFirestore(firebaseUser.uid)
                    }
                } else {
                    val errorMessage = getFirebaseErrorMessage(task.exception?.message)
                    view.onError(errorMessage)
                }
            }
    }

    private fun retrieveUserFromFirestore(userId: String) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username") ?: ""
                    val email = document.getString("email") ?: ""
                    val createdAt = document.getLong("createdAt") ?: 0L
                    val user = User(userId, username, email, createdAt)
                    view.onSuccess("Login successful", user)
                } else {
                    view.onError("User data not found")
                }
            }
            .addOnFailureListener { e ->
                view.onError("Failed to load user data")
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