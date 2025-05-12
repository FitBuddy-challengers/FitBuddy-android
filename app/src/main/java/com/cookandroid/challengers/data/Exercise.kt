package com.cookandroid.challengers.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import android.os.Parcelable // Parcelable 추가
import kotlinx.parcelize.Parcelize // Parcelize 추가

@Parcelize // Parcelize 어노테이션 추가
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // 운동 ID(PK, 자동 생성)
    val name: String, // 운동 이름
    val part: String, // 운동 부위 (가슴, 등, 하체, 어깨, 복근, 유산소)
    val equip: String, // 운동 장비 (맨몸, 덤벨(아령), 케틀벨, 세라밴드, 스텝박스)
    val imagePath: String? = null, // 운동 이미지 경로
    val startPosition: List<String>? = null,// 시작 자세
    val exerciseMotion: List<String>? = null, // 운동 동작
    val breathing: List<String>? = null, // 호흡법
    val caution: List<String>? = null, // 주의사항
    val mets: Double, // 운동계수
    var isFavorite: Boolean = false, // 북마크 여부
    var isHidden: Boolean = false // 숨김 여부

    // 횟수 세는 운동, 시간 세는 운동 : 플래그로?
    // 층간소음 bool : 버피테스트 등 추가..

) : Parcelable // Parcelable 인터페이스 구현
