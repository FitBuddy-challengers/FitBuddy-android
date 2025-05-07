package com.cookandroid.challengers.auth.login

data class LoginResponse(
    val message: String,
    val user: UserData? = null
)

data class UserData(
    val id: Int,
    val email: String,
    val name: String?,
    val age_group: String?,
    val gender: String?,
    val height: Int?,
    val weight: Int?,
    val diseases: String?,
    val workout_level: String?,
    val preferred_workouts: String?,
    val equipment: String?,
    val created_at: String?
)
