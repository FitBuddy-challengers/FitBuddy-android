package com.cookandroid.challengers

data class WeekDate(
    val date: String,            // 예: "6"
    val fullDate: String,        // 예: "2025-06-06" ← ✨ 이거 추가!
    val isToday: Boolean = false,
    val hasWorkout: Boolean = false
)