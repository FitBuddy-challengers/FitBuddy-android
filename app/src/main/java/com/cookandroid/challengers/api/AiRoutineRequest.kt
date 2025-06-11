package com.cookandroid.challengers.api

import com.cookandroid.challengers.model.ScheduleInfo
import com.cookandroid.challengers.model.UserInfo
import com.google.gson.annotations.SerializedName

data class AiRoutineRequest(
    @SerializedName("user_info")
    val userInfo: UserInfo,

    @SerializedName("schedule_info")
    val scheduleInfo: ScheduleInfo
)