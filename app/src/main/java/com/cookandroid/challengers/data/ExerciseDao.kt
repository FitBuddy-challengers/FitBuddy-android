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

    @Query("SELECT * FROM exercises WHERE isHidden = 0 ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE isHidden = 1 ORDER BY name ASC")
    fun getHiddenExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): Exercise?

    // 특정 운동 계획에 속한 운동 목록 조회 (PlanExercise Join 테이블 사용)
    @Query("SELECT e.* FROM exercises e INNER JOIN plan_details pd ON e.id = pd.exerciseId WHERE pd.exercisePlanId = :planId ORDER BY pd.exOrder ASC")
    fun getExercisesByPlanId(planId: Long): List<Exercise>

    // 특정 부위의 운동 목록 조회
    @Query("SELECT * FROM exercises WHERE part = :part ORDER BY name ASC")
    fun getExercisesByPart(part: String): Flow<List<Exercise>>

    // 특정 장비의 운동 목록 조회
    @Query("SELECT * FROM exercises WHERE equip = :equip ORDER BY name ASC")
    fun getExercisesByEquipment(equip: String): Flow<List<Exercise>>

    // 북마크된 운동 목록 조회
    @Query("SELECT * FROM exercises WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteExercises(): Flow<List<Exercise>>

    // 북마크 상태 업데이트
    @Query("UPDATE exercises SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    // PlanDetail에 연결된 정보까지 함께 조회 (Transaction 처리 권장)
    @Transaction
    @Query("SELECT * FROM exercises WHERE id IN (SELECT exerciseId FROM plan_details WHERE exercisePlanId = :planId) ORDER BY name ASC")
    suspend fun getExercisesWithPlanInfo(planId: Long): List<ExerciseWithPlanInfo>
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