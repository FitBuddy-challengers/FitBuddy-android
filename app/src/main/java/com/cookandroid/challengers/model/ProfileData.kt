package com.cookandroid.challengers.model

data class ProfileData(
    val email: String,
    val name: String,
    val age_group: String,
    val gender: String,
    val height: Int,
    val weight: Int,
    val diseases: List<String>,
    val workout_level: String,
    val preferred_workouts: List<String>,
    val equipment: List<String>
)