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

    @Delete
    suspend fun delete(exercisePlan: ExercisePlan)

    @Query("SELECT * FROM exercise_plans WHERE id = :id")
    suspend fun getPlanById(id: Long): ExercisePlan?

    @Query("SELECT * FROM exercise_plans ORDER BY plannedDate DESC")
    fun getAllExercisePlans(): Flow<List<ExercisePlan>>

    // 특정 날짜의 운동 계획 조회
    @Query("SELECT * FROM exercise_plans WHERE plannedDate BETWEEN :startTime AND :endTime ORDER BY plannedDate DESC")
    fun getExercisePlansByDate(startTime: Long, endTime: Long): Flow<List<ExercisePlan>>

    // 특정 운동이 포함된 운동 계획 조회 (PlanDetail Join 테이블 사용)
    @Query("SELECT ep.* FROM exercise_plans ep INNER JOIN plan_details pd ON ep.id = pd.exercisePlanId WHERE pd.exerciseId = :exerciseId ORDER BY ep.plannedDate DESC")
    fun getExercisePlansContainingExercise(exerciseId: Long): Flow<List<ExercisePlan>>

    // 운동 계획에 포함된 운동 목록 조회 (PlanDetail Join 테이블 사용) - 기존 방식 유지
    @Transaction
    @Query("SELECT e.*, pd.exOrder AS planOrder, pd.sets AS planSets, pd.reps AS planReps FROM exercises e INNER JOIN plan_details pd ON e.id = pd.exerciseId WHERE pd.exercisePlanId = :planId ORDER BY pd.exOrder ASC")
    suspend fun getExercisesInPlan(planId: Long): List<ExerciseInPlan>

    // 특정 운동 계획과 그 계획에 속한 운동 목록 조회 (PlanDetailWithExercise 사용)
    @Transaction
    @Query("SELECT pd.*, e.* FROM plan_details pd INNER JOIN exercises e ON pd.exerciseId = e.id WHERE pd.exercisePlanId = :planId ORDER BY pd.exOrder ASC")
    suspend fun getPlanDetailsWithExercise(planId: Long): List<PlanDetailWithExercise>

    // 특정 운동 계획과 그 계획에 속한 운동 목록 조회 (직접 Join 쿼리 사용) - 기존 방식 유지
    @Transaction
    @Query("SELECT ep.*, e.*, pd.exOrder AS planOrder, pd.sets AS planSets, pd.reps AS planReps FROM exercise_plans ep INNER JOIN plan_details pd ON ep.id = pd.exercisePlanId INNER JOIN exercises e ON pd.exerciseId = e.id WHERE ep.id = :planId ORDER BY pd.exOrder ASC")
    suspend fun getExercisePlanWithExercises(planId: Long): List<ExercisePlanWithExercises>

    // 특정 운동 계획 삭제 시 연결된 PlanDetail도 함께 삭제 (선택 사항 - Room의 onDelete CASCADE로 처리 가능)
    @Query("DELETE FROM plan_details WHERE exercisePlanId = :planId")
    suspend fun deletePlanDetails(planId: Long)

    @Transaction
    @Query("SELECT * FROM plan_details")
    fun getAllPlanDetailsWithExerciseFlow(): Flow<List<PlanDetailWithExercise>>

    @Query("SELECT * FROM exercise_plans WHERE id = :planId")
    suspend fun getExercisePlanById(planId: Long): ExercisePlan?

    @Transaction
    @Query("SELECT * FROM plan_details WHERE exercisePlanId = :planId")
    fun getPlanDetailsWithExerciseFlow(
        planId: Long
    ): Flow<List<PlanDetailWithExercise>>

}

data class ExerciseInPlan(
    @Embedded val exercise: Exercise,
    @ColumnInfo(name = "planOrder") val order: Int,
    @ColumnInfo(name = "planSets") val sets: Int,
    @ColumnInfo(name = "planReps") val reps: Int
)

data class ExercisePlanWithExercises(
    @Embedded val exercisePlan: ExercisePlan,
    @Embedded(prefix = "exercise_") val exercise: Exercise?,
    @ColumnInfo(name = "planOrder") val order: Int?,
    @ColumnInfo(name = "planSets") val sets: Int?,
    @ColumnInfo(name = "planReps") val reps: Int?
)