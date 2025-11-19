package com.cookandroid.challengers.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cookandroid.challengers.api.AiRoutineApi

class AiChatViewNewModelFactory(
    private val api: AiRoutineApi
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AiChatViewNewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AiChatViewNewModel(api) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}