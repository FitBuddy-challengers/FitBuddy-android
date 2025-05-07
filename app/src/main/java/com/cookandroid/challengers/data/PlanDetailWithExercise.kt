package com.cookandroid.challengers.data

import androidx.room.Embedded
import androidx.room.Relation

data class PlanDetailWithExercise(
    @Embedded val planDetail: PlanDetail,
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: Exercise
)