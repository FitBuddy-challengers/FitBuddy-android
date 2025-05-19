package com.cookandroid.challengers.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import androidx.room.Index
import kotlinx.parcelize.IgnoredOnParcel

@Entity(
    tableName = "exercise_sets",
    foreignKeys = [
        ForeignKey(
            entity = PlanDetail::class,
            parentColumns = ["exercisePlanId", "exerciseId"],
            childColumns = ["exercisePlanId", "exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exercisePlanId"), Index("exerciseId")]
)
@Parcelize
// 각 세트의 정보 (planDetail 기준 연결)
data class ExerciseSet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exercisePlanId: Long,
    val exerciseId: Long,
    var setNumber: Int,
    var weight: Int? = null,
    var reps: Int,
    var isCompleted: Boolean = false,
    // 세트별 수행 시간 (밀리초)
    var elapsedTimeMillis: Long = 0L,
    @IgnoredOnParcel
    var isHighlighted: Boolean = false
) : Parcelable
