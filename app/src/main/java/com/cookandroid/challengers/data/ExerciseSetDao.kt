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
interface ExerciseSetDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(exerciseSet: ExerciseSet): Long

    @Update
    suspend fun update(exerciseSet: ExerciseSet)

    @Delete
    suspend fun delete(exerciseSet: ExerciseSet)

    @Query("SELECT * FROM exercise_sets WHERE exercisePlanId = :planId AND exerciseId = :exerciseId ORDER BY setNumber ASC")
    fun getSetsByPlanAndExerciseId(planId: Long, exerciseId: Long): List<ExerciseSet>

    @Query("SELECT * FROM exercise_sets WHERE isCompleted = 1")
    suspend fun getAllSets(): List<ExerciseSet>

    @Query("UPDATE exercise_sets SET exerciseId = :newExerciseId WHERE exerciseId = :oldExerciseId")
    suspend fun updateExerciseId(oldExerciseId: Long, newExerciseId: Long)

    @Query("DELETE FROM exercise_sets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM exercise_sets WHERE exerciseId = :exerciseId ORDER BY setNumber ASC")
    suspend fun getSetsByExerciseId(exerciseId: Long): List<ExerciseSet>

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

    // setNumber의 개수 조회
    @Query("SELECT COUNT(DISTINCT setNumber) FROM exercise_sets WHERE exercisePlanId = :planId AND exerciseId = :exerciseId")
    suspend fun getSetCount(planId: Long, exerciseId: Long): Int

    // 특정 운동의 완료된 세트 수 조회
    @Query("SELECT COUNT(*) FROM exercise_sets WHERE exerciseId = :exerciseId AND isCompleted = 1")
    suspend fun getCompletedSetsCountForExercise(exerciseId: Long): Int

    // 특정 운동의 모든 세트 삭제
    @Query("DELETE FROM exercise_sets WHERE exerciseId = :exerciseId")
    suspend fun deleteSetsByExerciseId(exerciseId: Long)

}