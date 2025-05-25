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
    val id: Long = 0, // 아이디, 자동생성
    val exercisePlanId: Long, // *외래키: exercisePlan
    val exerciseId: Long, // *외래키: exercise
    //val scheduleId: Long,
    var setNumber: Int, // (세트)
    var weight: Int? = null, // 무게(아령, 케틀벨 등)
    var reps: Int, // 횟수
    // 추가
    var times: Long? = null, // 운동 시간
    var elapsedTimeMillis: Long = 0L, // 세트별 걸린 시간(밀리초): 일단 내부적으로 운동기록탭용으로만 사용.
    var isCompleted: Boolean = false, // 세트 완료 여부 (기본은 false)
    @IgnoredOnParcel
    var isHighlighted: Boolean = false // 프론트용, 현재 세트 강조용
) : Parcelable
