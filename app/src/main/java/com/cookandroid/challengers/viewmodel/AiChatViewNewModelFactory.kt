package com.cookandroid.challengers.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cookandroid.challengers.api.AiRoutineApi
import com.cookandroid.challengers.data.ExercisePlanDao
import com.cookandroid.challengers.data.ExerciseSetDao
import com.cookandroid.challengers.data.PlanDetailDao

class AiChatViewNewModelFactory(
    private val api: AiRoutineApi,
    private val userId: Int,
    private val exercisePlanDao: ExercisePlanDao,
    private val planDetailDao: PlanDetailDao,
    private val exerciseSetDao: ExerciseSetDao
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AiChatViewNewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AiChatViewNewModel(api, userId, exercisePlanDao,planDetailDao,
                exerciseSetDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}