package com.cookandroid.challengers.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseSetDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(exerciseSet: ExerciseSet): Long

    @Update
    suspend fun update(exerciseSet: ExerciseSet)

    @Delete
    suspend fun delete(exerciseSet: ExerciseSet)

    @Query("DELETE FROM exercise_sets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM exercise_sets WHERE exerciseId = :exerciseId ORDER BY setNumber ASC")
    suspend fun getSetsByExerciseId(exerciseId: Long): List<ExerciseSet>

    @Query("SELECT * FROM exercise_sets WHERE id = :setId")
    suspend fun getSetById(setId: Long): ExerciseSet?

    // 특정 운동의 완료된 세트 수 조회
    @Query("SELECT COUNT(*) FROM exercise_sets WHERE exerciseId = :exerciseId AND isCompleted = 1")
    suspend fun getCompletedSetsCountForExercise(exerciseId: Long): Int

    // 특정 운동의 모든 세트 삭제
    @Query("DELETE FROM exercise_sets WHERE exerciseId = :exerciseId")
    suspend fun deleteSetsByExerciseId(exerciseId: Long)

}