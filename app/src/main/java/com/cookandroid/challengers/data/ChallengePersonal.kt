//package com.cookandroid.challengers.data
//
//import androidx.room.Entity
//import androidx.room.PrimaryKey
//
//@Entity(tableName = "challenge_personal")
//data class ChallengePersonal(
//    @PrimaryKey(autoGenerate = true) val id: Int = 0, // 자동 생성되는 Primary Key
//    val name: String, // 챌린지명
//    val prerequisiteChallengeId: Int? = null, // 선행 챌린지 ID (없을 수 있음)
//    val isAchieved: Boolean = false, // 달성 여부 (기본값: false)
//    val coinReward: Int, // 코인 보상
//    val targetCount: Int? = null, // 목표 개수 (횟수 기반 챌린지)
//    val currentCount: Int = 0 // 현재 달성 개수 (횟수 기반 챌린지)
//    // 필요한 경우 다른 유형의 챌린지를 위한 속성 추가 (예: 목표 시간, 현재 시간 등)
//)