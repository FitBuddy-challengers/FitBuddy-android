package com.cookandroid.challengers.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "plan_details",
    primaryKeys = ["exercisePlanId", "exerciseId"],
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
    ],
    indices = [Index("exercisePlanId"), Index("exerciseId")]
)

// 한 계획에 속한 각 운동의 순서 및 완료 여부
data class PlanDetail(
    val exercisePlanId: Long, // (Pk,외래키) 운동계획 ID
    val exerciseId: Long, // (PK,외래키) 운동 ID
    val exOrder: Int, // 운동의 순서
    var isCompleted: Boolean = false // 모든 세트를 완료하면 true가 됨
    // 세트, 횟수, 무게, 시간은 ExerciseSet에 저장
    )