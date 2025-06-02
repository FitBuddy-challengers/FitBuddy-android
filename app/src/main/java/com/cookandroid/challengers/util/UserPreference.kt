package com.cookandroid.challengers.util

import android.content.Context
import android.content.SharedPreferences

class UserPreference(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val USER_ID = "userId"
        private const val IS_LOGGED_IN = "isLoggedIn"
    }

    fun saveLogin(userId: Int) {
        prefs.edit().putInt(USER_ID, userId).putBoolean(IS_LOGGED_IN, true).apply()
    }

    fun getUserId(): Int = prefs.getInt(USER_ID, -1)

    fun isLoggedIn(): Boolean = prefs.getBoolean(IS_LOGGED_IN, false)

    fun logout() {
        prefs.edit().clear().apply()
    }
}