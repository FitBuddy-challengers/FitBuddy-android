package com.cookandroid.challengers.data

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "plan_details", // 운동계획에 들어가는 운동의 세트,순서를 담음
    primaryKeys = ["exercisePlanId", "exerciseId"], // Define composite primary key
    foreignKeys = [
        ForeignKey(
            entity = ExercisePlan::class,
            parentColumns = ["id"],
            childColumns = ["exercisePlanId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlanDetail(
    val exercisePlanId: Long, // (Pk,외래키) 운동계획 ID
    val exerciseId: Long, // (PK,외래기) 운동 ID
    val exOrder: Int, // 운동 순서
    val sets: Int, // 세트 수
    val reps: Int, // 1세트 당 횟수
    var isCompleted: Boolean = false
    )