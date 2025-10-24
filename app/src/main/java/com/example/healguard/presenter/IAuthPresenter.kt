package com.example.healguard.presenter

interface IAuthPresenter {
    fun register(username: String, email: String, password: String, confirmPassword: String)
    fun login(email: String, password: String)
}