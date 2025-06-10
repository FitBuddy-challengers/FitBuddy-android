package com.cookandroid.challengers.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class UserPreference(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val USER_ID = "userId"
        private const val IS_LOGGED_IN = "isLoggedIn"
        private const val EQUIPPED_ITEMS = "equippedItems" // 착용 아이템 저장 키

    }

    fun saveLogin(userId: Int) {
        prefs.edit().putInt(USER_ID, userId).putBoolean(IS_LOGGED_IN, true).apply()
    }

    fun getUserId(): Int = prefs.getInt(USER_ID, -1)

    fun isLoggedIn(): Boolean = prefs.getBoolean(IS_LOGGED_IN, false)

    fun logout() {
        prefs.edit().clear().apply()
    }

    // 착용 아이템 ID 맵을 JSON 문자열로 저장
    fun saveEquippedItemIds(equippedMap: Map<String, Int?>) {
        val jsonString = gson.toJson(equippedMap)
        prefs.edit().putString(EQUIPPED_ITEMS, jsonString).apply()
    }

    // JSON 문자열을 읽어 착용 아이템 ID 맵으로 변환
    fun getEquippedItemIds(): Map<String, Int?> {
        val jsonString = prefs.getString(EQUIPPED_ITEMS, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, Int?>>() {}.type
        return gson.fromJson(jsonString, type) ?: emptyMap()
    }

}