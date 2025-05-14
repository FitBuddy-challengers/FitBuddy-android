package com.cookandroid.challengers.data
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CoolDownStretchDao {

    @Query("SELECT * FROM cooldown_stretches ORDER BY stOrder ASC")
    suspend fun getAllStretches(): List<CoolDownStretch>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(coolDownStretch: CoolDownStretch): Long

    @Query("DELETE FROM cooldown_stretches")
    suspend fun clearAll()
}
