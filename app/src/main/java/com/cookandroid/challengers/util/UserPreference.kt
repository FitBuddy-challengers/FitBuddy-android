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
        private const val USER_NAME = "userName"
        private const val USER_EMAIL = "userEmail"
        private const val LAST_VISIT_DATE = "lastVisitDate"

    }

//    fun saveLogin(userId: Int) {
//        prefs.edit().putInt(USER_ID, userId).putBoolean(IS_LOGGED_IN, true).apply()
//    }
    fun saveLogin(userId: Int) {
        prefs.edit()
            .putInt(USER_ID, userId)
            .putBoolean(IS_LOGGED_IN, true)
            .commit() // ✅ apply() → commit() : 저장이 즉시, 동기적으로 완료됨
    } //이전은 회원가입 이후 진입시 오류가 났었음! 수정함.

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

    // 닉네임 저장 및 조회
    fun saveUserName(name: String) {
        prefs.edit().putString(USER_NAME, name).apply()
    }

    fun getUserName(): String {
        return prefs.getString(USER_NAME, "") ?: ""
    }

    // 이메일 저장 및 조회
    fun saveUserEmail(email: String) {
        prefs.edit().putString(USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String {
        return prefs.getString(USER_EMAIL, "") ?: ""
    }

    // 접속일 저장 및 조회
    fun saveLastVisitDate() {
        prefs.edit().putLong(LAST_VISIT_DATE, System.currentTimeMillis()).apply()
    }
    fun getLastVisitDate(): Long {
        return prefs.getLong(LAST_VISIT_DATE, 0L)
    }

    // 알림 저장 및 조회
    fun setNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notification_enabled", enabled).apply()
    }

    fun isNotificationEnabled(): Boolean {
        return prefs.getBoolean("notification_enabled", true) // 기본값
    }
}