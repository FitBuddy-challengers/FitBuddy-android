//package com.cookandroid.challengers.data
//
//import androidx.room.*
//import androidx.lifecycle.LiveData
//
//@Dao
//interface ChallengePersonalDao {
//    @Query("SELECT * FROM challenge_personal WHERE prerequisiteChallengeId IS NULL LIMIT 1")
//    fun getFirstChallenge(): LiveData<ChallengePersonal?> // LiveData로 변경
//
//    @Query("SELECT * FROM challenge_personal WHERE prerequisiteChallengeId = :completedId LIMIT 1")
//    fun getNextChallenge(completedId: Int): LiveData<ChallengePersonal?> // LiveData로 변경
//
//    @Update
//    suspend fun update(challenge: ChallengePersonal)
//
//    @Insert(onConflict = OnConflictStrategy.IGNORE)
//    suspend fun insert(challenge: ChallengePersonal)
//
//    @Delete
//    suspend fun delete(challenge: ChallengePersonal)
//
//    @Query("SELECT * FROM challenge_personal")
//    fun getAllChallenges(): LiveData<List<ChallengePersonal>> // 필요하다면 LiveData로 변경
//}