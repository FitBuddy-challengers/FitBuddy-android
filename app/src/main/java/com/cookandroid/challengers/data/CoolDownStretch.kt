package com.cookandroid.challengers.data
import androidx.room.Entity
import androidx.room.PrimaryKey

//스트레칭의 경우, 별도로 변경될 사항이 없음. 이건 유지!
@Entity(tableName = "cooldown_stretches")
data class CoolDownStretch(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val imagePath: String? = null,
    val stOrder: Int
)
