package com.cookandroid.challengers.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface PlanDetailDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(planDetail: PlanDetail)

    @Delete
    suspend fun delete(planDetail: PlanDetail)

    @Query("DELETE FROM plan_details")
    suspend fun deleteAll() //데이터베이스 -> 룸db

    @Update
    suspend fun update(planDetail: PlanDetail)

    // 특정 운동 계획(planId)에 연결된 모든 PlanDetail을 exOrder 오름차순으로 조회
    @Query("SELECT * FROM plan_details WHERE exercisePlanId = :planId ORDER BY exOrder ASC")
    suspend fun getPlanDetailsForPlanId(planId: Long): List<PlanDetail>

    // 특정 계획의 운동 ID 목록 조회 (순서대로)
    @Query("SELECT exerciseId FROM plan_details WHERE exercisePlanId = :planId ORDER BY exOrder ASC")
    suspend fun getExerciseIdsForPlan(planId: Long): List<Long>

    @Query("SELECT * FROM plan_details WHERE exercisePlanId = :planId AND exerciseId = :exerciseId LIMIT 1")
    fun getByPlanAndExercise(planId: Long, exerciseId: Long): PlanDetail?

    // 특정 계획과 운동 연결 해제(삭제)
    @Query("DELETE FROM plan_details WHERE exercisePlanId = :planId AND exerciseId = :exerciseId")
    suspend fun deletePlanDetail(planId: Long, exerciseId: Long)

    // 특정 운동 계획(planId)에 속한 PlanDetail 중 가장 큰 exOrder 값을 조회
    @Query("SELECT MAX(exOrder) FROM plan_details WHERE exercisePlanId = :planId")
    suspend fun getMaxOrderForPlan(planId: Long): Int?

    // 특정 운동 계획(exercisePlanId)의 특정 운동(exerciseId)에 대한 exOrder 값을 업데이트
    @Query("UPDATE plan_details SET exOrder = :order WHERE exercisePlanId = :exercisePlanId AND exerciseId = :exerciseId")
    suspend fun updatePlanDetailOrder(exercisePlanId: Long, exerciseId: Long, order: Int)

    // 특정 운동 계획(exercisePlanId)과 특정 운동(exerciseId)에 해당하는 PlanDetail 목록을 조회
    @Query("SELECT * FROM plan_details WHERE exercisePlanId = :exercisePlanId AND exerciseId = :exerciseId")
    suspend fun getPlanDetailByExercisePlanIdAndExerciseId(
        exercisePlanId: Long,
        exerciseId: Long
    ): List<PlanDetail>

    // 특정 운동 계획(exercisePlanId)에 연결된 모든 PlanDetail을 한 번만 조회
    @Query("SELECT * FROM plan_details WHERE exercisePlanId = :exercisePlanId")
    suspend fun getPlanDetailsByExercisePlanIdOnce(exercisePlanId: Long): List<PlanDetail>

    // 특정 운동 계획에 연결된 모든 plandetail을 조회
    @Query("SELECT * FROM plan_details WHERE exercisePlanId = :planId ORDER BY exOrder")
    suspend fun getPlanDetailsByPlanId(planId: Long): List<PlanDetail>

    // 운동 순서 바꾸기
    @Query("SELECT * FROM plan_details WHERE exercisePlanId = :planId AND exerciseId = :exerciseId")
    suspend fun getPlanDetail(
        planId: Long,
        exerciseId: Long
    ): PlanDetail

    // 운동 순서 업데이트
    @Query("UPDATE plan_details SET exOrder = :newOrder WHERE exercisePlanId = :planId AND exerciseId = :exerciseId")
    suspend fun updateExOrder(
        planId: Long,
        exerciseId: Long,
        newOrder: Int
    )

    // PlanDetailWithExercise 리스트를 한 번만 가져오는 함수
    @Transaction
    @Query("SELECT pd.*, e.* FROM plan_details pd INNER JOIN exercises e ON pd.exerciseId = e.id WHERE pd.exercisePlanId = :planId ORDER BY pd.exOrder ASC")
    suspend fun getPlanDetailsWithExerciseOnce(planId: Long): List<PlanDetailWithExercise>

    //주어진 planId, exerciseId 조합의 isCompleted 값을 변경
    @Query(
        """
      UPDATE plan_details
         SET isCompleted = :completed
       WHERE exercisePlanId = :planId
         AND exerciseId = :exerciseId
    """
    )
    suspend fun updateCompletion(planId: Long, exerciseId: Long, completed: Boolean)

    // 운동을 다른 운동으로 변경
    @Query("UPDATE plan_details SET exerciseId = :newExerciseId WHERE exercisePlanId = :planId AND exerciseId = :oldExerciseId")
    suspend fun replaceExercise(
        planId: Long,
        oldExerciseId: Long,
        newExerciseId: Long
    ): Int // Int 반환형으로 변경하여 업데이트된 행 수를 알 수 있도록 함


    //특정 순서의 운동 조회 함수 추가:
    @Transaction
    @Query("SELECT pd.*, e.* FROM plan_details pd INNER JOIN exercises e ON pd.exerciseId = e.id WHERE pd.exercisePlanId = :planId AND pd.exOrder = :order")
    suspend fun getPlanDetailWithExerciseByOrder(
        planId: Long,
        order: Int
    ): PlanDetailWithExercise?

    // 운동간 순서 바꾸기
    @Transaction
    suspend fun swapOrder(
        planId: Long,
        firstExId: Long,
        secondExId: Long
    ) {
        val first = getPlanDetail(planId, firstExId)
        val second = getPlanDetail(planId, secondExId)
        // 서로 값을 바꿔서 저장
        updateExOrder(planId, firstExId, second.exOrder)
        updateExOrder(planId, secondExId, first.exOrder)
    }
}
