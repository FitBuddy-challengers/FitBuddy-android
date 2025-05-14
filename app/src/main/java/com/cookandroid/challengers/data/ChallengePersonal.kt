package com.cookandroid.challengers.data

import androidx.room.Entity
import androidx.room.PrimaryKey

//챌린지 기능 데이터는 데이터베이스 테이블로 실행할 예정!!!(ROOM 에서 데이터 베이스 전환이
//일단 존재만 하고, 추후 필요없을 때 삭제하기!

@Entity(tableName = "challenge_personal")
data class ChallengePersonal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // 자동 생성되는 Primary Key
    val name: String, // 챌린지명
    val prerequisiteChallengeId: Int? = null, // 선행 챌린지 ID (없을 수 있음)
    val isAchieved: Boolean = false, // 달성 여부 (기본값: false)
    val coinReward: Int, // 코인 보상
    val targetCount: Int? = null, // 목표 개수 (횟수 기반 챌린지)
    val currentCount: Int = 0 // 현재 달성 개수 (횟수 기반 챌린지)
    // 필요한 경우 다른 유형의 챌린지를 위한 속성 추가 (예: 목표 시간, 현재 시간 등)
)