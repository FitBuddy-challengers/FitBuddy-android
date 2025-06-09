package com.cookandroid.challengers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cookandroid.challengers.api.ExerciseApi
import com.cookandroid.challengers.repository.AiWorkoutRepository
import com.cookandroid.challengers.util.UserPreference

class AiChatViewModelFactory(
    private val workoutRepository: AiWorkoutRepository,
    private val exerciseApi: ExerciseApi,
    private val userPreference: UserPreference
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AiChatViewModel::class.java)) {
            return AiChatViewModel(
                workoutRepository = workoutRepository,
                exerciseApi = exerciseApi,
                userPreference = userPreference
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
