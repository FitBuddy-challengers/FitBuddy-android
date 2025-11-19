package com.cookandroid.challengers.api

import com.google.gson.annotations.SerializedName


data class AiRoutineResponse(
    val routine_text: String,
    val exercises: List<ExerciseItem>
)

data class ExerciseItem(
    val name: String,
    val sets: Int,
    val reps: Int? = null,
    val seconds: Int? = null
)

/**
 * {
 *   "routine_text": "오늘도 화이팅🔥...",
 *   "exercises": [
 *     { "name": "스쿼트", "sets": 3, "reps": 15 }
 *   ]
 * }
 * 서버는 다음과 같이 내려줍니다! 해서 api 연동 수정했습니다:) -윤지-
 */
