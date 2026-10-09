package com.example.openmeteoweather.data.remote.imd

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IMDTokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "imd_auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String, expiresIn: Long) {
        val expiryTime = System.currentTimeMillis() + (expiresIn * 1000)
        sharedPreferences.edit()
            .putString("access_token", token)
            .putLong("token_expiry", expiryTime)
            .apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString("access_token", null)
    }

    fun isTokenExpired(): Boolean {
        val expiry = sharedPreferences.getLong("token_expiry", 0L)
        // Buffer of 60 seconds to prevent expiry issues mid-flight
        return System.currentTimeMillis() >= (expiry - 60000)
    }

    fun clearToken() {
        sharedPreferences.edit().clear().apply()
    }
}
