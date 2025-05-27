package com.cookandroid.challengers.data

import android.icu.text.ListFormatter.Type.AND
import android.icu.text.MessagePattern.ArgType.SELECT
import androidx.room.*
import com.cookandroid.challengers.MostFrequentExercise
import com.cookandroid.challengers.MostFrequentWorkoutPart
import com.cookandroid.challengers.WorkoutTimeByPart
import kotlinx.coroutines.NonCancellable.isCompleted
import kotlinx.coroutines.flow.Flow

@Dao
interface
ExerciseSetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: ExerciseSet): Long

    @Update
    suspend fun update(exerciseSet: ExerciseSet)

    @Query("DELETE FROM exercise_sets")
    suspend fun deleteAll() //데이터베이스 -> 룸db

    @Delete
    suspend fun delete(exerciseSet: ExerciseSet)

    // 특정 운동계획에 속하고 특정 욷동에 해당하는 모든 세트 목록 조회
    @Query("SELECT * FROM exercise_sets WHERE exercisePlanId = :planId AND exerciseId = :exerciseId ORDER BY setNumber ASC")
    fun getSetsByPlanAndExerciseId(planId: Long, exerciseId: Long): List<ExerciseSet>

    // 모든 완료된(isCompleted가 1인) ExerciseSet 목록을 조회
    @Query("SELECT * FROM exercise_sets WHERE isCompleted = 1")
    suspend fun getAllSets(): List<ExerciseSet>

    // 특정 운동 ID(oldExerciseId)를 가진 모든 ExerciseSet들의 exerciseId를 새로운 운동 ID(newExerciseId)로 업데이트
    @Query("UPDATE exercise_sets SET exerciseId = :newExerciseId WHERE exerciseId = :oldExerciseId")
    suspend fun updateExerciseId(oldExerciseId: Long, newExerciseId: Long)

    // 지정된 id를 가진 exerciseSet 데이터를 삭제
    @Query("DELETE FROM exercise_sets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM exercise_sets WHERE exercisePlanId = :planId AND exerciseId = :exerciseId")
    suspend fun deleteByPlanAndExercise(planId: Long, exerciseId: Long)

    // 특정 운동 id에 해당하는 모든 세트 목록 조회
    @Query("SELECT * FROM exercise_sets WHERE exerciseId = :exerciseId ORDER BY setNumber ASC")
    suspend fun getSetsByExerciseId(exerciseId: Long): List<ExerciseSet>

    // 지정된 세트id를 가진 exerciseSet 데이터를 조회
    @Query("SELECT * FROM exercise_sets WHERE id = :setId")
    suspend fun getSetById(setId: Long): ExerciseSet?

    // 이번 달 가장 많이 한 운동
    @Query(
        """
        SELECT e.name, COUNT(pd.exerciseId) AS exerciseCount
        FROM plan_details pd
        INNER JOIN exercise_plans ep ON pd.exercisePlanId = ep.id
        INNER JOIN exercises e ON pd.exerciseId = e.id
        WHERE strftime('%Y-%m', ep.plannedDate / 1000, 'unixepoch', 'localtime') = strftime('%Y-%m', date('now', 'localtime'))
        GROUP BY e.name
        ORDER BY exerciseCount DESC
        LIMIT 1
        """
    )
    suspend fun getMostFrequentExerciseThisMonth(): MostFrequentExercise?

    // 이번 달 가장 많이 한 운동 부위
    @Query(
        """
        SELECT e.part, COUNT(pd.exerciseId) AS exerciseCount
        FROM plan_details pd
        INNER JOIN exercise_plans ep ON pd.exercisePlanId = ep.id
        INNER JOIN exercises e ON pd.exerciseId = e.id
        WHERE strftime('%Y-%m', ep.plannedDate / 1000, 'unixepoch', 'localtime') = strftime('%Y-%m', date('now', 'localtime'))
        GROUP BY e.part
        ORDER BY exerciseCount DESC
        LIMIT 1
        """
    )
    suspend fun getMostFrequentWorkoutPartThisMonth(): MostFrequentWorkoutPart?

    // 부위별 총 운동 시간 (특정 기간)
    @Query(
        """
        SELECT e.part, SUM(es.elapsedTimeMillis) AS totalTimeMillis
        FROM exercise_sets es
        INNER JOIN plan_details pd ON es.exercisePlanId = pd.exercisePlanId AND es.exerciseId = pd.exerciseId
        INNER JOIN exercise_plans ep ON pd.exercisePlanId = ep.id
        INNER JOIN exercises e ON pd.exerciseId = e.id
        WHERE ep.plannedDate BETWEEN :startDate AND :endDate
        GROUP BY e.part
        """
    )
    suspend fun getWorkoutTimeByPart(startDate: Long, endDate: Long): List<WorkoutTimeByPart>

    // 함수는 특정 날짜의 운동 계획에 포함된 각 운동에 대해, 그 운동의 모든 세트 정보를 가져오는 데 사용
    @Query("SELECT * FROM exercise_sets WHERE exercisePlanId = :planId AND exerciseId = :exerciseId")
    suspend fun getExerciseSets(planId: Long, exerciseId: Long): List<ExerciseSet>

    // setNumber의 개수 조회 = 한 운동안에 몇 세트가 들어있는지
    @Query("SELECT COUNT(DISTINCT setNumber) FROM exercise_sets WHERE exercisePlanId = :planId AND exerciseId = :exerciseId")
    suspend fun getSetCount(planId: Long, exerciseId: Long): Int

    // 특정 운동의 완료된 세트 수 조회
    @Query("SELECT COUNT(*) FROM exercise_sets WHERE exerciseId = :exerciseId AND isCompleted = 1")
    suspend fun getCompletedSetsCountForExercise(exerciseId: Long): Int

    // 특정 운동의 모든 세트 삭제
    @Query("DELETE FROM exercise_sets WHERE exerciseId = :exerciseId")
    suspend fun deleteSetsByExerciseId(exerciseId: Long)

    @Query("DELETE FROM exercise_sets WHERE exercisePlanId = :planId AND exerciseId = :exerciseId")
    suspend fun deleteSetsByPlanAndExerciseId(planId: Long, exerciseId: Long)





}