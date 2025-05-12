package com.cookandroid.challengers.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.cookandroid.challengers.data.converters.LocalDateConverter
import java.time.LocalDate

@Entity(tableName = "weight_records")
@TypeConverters(LocalDateConverter::class)
data class WeightRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate, // 기록 날짜
    val weight: Double, // 몸무게 (kg)
    val bodyFatPercentage: Double?, // 체지방률 (%) - nullable
    val skeletalMuscleMass: Double? // 골격근량 (kg) - nullable
)