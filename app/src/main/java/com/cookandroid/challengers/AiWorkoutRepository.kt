package com.cookandroid.challengers

import com.cookandroid.challengers.data.Exercise
import com.cookandroid.challengers.data.ExercisePlan
import com.cookandroid.challengers.data.PlanDetail
import com.cookandroid.challengers.data.PlanDetailDao
import com.cookandroid.challengers.data.ExercisePlanDao
import com.cookandroid.challengers.data.ExerciseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiWorkoutRepository(
    private val planDetailDao: PlanDetailDao,
    private val exercisePlanDao: ExercisePlanDao,
    private val exerciseDao: ExerciseDao
) {
    suspend fun createPlanWithDetails(
        plannedDate: Long,
        parsedList: List<Triple<String, Int?, Int?>>
    ): Long = withContext(Dispatchers.IO) {
        val plan = ExercisePlan(plannedDate = plannedDate)
        val planId = exercisePlanDao.insert(plan)

        parsedList.forEachIndexed { index, (name, reps, sets) ->

            val existingExercise = exerciseDao.getExerciseByName(name)

//            val exercise = if (existingExercise != null) {
//                existingExercise
//            } else {
//                // 임시 처리, 삭제 필
//                val newExercise = Exercise(
//                    name = name,
//                    part = "기타",
//                    equip = "맨몸",
//                    mets = 3.0
//                )
//                val newId = exerciseDao.insert(newExercise)
//                newExercise.copy(id = newId)
//            }
//
//            val planDetail = PlanDetail(
//                exercisePlanId = planId,
//                exerciseId = exercise.id,
//                exOrder = index,
//                reps = reps ?: 10,
//                sets = sets ?: 3
//            )
//            planDetailDao.insert(planDetail)
        }
        return@withContext planId
    }
}