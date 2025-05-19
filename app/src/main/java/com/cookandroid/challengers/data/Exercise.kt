package com.cookandroid.challengers.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import android.os.Parcelable // Parcelable 추가
import kotlinx.parcelize.Parcelize // Parcelize 추가

@Parcelize // Parcelize 어노테이션 추가
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // 운동 ID(PK)
    val name: String, // 운동 이름
    val part: String, // 운동 부위 (가슴, 등, 하체, 어깨, 복근, 유산소)
    val equip: String, // 운동 장비 (맨몸, 덤벨(아령), 케틀벨, 세라밴드, 스텝박스)
    val imagePath: String? = null, // 운동 이미지 경로
    val startPosition: List<String>? = null,// 시작 자세
    val exerciseMotion: List<String>? = null, // 운동 동작
    val breathing: List<String>? = null, // 호흡법
    val caution: List<String>? = null, // 주의사항
    val mets: Double, // 운동계수: 운동강도를 나타냄!
    var isFavorite: Boolean = false, // 북마크 여부
    var isHidden: Boolean = false, // 숨김 여부
    // 추가
    val isTimeType: Boolean = false,  // true: 시간, false: 횟수(기본)
    val isNoise: Boolean = true // 층간소음 여부 true: 층간소음 있음, false: 층간소음 없음(기본)
) : Parcelable // Parcelable 인터페이스 구현
