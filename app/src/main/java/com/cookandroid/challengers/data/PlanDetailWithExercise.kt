package com.cookandroid.challengers.data

import androidx.room.Embedded
import androidx.room.Relation

//Room 데이터베이스에서 복잡한 관계형 데이터를 한 번의 쿼리로 효율적으로 가져오기 위해 설계된 객체
//세 개의 개별 엔티티(PlanDetail, Exercise, ExerciseSet) 간의 1:1 또는 1:N 관계를 정의하며,
// 이들을 하나의 논리적인 단위로 묶어서 애플리케이션에서 사용하기 편리하게 만듭니다.
data class PlanDetailWithExercise(
    @Embedded
    val planDetail: PlanDetail,

    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: Exercise,

    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "exerciseId"
    )
    val sets: List<ExerciseSet>
)