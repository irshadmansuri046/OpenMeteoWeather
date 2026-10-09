package com.example.openmeteoweather.data.remote.imd.model

import com.google.gson.annotations.SerializedName

data class IMDAuthRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)
