package com.cookandroid.challengers.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ExercisePlanDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(exercisePlan: ExercisePlan): Long

    @Update
    suspend fun update(exercisePlan: ExercisePlan)

    @Query("DELETE FROM exercise_plans")
    suspend fun deleteAll()  // ExercisePlanDao //데이터 베이스 -> 륨 db

    @Delete
    suspend fun delete(exercisePlan: ExercisePlan)

    @Query("SELECT * FROM exercise_plans WHERE id = :id")
    suspend fun getPlanById(id: Long): ExercisePlan?

    @Query("SELECT * FROM exercise_plans ORDER BY plannedDate DESC")
    fun getAllExercisePlans(): Flow<List<ExercisePlan>>

    // 오늘 날짜 기준으로 plan이 존재하는지 확인
    @Query("SELECT * FROM exercise_plans WHERE plannedDate BETWEEN :start AND :end")
    suspend fun getPlansByDate(start: Long, end: Long): List<ExercisePlan>

    // 특정 날짜의 운동 계획 조회
    @Query("SELECT * FROM exercise_plans WHERE plannedDate BETWEEN :startTime AND :endTime ORDER BY plannedDate DESC")
    fun getExercisePlansByDate(startTime: Long, endTime: Long): Flow<List<ExercisePlan>>

    // 특정 운동이 포함된 운동 계획 조회 (PlanDetail Join 테이블 사용)
    @Query("SELECT ep.* FROM exercise_plans ep INNER JOIN plan_details pd ON ep.id = pd.exercisePlanId WHERE pd.exerciseId = :exerciseId ORDER BY ep.plannedDate DESC")
    fun getExercisePlansContainingExercise(exerciseId: Long): Flow<List<ExercisePlan>>

    // 운동 계획에 포함된 운동 목록 조회 (PlanDetail Join 테이블 사용) - 기존 방식 유지
    @Transaction
    @Query("SELECT e.*, pd.exOrder AS planOrder, pd.isCompleted AS isCompleted FROM exercises e INNER JOIN plan_details pd ON e.id = pd.exerciseId WHERE pd.exercisePlanId = :planId ORDER BY pd.exOrder ASC")
    suspend fun getExercisesInPlan(planId: Long): List<ExerciseInPlan>

    @Query("SELECT * FROM exercise_plans WHERE id = :planId")
    suspend fun getExercisePlanById(planId: Long): ExercisePlan?

    // 특정 운동 계획 삭제 시 연결된 PlanDetail도 함께 삭제 (선택 사항 - Room의 onDelete CASCADE로 처리 가능)
    @Query("DELETE FROM plan_details WHERE exercisePlanId = :planId")
    suspend fun deletePlanDetails(planId: Long)

    @Transaction
    @Query("SELECT * FROM plan_details")
    fun getAllPlanDetailsWithExerciseFlow(): Flow<List<PlanDetailWithExercise>>

    @Transaction
    @Query("SELECT * FROM plan_details WHERE exercisePlanId = :planId")
    fun getPlanDetailsWithExerciseFlow(
        planId: Long
    ): Flow<List<PlanDetailWithExercise>>

    // 오늘 날짜의 운동 루틴에 있는 운동
    @Query("SELECT * FROM exercise_plans WHERE plannedDate BETWEEN :startDate AND :endDate")
    suspend fun getExercisePlansByDateRange(startDate: Long, endDate: Long): List<ExercisePlan>

}

data class ExerciseInPlan(
    @Embedded val exercise: Exercise,
    @ColumnInfo(name = "planOrder") val order: Int,
    @ColumnInfo(name = "isCompleted") val isCompleted: Boolean
)
