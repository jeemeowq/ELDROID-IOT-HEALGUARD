package com.example.healguard.model

import com.google.firebase.firestore.Exclude

data class Medicine(
    @get:Exclude var id: String = "",
    var name: String = "",
    var usage: String = "",
    var description: String = "",
    var time: String? = null,
    var type: String? = null,
    var timing: String? = null
) {
    // Empty constructor for Firestore
    constructor() : this("", "", "", "", null, null, null)
}