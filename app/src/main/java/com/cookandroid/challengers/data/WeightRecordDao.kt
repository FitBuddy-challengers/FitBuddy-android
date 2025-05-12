package com.cookandroid.challengers.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface WeightRecordDao {
    @Insert
    suspend fun insert(record: WeightRecord)


    @Query("SELECT * FROM weight_records WHERE date = :date")
    fun getRecordByDate(date: LocalDate): Flow<WeightRecord?> // 파라미터에는 어노테이션 불필요

    @Query("SELECT * FROM weight_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<WeightRecord>>

    @Query("SELECT * FROM weight_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getRecordsBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<WeightRecord>>
}