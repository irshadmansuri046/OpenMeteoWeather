package com.example.openmeteoweather.data.remote.imd.model

import com.google.gson.annotations.SerializedName

data class IMDTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String?,
    @SerializedName("expires_in")
    val expiresIn: Long?
)
