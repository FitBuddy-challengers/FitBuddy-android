package com.cookandroid.challengers.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(exercise: Exercise): Long

    @Update
    suspend fun update(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)

    // 숨겨진 상태가 아닌 운동을 조회
    @Query("SELECT * FROM exercises WHERE isHidden = 0 ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    // 숨겨진 상태인 운동을 조회
    @Query("SELECT * FROM exercises WHERE isHidden = 1 ORDER BY name ASC")
    fun getHiddenExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): Exercise?

    // 운동 이름을 가져오기 위해 필요
    @Query("SELECT name FROM exercises WHERE id = :exerciseId")
    suspend fun getExerciseNameById(exerciseId: Long): String

    // 운동의 mets(운동계수)를 가져옴
    @Query("SELECT mets FROM exercises WHERE part = :part LIMIT 1")
    suspend fun getExerciseMets(part: String): Float

    // 층간소음이 없는 운동만 가져옴
    @Query("SELECT * FROM exercises WHERE isNoise = false")
    fun getNonNoisyExercises(): Flow<List<Exercise>>
}

// Exercise와 PlanDetail 정보를 함께 담는 데이터 클래스 (필요한 정보에 따라 필드 추가)
data class ExerciseWithPlanInfo(
    @Embedded val exercise: Exercise,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId",
        entity = PlanDetail::class
    )
    val planDetails: List<PlanDetail>
)