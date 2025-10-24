package com.example.healguard.presenter

import com.example.healguard.model.User

interface IAuthView {
    fun onSuccess(message: String, user: User?)
    fun onError(message: String)
}