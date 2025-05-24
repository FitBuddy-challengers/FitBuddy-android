package com.cookandroid.challengers

data class WeekDate(
    val date: String,
    val isToday: Boolean = false,
    val hasWorkout: Boolean = false
)