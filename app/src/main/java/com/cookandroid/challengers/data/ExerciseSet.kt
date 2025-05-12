package com.cookandroid.challengers.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import androidx.room.Index

@Entity(
    tableName = "exercise_sets", // 각 세트의 횟수, 무게, 완료여부
    foreignKeys = [
        ForeignKey(
            entity = Exercise::class, parentColumns = ["id"],
            childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId")] // 인덱스 추가
)
@Parcelize
data class ExerciseSet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // 세트 ID (기본 키, 자동 생성)
    val exerciseId: Long, // 해당 세트가 속하는 운동 ID (외래 키)
    var setNumber: Int, // 세트 번호 (1, 2, 3...)
    var weight: Int? = null, // 무게 (nullable)
    var reps: Int, // 반복 횟수
    var isCompleted: Boolean = false,// 완료 여부
    var isHighlighted: Boolean = false // 현재 세트 강조 여부 (UI 용도, DB 저장 X)
) : Parcelable