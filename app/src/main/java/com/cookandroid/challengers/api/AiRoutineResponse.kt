package com.cookandroid.challengers.api

import com.google.gson.annotations.SerializedName

data class AiRoutineResponse(
    @SerializedName("plan_text")
    val planText: String
)