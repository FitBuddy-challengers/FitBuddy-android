package com.cookandroid.challengers.data.db

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cookandroid.challengers.data.Exercise
import com.cookandroid.challengers.data.ExerciseDao
import com.cookandroid.challengers.data.ExercisePlan
import com.cookandroid.challengers.data.ExercisePlanDao
import com.cookandroid.challengers.data.ExerciseSet
import com.cookandroid.challengers.data.ExerciseSetDao
import com.cookandroid.challengers.data.PlanDetail
import com.cookandroid.challengers.data.PlanDetailDao

import com.cookandroid.challengers.data.CoolDownStretch
import com.cookandroid.challengers.data.CoolDownStretchDao
import com.cookandroid.challengers.data.ExerciseSetEntity
import com.cookandroid.challengers.data.ExerciseSetEntityDao
import com.cookandroid.challengers.data.WeightRecord
import com.cookandroid.challengers.data.WeightRecordDao
import com.cookandroid.challengers.data.converters.ListConverter
import com.cookandroid.challengers.data.converters.LocalDateConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Database(
    entities = [
        ExercisePlan::class, Exercise::class, ExerciseSet::class, WeightRecord::class,
        PlanDetail::class,  CoolDownStretch::class,

    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(ListConverter::class, LocalDateConverter::class)

abstract class AppDatabase : RoomDatabase() {

    abstract fun coolDownStretchDao(): CoolDownStretchDao // 쿨다운 스트레칭 목록(프론트에서만 사용)
    abstract fun exercisePlanDao(): ExercisePlanDao // 어떤 날짜의 운동 계획인지(하루에 한 계획만 생성가능)
    abstract fun exerciseDao(): ExerciseDao // 운동의 이름, 부위, 장비 등 정보를 담음(설명 등 정보는 프론트에서만)
    abstract fun exerciseSetDao(): ExerciseSetDao // 각 운동 계획별 세트 정보 (PlanDetail 외래키)
    abstract fun planDetailDao(): PlanDetailDao // 한 exercisePlan(날짜)에 담긴 운동 계획(exerciseplan, exercise 외래키)
    //abstract fun challengePersonalDao(): ChallengePersonalDao // 확정x, 개인 챌린지 정보를 담음
    abstract fun weightRecordDao(): WeightRecordDao // 확정x, 체중 기록 정보를 담음

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(
            context: Context,
            scope: LifecycleCoroutineScope
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "exercise_plan_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration() // 개발 중 스키마 변경 시 데이터 삭제 후 재생성
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    database.withTransaction {
                        populateInitialData(
                            database.exerciseDao(),
                            database.coolDownStretchDao(),

                            database.exercisePlanDao(),
                            database.planDetailDao(),
                            database.exerciseSetDao(),
                            database.weightRecordDao()
                        )
                    }
                }
            }
        }

        private suspend fun populateInitialData(
            exerciseDao: ExerciseDao,
            coolDownStretchDao: CoolDownStretchDao,

            exercisePlanDao: ExercisePlanDao,
            planDetailDao: PlanDetailDao,
            exerciseSetDao: ExerciseSetDao,
            weightRecordDao: WeightRecordDao
        ) {
//            // 개인챌린지 초기데이터
//            val challenge1 =
//                name = "스쿼트 10번 하기",
//                coinReward = 500,
//                targetCount = 10,
//                currentCount = 4
//            )
//            val challenge2 =
//                name = "런지 5회 하기",
//                coinReward = 300,
//                targetCount = 5,
//            )
//            // 스쿼트 10번 완료 후 활성화
//            val challenge3 =
//                name = "스쿼트 30번 하기",
//                prerequisiteChallengeId = 1,
//                coinReward = 700,
//                targetCount = 30
//            )

            //challengePersonalDao.insert(challenge1)
            //challengePersonalDao.insert(challenge2)
            //challengePersonalDao.insert(challenge3)

            val stretch1 =
                CoolDownStretch(name = "상체 스트레칭", imagePath = "upper_body_stretch", stOrder = 1)
            val stretch2 = CoolDownStretch(
                name = "암 써클링 어깨 스트레칭",
                imagePath = "arm_circling_shoulders",
                stOrder = 2
            )
            val stretch3 = CoolDownStretch(
                name = "라잉 햄스트링 스트레칭",
                imagePath = "lying_hamstring_stretch",
                stOrder = 3
            )

            coolDownStretchDao.insert(stretch1)
            coolDownStretchDao.insert(stretch2)
            coolDownStretchDao.insert(stretch3)

            val weightRecord1 = WeightRecord(
                date = LocalDate.of(2025, 5, 10),
                weight = 57.3,
                bodyFatPercentage = 14.5,
                skeletalMuscleMass = 23.4
            )
            val weightRecord2 = WeightRecord(
                date = LocalDate.of(2025, 5, 12),
                weight = 56.1,
                bodyFatPercentage = 14.2,
                skeletalMuscleMass = 23.6
            )
            val weightRecord3 = WeightRecord(
                date = LocalDate.of(2025, 5, 18),
                weight = 55.1,
                bodyFatPercentage = 13.2,
                skeletalMuscleMass = 24.6
            )

            weightRecordDao.insert(weightRecord1)
            weightRecordDao.insert(weightRecord2)
            weightRecordDao.insert(weightRecord3)

            // Exercise 초기 데이터 삽입
            val donkeyKickId = exerciseDao.insert( //
                Exercise(
                    id = 1,
                    name = "덩키킥",
                    part = "하체",
                    equip = "맨몸",
                    imagePath = "donkey_kick", //
                    startPosition = listOf("바닥에 손과 무릎을 대고 네발 기기 자세를 취한다."),
                    exerciseMotion = listOf(
                        "한쪽 다리를 무릎을 굽힌 채로 천천히 뒤로 밀어 올려 엉덩이를 수축한다.",
                        "정점에서 1초간 멈춘 뒤 천천히 시작 자세로 돌아온다.",
                        "반대쪽도 같은 방식으로 반복한다."
                    ),
                    breathing = listOf(" 숨을 들이쉬면서 다리를 올리고, 숨을 내쉬면서 다리를 내린다. "),
                    caution = listOf("허리가 과도하게 꺾이지 않도록 복부에 힘을 유지한다."),
                    mets = 3.5
                )
            )

            val widePushUpId = exerciseDao.insert(
                Exercise(
                    id = 2,
                    name = "와이드 푸쉬업",
                    part = "가슴, 팔",
                    equip = "맨몸",
                    imagePath = "wide_push_up",
                    startPosition = listOf("어깨보다 넓게 손을 짚고 플랭크 자세를 취한다."),
                    exerciseMotion = listOf(
                        "가슴을 바닥 쪽으로 내리며 팔꿈치를 90° 굽힌다.",
                        "가슴 근육을 수축하며 팔을 펴 원위치."
                    ),
                    breathing = listOf("내려갈 때 들이쉬고, 올라올 때 내쉰다."),
                    caution = listOf("허리가 꺾이지 않도록 복근에 힘을 준다."),
                    mets = 4.0
                )

            )

            val pullUpId = exerciseDao.insert(
                Exercise(
                    id = 4,
                    name = "풀업",
                    part = "어깨, 등, 팔",
                    equip = "맨몸",
                    imagePath = "pull_up",
                    startPosition = listOf("어깨너비보다 약간 넓게 바를 잡고 매달린다."),
                    exerciseMotion = listOf("어깨와 광배를 수축하며 턱이 바 위로 올 때까지 당긴다.", "천천히 팔을 펴며 내려간다."),
                    breathing = listOf("올라올 때 숨을 내쉬고, 내려갈 때 들이쉬기."),
                    caution = listOf("반동을 최소화하고 어깨를 내린 상태 유지."),
                    mets = 7.5
                )

            )

            val bicepCurlId = exerciseDao.insert(
                Exercise(
                    id = 5,
                    name = "덤벨 이두 컬",
                    part = "팔",
                    equip = "덤벨",
                    imagePath = "bicep_curl",
                    startPosition = listOf("양손에 덤벨을 들고 서서 손바닥이 앞을 향하게."),
                    exerciseMotion = listOf(
                        "팔꿈치를 몸에 붙이고 덤벨을 어깨 방향으로 들어 올린다.",
                        "이두를 수축한 후 천천히 시작 자세로 내린다."
                    ),
                    breathing = listOf("올릴 때 숨을 내쉬고, 내릴 때 들이쉰다."),
                    caution = listOf("팔꿈치가 앞뒤로 흔들리지 않도록 고정."),
                    mets = 6.0
                )
            )

            val dumbbellSideBendId = exerciseDao.insert(
                Exercise(
                    id = 6,
                    name = "덤벨 사이드 밴드",
                    part = "복근",
                    equip = "덤벨",
                    imagePath = "dumbbell_side_bend",
                    startPosition = listOf("한쪽 손에 덤벨을 들고 바로 선다.", "덤벨이 허벅지 옆에 오게 위치시킨다."),
                    exerciseMotion = listOf("상체를 옆으로 굽혀 덤벨을 아래로 내린다.", "시작 자세로 천천히 돌아온다."),
                    breathing = listOf("상체를 내릴 때 숨을 내쉬고, 상체를 올릴 때 숨을 들이쉰다."),
                    caution = listOf("과도한 무게는 허리에 부담을 줄 수 있으니 가벼운 무게로 진행한다. "),
                    mets = 4.0
                )
            )

            val runInPlaceId = exerciseDao.insert(
                Exercise(
                    id = 7,
                    name = "제자리 뛰기",
                    part = "유산소",
                    equip = "맨몸",
                    imagePath = "run_in_place",
                    startPosition = listOf("서서 조깅 자세를 취한다."),
                    exerciseMotion = listOf(
                        "제자리에서 무릎을 번갈아 가며 빠르게 들어 뛴다",
                        "팔을 흔들며 리듬을 유지한다.",
                        "일정한 속도로 뛰며 호흡을 고르게 한다."
                    ),
                    breathing = listOf("무릎을 들어올릴 때 숨을 들이쉬고, 내릴 때 숨을 내쉰다."),
                    caution = listOf("조깅할 때 발을 부드럽게 착지하고 무릎에 무리가 가지 않도록 주의한다."),
                    mets = 5.0,
                    isTimeType = true,
                    isNoise = true
                )
            )


            val lungeId = exerciseDao.insert( //
                Exercise(
                    id = 8,
                    name = "런지",
                    part = "하체",
                    equip = "맨몸",
                    imagePath = "lunge", //
                    startPosition = listOf("어깨너비로 서고 가슴을 편다."),
                    exerciseMotion = listOf(
                        "한쪽 발을 앞으로 내디디면서, 반대쪽 발의 뒤꿈치를 세워준다.",
                        "양쪽 무릎의 각도가 90도가 될 때까지 내려가 준다.",
                        "가슴을 펴고, 허리를 세운 상태를 유지하며 시작 자세로 돌아온다.",
                        "반대쪽 다리도 똑같이 진행한다."
                    ),
                    breathing = listOf("내려갈 때 숨을 들이쉬고, 올라갈 때 숨을 내쉰다."),
                    caution = listOf(
                        "상체가 앞으로 기울어지지 않게 주의한다.",
                        "무릎을 굽힐 때 앞으로 내디딘 발의 무릎이 발보다 앞으로 나오지 않도록 주의한다.",
                        "무게중심이 지나치게 앞으로 쏠리지 않도록 한다."
                    ),
                    mets = 4.0
                )
            )
            val dumbbellBenchPressId = exerciseDao.insert(
                Exercise(
                    id = 9,
                    name = "덤벨 벤치 프레스",
                    part = "가슴, 팔",
                    equip = "덤벨",
                    imagePath = "dumbbell_bench_press",
                    startPosition = listOf("벤치에 누워 덤벨을 가슴 위로 들어 올린다."),
                    exerciseMotion = listOf("팔꿈치를 90°까지 굽혀 덤벨을 내린 뒤 가슴 근육으로 밀어 올린다."),
                    breathing = listOf("내릴 때 들이쉬고, 올릴 때 내쉰다."),
                    caution = listOf("허리가 과도하게 뜨지 않도록 코어 고정."),
                    mets = 6.0
                )
            )
            val bandDeadliftId = exerciseDao.insert(
                Exercise(
                    id = 10,
                    name = "밴드 데드리프트",
                    part = "등, 하체",
                    equip = "세라밴드",
                    imagePath = "band_deadlift",
                    startPosition = listOf(
                        "양발로 세라밴드를 밟고 어깨너비로 선다.",
                        "양손으로 밴드 끝을 잡고 상체를 앞으로 숙인다.",
                        "등은 곧게 펴고 시선은 정면을 향한다."
                    ),
                    exerciseMotion = listOf(
                        "엉덩이와 햄스트링의 힘으로 상체를 일으키며 밴드를 당긴다.",
                        "상체를 완전히 세운 후, 천천히 시작 자세로 돌아간다.",
                        "동작 내내 허리를 곧게 유지한다."
                    ),
                    breathing = listOf(
                        "몸을 일으킬 때 숨을 내쉬고, 숙일 때 숨을 들이쉰다."
                    ),
                    caution = listOf(
                        "허리가 굽지 않도록 복부에 힘을 준다.",
                        "반동을 사용하지 말고 천천히 움직인다.",
                        "무릎을 과도하게 굽히지 않는다."
                    ),
                    mets = 5.5
                )
            )
            val bentOverLateralRaiseId = exerciseDao.insert(
                Exercise(
                    id = 11,
                    name = "벤트 오버 레터럴 레이즈",
                    part = "어깨",
                    equip = "맨몸",
                    imagePath = "bent_over_lateral_raise",
                    startPosition = listOf("상체를 45° 숙이고 양팔을 아래로 늘어뜨린다."),
                    exerciseMotion = listOf("팔꿈치를 살짝 굽힌 채 팔을 양옆으로 들어 어깨 높이까지 올린다.", "천천히 내린다."),
                    breathing = listOf("올릴 때 내쉬고, 내릴 때 들이쉬기."),
                    caution = listOf("허리가 굽지 않도록 코어 고정."),
                    mets = 4.0
                )
            )
            val hammerCurlId = exerciseDao.insert(
                Exercise(
                    id = 12,
                    name = "덤벨 해머 컬",
                    part = "팔",
                    equip = "덤벨",
                    imagePath = "hammer_curl",
                    startPosition = listOf("덤벨을 몸 옆에 들고 손바닥이 서로 마주보게."),
                    exerciseMotion = listOf("팔꿈치를 고정하고 덤벨을 어깨 쪽으로 들어 올린다.", "천천히 내려 원위치."),
                    breathing = listOf("올릴 때 내쉬고, 내릴 때 들이쉬기."),
                    caution = listOf("손목을 곧게 유지."),
                    mets = 6.0
                )
            )
            val legRaiseId = exerciseDao.insert(
                Exercise(
                    id = 13,
                    name = "레그 레이즈",
                    part = "복근",
                    equip = "맨몸",
                    imagePath = "leg_raise",
                    startPosition = listOf(
                        "등을 대고 매트에 눕고, 양다리는 곧게 편다.",
                        "팔은 몸통 옆에 두어 손바닥으로 바닥을 가볍게 누르며 안정화한다.",
                        "허리가 과도하게 뜨지 않도록 복부에 힘을 주고 골반을 중립 위치로 유지한다."
                    ),
                    exerciseMotion = listOf(
                        "복근 힘으로 양다리를 천천히 들어 올려 약 70–90°까지 올린다.",
                        "정점에서 1초간 복근을 수축하며 유지한다.",
                        "같은 속도로 다리를 천천히 내려 시작 위치 바로 위(바닥에 닿지 않을 정도)까지 내린다.",
                        "반복 횟수 또는 시간(예: 12회 × 3세트, 혹은 30초)을 설정해 반복 수행한다."
                    ),
                    breathing = listOf(
                        "다리를 올릴 때 숨을 내쉬고, 내릴 때 숨을 들이쉰다."
                    ),
                    caution = listOf(
                        "허리가 바닥에서 뜨지 않도록 복부에 힘을 지속적으로 유지한다.",
                        "다리를 내릴 때 반동을 사용하지 말고 천천히 제어한다.",
                        "목과 어깨에 힘이 과도하게 들어가지 않도록 이완한다."
                    ),
                    mets = 3.8
                )
            )
            val walkId = exerciseDao.insert(
                Exercise(
                    id = 14,
                    name = "걷기",
                    part = "유산소",
                    equip = "맨몸",
                    imagePath = "walking",
                    startPosition = listOf("등을 곧게 펴고 자연스럽게 선다."),
                    exerciseMotion = listOf("팔을 자연스럽게 흔들며 일정 속도로 걷는다."),
                    breathing = listOf("규칙적인 호흡 유지."),
                    caution = listOf("발뒤꿈치에서 발끝 순서로 착지한다."),
                    mets = 3.0,
                    isTimeType = true
                )
            )
            val squatId = exerciseDao.insert( //
                Exercise(
                    id = 15,
                    name = "스쿼트",
                    part = "하체",
                    equip = "맨몸",
                    imagePath = "squat", //
                    startPosition = listOf("양발을 어깨너비보다 약간 넓게 벌리고 선다."),
                    exerciseMotion = listOf(
                        "엉덩이를 뒤로 빼며 무릎을 굽혀 앉는다. 이때 양팔을 앞으로 벌려주면 균형을 잡기 쉽다.",
                        "발바닥으로 지면을 밀고 일어나면서 시작 자세로 돌아온다."
                    ),
                    breathing = listOf("내려갈 때 숨을 들이쉬고, 올라올 때 숨을 내쉰다."),
                    caution = listOf(
                        "과도하게 상체를 앞으로 숙이지 않는다.",
                        "허리가 꺾이지 않도록 가슴을 펴고ㅡ 코어에 힘을 준다.",
                        "엉덩이를 너무 뒤로 빼지 않는다.",
                        "일어날 때 무릎이 안으로 모이지 않도록 해준다."
                    ),
                    mets = 4.5
                )
            )
            val dipsPushUpId = exerciseDao.insert(
                Exercise(
                    id = 16,
                    name = "딥스 푸쉬업",
                    part = "팔, 가슴",
                    equip = "맨몸",
                    imagePath = "dips_pushup",
                    startPosition = listOf("손을 뒤로 짚고 다리를 펴 엉덩이를 앞에 둔다."),
                    exerciseMotion = listOf("팔꿈치를 굽혀 몸을 내렸다가 팔을 펴 밀어 올린다."),
                    breathing = listOf("내릴 때 들이쉬고, 올릴 때 내쉬기."),
                    caution = listOf("어깨가 올라가지 않도록."),
                    mets = 5.0
                )
            )
            val dumbbellShoulderPressId = exerciseDao.insert(
                Exercise(
                    id = 18,
                    name = "덤벨 숄더프레스",
                    part = "어깨, 팔",
                    equip = "덤벨",
                    imagePath = "dumbbell_shoulder_press",
                    startPosition = listOf("덤벨을 어깨 높이에서 손바닥이 앞을 향하게 든다."),
                    exerciseMotion = listOf("팔을 위로 밀어 덤벨을 머리 위로 올린다.", "천천히 어깨 높이로 내린다."),
                    breathing = listOf("밀 때 내쉬고, 내릴 때 들이쉬기."),
                    caution = listOf("허리가 과도하게 젖혀지지 않도록."),
                    mets = 6.0
                )
            )
            val dumbbellKickbackId = exerciseDao.insert(
                Exercise(
                    id = 19,
                    name = "덤벨 킥백",
                    part = "팔",
                    equip = "덤벨",
                    imagePath = "dumbbell_kickback",
                    startPosition = listOf("상체를 45° 숙이고 팔꿈치를 몸 옆에 고정."),
                    exerciseMotion = listOf("팔을 뒤로 펴 삼두를 수축한 후 천천히 굽혀 돌아온다."),
                    breathing = listOf("팔을 펴며 내쉬고, 굽히며 들이쉬기."),
                    caution = listOf("상체가 흔들리지 않도록."),
                    mets = 6.2
                )
            )
            val sitUpId = exerciseDao.insert(
                Exercise(
                    id = 20,
                    name = "윗몸 일으키기",
                    part = "복근",
                    equip = "맨몸",
                    imagePath = "sit_up",
                    startPosition = listOf("무릎을 세워 바닥에 눕는다.", "두 손은 이마 앞에 두거나, 머리를 감사 지탱한다."),
                    exerciseMotion = listOf("앉은 자세가 될 수 있게 상체를 들어 올린다.", "다시 천천히 시작 자세로 돌아온다."),
                    breathing = listOf("상체를 들어 올릴 때 숨을 내쉬고, 내릴 때 숨을 들이쉰다."),
                    caution = listOf("허리가 좋지 않은 분은 윗몸 일으키기를 피하고, 크런치로 대체해주세요."),
                    mets = 6.2
                )
            )
            val danceId = exerciseDao.insert(
                Exercise(
                    id = 21,
                    name = "댄스",
                    part = "유산소",
                    equip = "맨몸",
                    imagePath = "dance",
                    startPosition = listOf("편안한 균형 자세로 선다."),
                    exerciseMotion = listOf("음악에 맞춰 전신을 리드미컬하게 움직인다."),
                    breathing = listOf("리듬에 맞춰 호흡한다."),
                    caution = listOf("무리한 동작은 피한다."),
                    mets = 5.5,
                    isTimeType = true,
                    isNoise = true
                )
            )
            val hipExtensionId = exerciseDao.insert( //
                Exercise(
                    id = 22,
                    name = "힙 익스텐션",
                    part = "하체",
                    equip = "맨몸",
                    imagePath = "hip_extension", //
                    startPosition = listOf("엎드린 자세에서 두 손을 턱 아래에 둔다."),
                    exerciseMotion = listOf("한쪽 다리를 곧게 펴서 천천히 위로 들어 올린다.", "엉덩이를 수축한 뒤 천천히 내려온다."),
                    breathing = listOf("다리를 올릴 때 숨을 내쉬고, 내릴 때 들이쉰다."),
                    caution = listOf("허리가 꺾이지 않도록 가슴을 펴고, 코어에 힘을 준다.."),
                    mets = 3.0
                )
            )
            val dumbbellSqueezePressId = exerciseDao.insert(
                Exercise(
                    id = 23,
                    name = "덤벨 스퀴즈 프레스",
                    part = "가슴",
                    equip = "덤벨",
                    imagePath = "dumbbell_squeeze_press",
                    startPosition = listOf("벤치에 누워 덤벨 두 개를 가슴 중앙에 붙인다."),
                    exerciseMotion = listOf("덤벨을 서로 밀어내며 위로 눌러 올린다.", "천천히 내리며 덤벨끼리 닿은 상태 유지."),
                    breathing = listOf("올릴 때 내쉬고, 내릴 때 들이쉰다."),
                    caution = listOf("손목을 곧게 유지한다."),
                    mets = 6.0
                )
            )
            val dumbbellRowId = exerciseDao.insert(
                Exercise(
                    id = 24,
                    name = "덤벨 로우",
                    part = "등",
                    equip = "덤벨",
                    imagePath = "dumbbell_row",
                    startPosition = listOf(
                        "정강이에 위치에서 어깨너비로 덤벨을 잡아준다.",
                        "다리는 어깨너비만큼 벌려 준다.",
                        "엉덩이를 뒤로 살짝 빼면서 자연스럽게 무릎을 굽혀 준다."
                    ),
                    exerciseMotion = listOf(
                        "덤벨이 수직으로 올라갈 수 있게 옆구리 쪽으로 당겨준다.",
                        "날개뼈를 모으면서 자연스럽게 팔꿈치와 손을 당겨준다.",
                        " 등의 힘으로 버티며 무릎 아래까지 손을 내려준다."
                    ),
                    breathing = listOf("팔을 당길 때 숨을 내쉬고, 팔을 펼 때 숨을 들이쉰다."),
                    caution = listOf(" 팔꿈치가 벌어지지 않도록 주의한다.", "허리가 말리지 않도록 코어에 힘을 준다."),
                    mets = 3.5
                )
            )
            val lateralRaiseId = exerciseDao.insert(
                Exercise(
                    id = 25,
                    name = "레터럴 레이즈",
                    part = "어깨",
                    equip = "맨몸",
                    imagePath = "lateral_raise",
                    startPosition = listOf("양팔을 몸 옆에 두고 선다."),
                    exerciseMotion = listOf("팔꿈치를 살짝 굽혀 양팔을 어깨 높이까지 들어 올린다.", "천천히 내린다."),
                    breathing = listOf("올릴 때 내쉬고, 내릴 때 들이쉬기."),
                    caution = listOf("승모가 과도하게 긴장되지 않도록 어깨를 내린다."),
                    mets = 3.8
                )
            )
            val reverseCurlId = exerciseDao.insert(
                Exercise(
                    id = 26,
                    name = "리버스 컬",
                    part = "팔",
                    equip = "덤벨",
                    imagePath = "reverse_curl",
                    startPosition = listOf("손등이 앞을 향하게 덤벨을 잡고 선다."),
                    exerciseMotion = listOf("팔꿈치를 고정하고 덤벨을 들어 올린다.", "천천히 내려온다."),
                    breathing = listOf("올릴 때 내쉬고, 내릴 때 들이쉬기."),
                    caution = listOf("손목이 꺾이지 않도록 주의."),
                    mets = 5.8
                )
            )
            val crunchlId = exerciseDao.insert(
                Exercise(
                    id = 27,
                    name = "크런치",
                    part = "복근",
                    equip = "맨몸",
                    imagePath = "crunch",
                    startPosition = listOf(
                        "바닥에 바로 누워 무릎을 세운다.",
                        "등 윗부분과 머리가 바닥에 닿지 않도록 들어준다."
                    ),
                    exerciseMotion = listOf(
                        "허리가 굽지 않는 자세로 상체 윗부분을 들어 올린다.",
                        "시작 자세로 천천히 돌아온다."
                    ),
                    breathing = listOf(
                        "상체를 올릴 때 숨을 내쉬고, 상체를 내릴 때 숨을 들이쉰다."
                    ),
                    caution = listOf(
                        "상체를 과도하게 들어 올리면 허리에 무리가 갈 수 있으니 허리가 뜨지 않게 상체를 들어 올린다.",
                        "목이 아프다면 손으로 머리를 받쳐 목에 힘을 풀고 진행한다."
                    ),
                    mets = 3.8
                )
            )
            val hulaHoopId = exerciseDao.insert(
                Exercise(
                    id = 28,
                    name = "훌라후프",
                    part = "유산소, 복근",
                    equip = "맨몸",
                    imagePath = "hula_hoop",
                    startPosition = listOf(
                        "양발을 어깨너비로 벌리고 선다.",
                        "훌라후프를 허리 높이에 위치시킨다."
                    ),
                    exerciseMotion = listOf(
                        "허리를 좌우로 움직이며 훌라후프를 회전시킨다.",
                        "회전을 유지하며 일정 시간 동안 지속한다."
                    ),
                    breathing = listOf(
                        "자연스럽게 호흡하며 리듬을 유지한다."
                    ),
                    caution = listOf(
                        "허리에 무리가 가지 않도록 무리한 회전은 피한다.",
                        "충분한 공간을 확보하여 주변과의 충돌을 방지한다."
                    ),
                    mets = 3.5,
                    isTimeType = true
                )
            )
            val sideLegRaiseId = exerciseDao.insert( //
                Exercise(
                    id = 29,
                    name = "사이드 레그레이즈",
                    part = "하체",
                    equip = "맨몸",
                    imagePath = "side_leg_raise",
                    startPosition = listOf("옆으로 누워 아래쪽 팔은 머리를 지지한다."),
                    exerciseMotion = listOf("윗다리를 곧게 편 채 천천히 위로 들어 올린다.", "고점에서 1초 멈춘 후 천천히 내려온다."),
                    breathing = listOf("다리를 올릴 때 숨을 내쉬고, 내릴 때 들이쉰다."),
                    mets = 3.2
                )
            )
            val clapPushUpId = exerciseDao.insert(
                Exercise(
                    id = 30,
                    name = "클랩 푸쉬업",
                    part = "가슴, 팔",
                    equip = "맨몸",
                    imagePath = "clap_push_up",
                    startPosition = listOf("스탠다드 푸쉬업 자세를 취한다."),
                    exerciseMotion = listOf("폭발적으로 밀어 올려 공중에서 손뼉을 치고 착지."),
                    breathing = listOf("올라올 때 내쉬고, 착지 후 들이쉰다."),
                    caution = listOf("손목 충격 주의."),
                    mets = 6.5,
                    isNoise = true
                )
            )
            val kettlebellRowId = exerciseDao.insert(
                Exercise(
                    id = 31,
                    name = "케틀벨 로우",
                    part = "등",
                    equip = "케틀벨",
                    imagePath = "kettlebell_row",
                    startPosition = listOf(
                        "어깨너비로 서서 어깨너비로 케틀벨을 잡는다.",
                        "가슴을 펴고 등과 하체에 긴장감을 유지한다."
                    ),
                    exerciseMotion = listOf(
                        "엉덩이를 먼저 뒤로 빼면서 자연스럽게 무릎을 굽혀 내려간다.",
                        "올라올 때는 발바닥 중심에 힘을 준 상태로 무릎을 먼저 펴고 엉덩이를 앞으로 넣는다.",
                        "완전히 일어났을 때 가슴을 펴면서 신체 후면 전체를 수축한다."
                    ),
                    breathing = listOf(
                        "팔을 당길 때 숨을 내쉬고, 팔을 펼 때 숨을 들이쉰다."
                    ),
                    caution = listOf(
                        "어깨가 앞으로 굽지 않도록 가슴을 계속 편다.",
                        "허리가 말리지 않도록 코어에 힘을 준다."
                    ),
                    mets = 6.0
                )
            )
            val frontRaiseId = exerciseDao.insert(
                Exercise(
                    id = 32,
                    name = "프론트 레이즈",
                    part = "어깨",
                    equip = "맨몸",
                    imagePath = "front_raise",
                    startPosition = listOf("양팔을 허벅지 앞에 두고 선다."),
                    exerciseMotion = listOf("한 팔 또는 양팔을 앞으로 들어 어깨 높이까지 올린다.", "천천히 내린다."),
                    breathing = listOf("올릴 때 내쉬고, 내릴 때 들이쉬기."),
                    caution = listOf("어깨가 올라가지 않도록."),
                    mets = 3.8
                )
            )
            val armWalkingId = exerciseDao.insert(
                Exercise(
                    id = 33,
                    name = "암워킹",
                    part = "팔, 어깨",
                    equip = "맨몸",
                    imagePath = "arm_walking",
                    startPosition = listOf("네발 기기 자세에서 손과 발로 이동한다."),
                    exerciseMotion = listOf(
                        "한 손과 반대쪽 발을 앞으로 이동시킨다.",
                        "반대 손과 발로 같은 동작을 반복한다."
                    ),
                    breathing = listOf("리듬에 맞춰 자연스럽게 호흡한다."),
                    caution = listOf("허리가 과도하게 흔들리지 않도록 한다."),
                    mets = 3.8
                )
            )

            val russianTwistId = exerciseDao.insert(
                Exercise(
                    id = 34,
                    name = "러시안 트위스트",
                    part = "복근",
                    equip = "맨몸",
                    imagePath = "russian_twist",
                    startPosition = listOf("바닥에 앉아 무릎을 굽히고 발을 들거나 바닥에 댄다."),
                    exerciseMotion = listOf(
                        "상체를 뒤로 젖히고 양손을 모아 좌우로 몸통을 비튼다."
                    ),
                    breathing = listOf("비틀 때 숨을 내쉬고, 원위치 시 숨을 들이쉰다."),
                    caution = listOf("허리를 과도하게 젖히지 않도록 주의한다."),
                    mets = 4.0
                )
            )
            val stepUpStepBoxId = exerciseDao.insert(
                Exercise(
                    id = 35,
                    name = "스텝 업 스텝 박스",
                    part = "유산소",
                    equip = "스텝박스",
                    imagePath = "step_up_step_box",
                    startPosition = listOf("서서 박스 앞에 선다."),
                    exerciseMotion = listOf(
                        "스텝박스 앞에 서서 한 발을 박스 위에 올린다.",
                        "몸무게를 실어 박스 위로 올라간다.",
                        "다른 발을 바닥에 내리며 내려온다."
                    ),
                    breathing = listOf("발을 박스에 올릴 때 숨을 들이쉬고, 내릴 때 숨을 내쉰다."),
                    caution = listOf("스텝박스를 오를 때 무릎이 발끝을 넘지 않도록 주의한다."),
                    mets = 5.0,
                )
            )

            val legKickBackId = exerciseDao.insert(
                Exercise(
                    id = 36,
                    name = "레그 킥 백",
                    part = "하체",
                    equip = "맨몸",
                    imagePath = "leg_kick_back",
                    startPosition = listOf("네 발로 기는 자세를 취한다."),
                    exerciseMotion = listOf("한쪽 다리를 뒤로 뻗고, 천천히 시작 자세로 돌아온다."),
                    breathing = listOf("다리를 뻗을 때 숨을 내쉬고, 돌아올 때 들이쉰다."),
                    caution = listOf("허리가 꺾이지 않도록 주의한다."),
                    mets = 3.5
                )
            )

            val declinePushUpId = exerciseDao.insert(
                Exercise(
                    id = 37,
                    name = "디클라인 푸쉬업",
                    part = "가슴, 어깨",
                    equip = "스텝박스",
                    imagePath = "decline_push_up",
                    startPosition = listOf("발을 스텝박스 위에 올리고 플랭크."),
                    exerciseMotion = listOf("가슴을 바닥으로 내렸다가 밀어 올린다."),
                    breathing = listOf("내릴 때 들이쉬고, 올릴 때 내쉰다."),
                    caution = listOf("목이 꺾이지 않도록."),
                    mets = 4.5
                )
            )

            val backExtensionId = exerciseDao.insert(
                Exercise(
                    id = 38,
                    name = "백 익스텐션",
                    part = "등",
                    equip = "맨몸",
                    imagePath = "back_extension",
                    startPosition = listOf("엎드려 누워 손은 머리 뒤에 둔다."),
                    exerciseMotion = listOf("상체를 들어 올리고 천천히 내려온다."),
                    breathing = listOf("들어 올릴 때 숨을 내쉬고, 내려올 때 들이쉰다."),
                    caution = listOf("허리에 무리 가지 않도록 한다."),
                    mets = 4.0
                )
            )

            val sideLateralRaiseId = exerciseDao.insert(
                Exercise(
                    id = 39,
                    name = "사이드 레터럴 레이즈",
                    part = "어깨",
                    equip = "맨몸",
                    imagePath = "side_lateral_raise",
                    startPosition = listOf("양팔을 몸 옆에 둔다."),
                    exerciseMotion = listOf("팔을 옆으로 들어 올린다.", "천천히 내린다."),
                    breathing = listOf("올릴 때 내쉬고, 내릴 때 들이쉰다."),
                    caution = listOf("어깨 긴장하지 않도록 한다."),
                    mets = 3.8
                )
            )
            val joggingId = exerciseDao.insert(
                Exercise(
                    id = 42,
                    name = "조깅",
                    part = "유산소",
                    equip = "맨몸",
                    imagePath = "jogging",
                    startPosition = listOf("편안한 자세로 서서 준비한다."),
                    exerciseMotion = listOf("천천히 제자리에서 뛰며 심박수를 올린다."),
                    breathing = listOf("호흡을 고르게 유지한다."),
                    caution = listOf("무릎과 발목 부상을 주의한다."),
                    mets = 5.5,
                    isTimeType = true
                )
            )

            val basicStepperId = exerciseDao.insert(
                Exercise(
                    id = 43,
                    name = "기본 스텝퍼",
                    part = "하체",
                    equip = "스텝퍼",
                    imagePath = "basic_stepper",
                    startPosition = listOf("스텝퍼 위에 올라서서 균형을 잡는다."),
                    exerciseMotion = listOf(
                        "발을 번갈아 올리고 내리며 스텝퍼 동작을 반복한다."
                    ),
                    breathing = listOf("리듬에 맞춰 규칙적인 호흡을 유지한다."),
                    caution = listOf("발목에 무리가 가지 않도록 주의한다."),
                    mets = 4.5
                )
            )
            val inclinePushUpId = exerciseDao.insert(
                Exercise(
                    id = 44,
                    name = "인클라인 푸쉬업",
                    part = "가슴, 팔",
                    equip = "스텝박스",
                    imagePath = "incline_push_up",
                    startPosition = listOf("손을 박스 위에 짚고 플랭크."),
                    exerciseMotion = listOf("가슴을 박스 쪽으로 내렸다가 밀어 올린다."),
                    breathing = listOf("내릴 때 들이쉬고, 올릴 때 내쉰다."),
                    caution = listOf("코어 유지."),
                    mets = 3.8
                )
            )
            val reversePushUpId = exerciseDao.insert(
                Exercise(
                    id = 45,
                    name = "리버스 푸쉬업",
                    part = "등",
                    equip = "맨몸",
                    imagePath = "reverse_push_up",
                    startPosition = listOf("의자나 평평한 곳에 손을 대고 엉덩이를 들어 올린다."),
                    exerciseMotion = listOf("팔꿈치를 굽혀 몸을 내리고 다시 올린다."),
                    breathing = listOf("내릴 때 들이쉬고, 올릴 때 내쉰다."),
                    caution = listOf("어깨가 올라가지 않도록."),
                    mets = 4.5
                )
            )
            val diamondPushUpId = exerciseDao.insert(
                Exercise(
                    id = 47,
                    name = "다이아몬트 푸쉬업",
                    part = "팔, 가슴",
                    equip = "맨몸",
                    imagePath = "diamond_push_up",
                    startPosition = listOf("손가락을 모아 다이아몬드 모양을 만들고 플랭크 자세."),
                    exerciseMotion = listOf("가슴을 손쪽으로 내렸다가 팔을 펴며 올라온다."),
                    breathing = listOf("내릴 때 들이쉬고, 올릴 때 내쉬기."),
                    caution = listOf("팔꿈치가 몸에 붙도록."),
                    mets = 5.2
                )
            )
            val sidePlankId = exerciseDao.insert(
                Exercise(
                    id = 48,
                    name = "사이드 플랭크",
                    part = "복근",
                    equip = "맨몸",
                    imagePath = "side_plank",
                    startPosition = listOf(
                        "다리를 쭉 편 채 옆으로 누운다.",
                        "바닥에 있는 쪽 팔로 바닥을 짚는다.",
                        "반대쪽 손은 편하게 다리 위에 올린다."
                    ),
                    exerciseMotion = listOf(
                        "옆으로 누워 팔꿈치를 어깨 밑에 위치시킨다.",
                        "두 다리를 붙이고 머리부터 다리까지 일직선이 되도록 복부와 엉덩이에 힘을 준다.",
                        "주어진 시간 동안 복부에 긴장을 놓치지 않고 버텨주며 반대쪽도 똑같이 진행한다."
                    ),
                    breathing = listOf("숨을 들이마시고 내쉬며 몸을 들어 올린다."),
                    caution = listOf("옆으로 플랭크를 할 때 허리가 휘지 않도록 몸이 일직선을 유지한다."),
                    mets = 4.0,
                )
            )
            val jumpRopeId = exerciseDao.insert(
                Exercise(
                    id = 49,
                    name = "줄넘기",
                    part = "유산소",
                    equip = "맨몸",
                    imagePath = "jump_rope",
                    startPosition = listOf("줄을 뒤로 두고 손잡이를 잡아 선다."),
                    exerciseMotion = listOf("손목을 돌려 줄을 넘기며 가볍게 점프한다."),
                    breathing = listOf("점프 시 내쉬고 착지하며 들이쉰다."),
                    caution = listOf("무릎에 무리 가지 않도록 낮은 점프."),
                    mets = 8.0,
                    isTimeType = true
                )
            )
            val cycleId = exerciseDao.insert(
                Exercise(
                    id = 50,
                    name = "싸이클",
                    part = "유산소",
                    equip = "맨몸",
                    imagePath = "cycle",
                    startPosition = listOf("사이클 머신에 앉아 페달에 발을 고정한다."),
                    exerciseMotion = listOf(
                        "페달을 부드럽게 밟아 일정 속도로 회전시킨다.",
                        "속도와 저항을 조절하며 목표 시간 동안 지속한다."
                    ),
                    breathing = listOf("규칙적으로 호흡한다."),
                    caution = listOf("무릎이 과도하게 굽혀지지 않도록 안장 높이를 조정한다."),
                    mets = 7.0
                )
            )
            val archerPushUpId = exerciseDao.insert(
                Exercise(
                    id = 51,
                    name = "아처 푸쉬업",
                    part = "가슴, 팔",
                    equip = "맨몸",
                    imagePath = "archer_push_up", //
                    startPosition = listOf("손을 넓게 벌려 플랭크."),
                    exerciseMotion = listOf("한 팔을 굽혀 가슴을 내리며 반대팔은 곧게 편다.", "밀어 올리며 반대쪽 반복."),
                    breathing = listOf("내릴 때 들이쉬고, 올릴 때 내쉰다."),
                    mets = 5.5
                )
            )
            val shoulderTapId = exerciseDao.insert(
                Exercise(
                    id = 53,
                    name = "숄더 탭",
                    part = "어깨, 복근",
                    equip = "맨몸",
                    imagePath = "shoulder_tap",
                    startPosition = listOf("푸쉬업 자세에서 몸을 곧게 유지."),
                    exerciseMotion = listOf("한 손으로 반대쪽 어깨를 가볍게 터치 후 교대."),
                    breathing = listOf("터치할 때 내쉬고, 손을 내릴 때 들이쉬기."),
                    caution = listOf("골반이 좌우로 흔들리지 않도록."),
                    mets = 4.0
                )
            )
            val closeGripPushUpId = exerciseDao.insert(
                Exercise(
                    id = 54,
                    name = "클로즈 그립 푸쉬업",
                    part = "팔, 가슴",
                    equip = "맨몸",
                    imagePath = "close_grip_push_up",
                    startPosition = listOf("손을 어깨너비보다 좁게 짚는다."),
                    exerciseMotion = listOf("가슴을 내렸다가 팔을 펴며 올린다."),
                    breathing = listOf("내릴 때 들이쉬고, 올릴 때 내쉬기."),
                    caution = listOf("코어 유지."),
                    mets = 4.8
                )
            )
            val flutterKickId = exerciseDao.insert(
                Exercise(
                    id = 55,
                    name = "플로터킥",
                    part = "복근",
                    equip = "맨몸",
                    imagePath = "flutter_kick",
                    startPosition = listOf("누워서 다리를 곧게 펴고 양손은 엉덩이 밑에 둔다."),
                    exerciseMotion = listOf("다리를 빠르게 위아래로 움직인다."),
                    breathing = listOf("자연스러운 호흡 유지."),
                    caution = listOf("허리가 뜨지 않도록 복근에 힘을 준다."),
                    mets = 3.5
                )
            )
            val jumpingJackId = exerciseDao.insert(
                Exercise(
                    id = 56,
                    name = "점핑 잭",
                    part = "유산소",
                    equip = "맨몸",
                    imagePath = "jumping_jack",
                    startPosition = listOf("발을 모으고 팔을 몸 옆에 둔다."),
                    exerciseMotion = listOf("점프하며 발을 벌리고 팔을 머리 위로 올린다.", "다시 점프해 원위치."),
                    breathing = listOf("점프 시 내쉬고, 착지 시 들이쉰다."),
                    caution = listOf(" 부드럽게 착지한다."),
                    mets = 5.5,
                    isNoise = true,
                    isTimeType = true
                )
            )
            val gluteBridgeId = exerciseDao.insert( //
                Exercise(
                    id = 58,
                    name = "글루트 브릿지",
                    part = "하체",
                    equip = "맨몸",
                    imagePath = "glute_bridge", //
                    startPosition = listOf("바닥에 누워 무릎을 구부린 상태에서. 양 발바닥면 전체가 바닥에 닿게 한다.."),
                    exerciseMotion = listOf(
                        "엉덩이를 들어 올려 무릎-엉덩이-어깨가 일직선이 되게 한다.",
                        "정점에서 약 2초 정지 후 천천히 내려온다."
                    ),
                    breathing = listOf("올라갈 때 숨을 내쉬고, 내려올 때 들이쉰다."),
                    caution = listOf("허리가 과도히 꺾이지 않도록 복부에 힘을 준다."),
                    mets = 3.5
                )
            )
            val oneArmPushUpId = exerciseDao.insert(
                Exercise(
                    id = 59,
                    name = "원암 푸쉬업",
                    part = "가슴, 팔",
                    equip = "맨몸",
                    imagePath = "one_arm_push_up", //
                    startPosition = listOf("발을 넓게 벌리고 한 손은 등 뒤에 둔다."),
                    exerciseMotion = listOf("한 팔로 몸을 지탱해 내려갔다 올라온다."),
                    breathing = listOf("내릴 때 들이쉬고, 올릴 때 내쉰다."),
                    caution = listOf("어깨 안정성 확보."),
                    mets = 7.0
                )
            )
            val yRaiseId = exerciseDao.insert(
                Exercise(
                    id = 60,
                    name = "Y 레이즈",
                    part = "어깨, 등",
                    equip = "맨몸",
                    imagePath = "y_raise",
                    startPosition = listOf("상체를 45° 숙이고 팔을 아래로 내린다."),
                    exerciseMotion = listOf("팔을 ‘Y’자 형태로 위로 들어 등-어깨를 수축.", "천천히 내린다."),
                    breathing = listOf("올릴 때 내쉬고, 내릴 때 들이쉬기."),
                    caution = listOf("승모를 최소화하고 어깨 후면에 집중."),
                    mets = 4.2
                )
            )
            val seatedKneeUpId = exerciseDao.insert(
                Exercise(
                    id = 62,
                    name = "시티드 니업",
                    part = "복근",
                    equip = "맨몸",
                    imagePath = "seated_knee_up",
                    startPosition = listOf(
                        "벤치의 끝에 앉아 다리를 앞으로 뻗는다.",
                        "상체는 자연스럽게 뒤로 보내고 두 손으로 벤치를 잡아 몸이 흔들리지 않게 한다."
                    ),
                    exerciseMotion = listOf(
                        "무릎과 가슴이 가까워지도록 하체를 들어 올린다.",
                        "상체는 자연스럽게 앞으로 보내 코어가 수축하도록 한다.",
                        "다시 천천히 시작 자세로 돌아온다."
                    ),
                    breathing = listOf("다리를 올릴 때 숨을 내쉬고 다리를 내릴 때 숨을 들이쉰다."),
                    caution = listOf("너무 어렵다면 무릎을 굽힌 채 진행한다."),
                    mets = 4.5,
                )
            )

            val slowBurpeeId = exerciseDao.insert(
                Exercise(
                    id = 63,
                    name = "슬로우 버피",
                    part = "유산소, 복근, 하체",
                    equip = "맨몸",
                    imagePath = "slow_burpee",
                    startPosition = listOf("선 자세에서 시작."),
                    exerciseMotion = listOf("스쿼트 → 플랭크 → 푸쉬업 → 스쿼트 → 점프를 천천히 수행."),
                    breathing = listOf("동작 전환마다 자연 호흡."),
                    caution = listOf("무릎과 허리에 과부하 주의."),
                    mets = 6.0,
                    isNoise = true,
                )
            )
            val reverseLungeId = exerciseDao.insert(
                Exercise(
                    id = 64,
                    name = "리버스 런지",
                    part = "하체",
                    equip = "맨몸",
                    imagePath = "reverse_lunge",
                    startPosition = listOf("어깨너비로 서고 가슴을 편다."),
                    exerciseMotion = listOf(
                        "한쪽 다리를 뒤로 크게 내딛으며 무릎을 90도로 굽힌다.",
                        "앞다리로 밀어 시작 자세로 돌아온다.",
                        "다리 교체하여 반복."
                    ),
                    breathing = listOf("내려갈 때 숨 들이쉬고, 올라올 때 내쉰다."),
                    caution = listOf("무릎과 발끝이 동일한 방향을 유지한다."),
                    mets = 4.0
                )
            )
            val frontSquatId = exerciseDao.insert(
                Exercise(
                    id = 65,
                    name = "프론트 스쿼트",
                    part = "하체",
                    equip = "맨몸",
                    imagePath = "front_squat",
                    startPosition = listOf("발을 어깨너비로 벌리고 선다."),
                    exerciseMotion = listOf("상체를 곧게 유지하며 무릎을 굽혀 내려간다.", "허벅지가 지면과 평행하면 올라온다."),
                    breathing = listOf("내려갈 때 들이쉬고, 올라올 때 내쉰다."),
                    caution = listOf("상체가 앞으로 과도하게 기울지 않도록 한다."),
                    mets = 4.8
                )
            )
            val bandFrontRaiseId = exerciseDao.insert(
                Exercise(
                    id = 67,
                    name = "밴드 프론트 레이즈",
                    part = "어깨",
                    equip = "세라밴드",
                    imagePath = "band_front_raise",
                    startPosition = listOf("밴드 중앙을 발로 밟고 양손으로 잡는다."),
                    exerciseMotion = listOf("팔을 앞으로 들어 어깨 높이까지 올린다.", "저항을 유지하며 내려온다."),
                    breathing = listOf("올릴 때 내쉬고, 내릴 때 들이쉬기."),
                    caution = listOf("팔꿈치를 약간 굽혀 관절 보호."),
                    mets = 4.0
                )
            )
            val gripTrainerId = exerciseDao.insert(
                Exercise(
                    id = 68,
                    name = "악력기",
                    part = "팔",
                    equip = "맨몸",
                    imagePath = "grip_trainer",
                    startPosition = listOf("악력기를 한손에 잡는다."),
                    exerciseMotion = listOf("손가락으로 악력기를 완전히 쥐어 닫았다가 서서히 연다."),
                    breathing = listOf("쥘 때 내쉬고, 펼 때 들이쉬기."),
                    caution = listOf("손목에 무리 가지 않도록."),
                    mets = 3.0
                )
            )
            val hollowHoldId = exerciseDao.insert(
                Exercise(
                    id = 69,
                    name = "할로우 홀드",
                    part = "복근",
                    equip = "맨몸",
                    imagePath = "hollow",
                    startPosition = listOf(
                        "바닥을 보고 누운다.",
                        "두 손을 쭉 뻗어 머리 위로 올린다.",
                        "두 발을 쭉 뻗어 발끝을 모은다."
                    ),
                    exerciseMotion = listOf(
                        "발을 띄운 채로 상체를 들어 올린다.",
                        "코어에 힘을 준 채 버틴다."
                    ),
                    breathing = emptyList(),
                    caution = listOf(
                        "버티는 동작에서 엉덩이만 바닥에 닿아 있어야 한다."
                    ),
                    mets = 4.5,
                )
            )

            val sideLungeId = exerciseDao.insert(
                Exercise(
                    id = 71,
                    name = "사이드 런지",
                    part = "하체",
                    equip = "맨몸",
                    imagePath = "side_lunge",  //
                    startPosition = listOf("양발을 넓게 벌리고 선다."),
                    exerciseMotion = listOf(
                        "한쪽 무릎을 굽혀 엉덩이를 뒤로 밀며 몸을 내린다.",
                        "다른 쪽 다리는 곧게 편다.",
                        "시작 자세로 복귀 후 반대쪽."
                    ),
                    breathing = listOf("내려갈 때 들이쉬고, 올라올 때 내쉰다."),
                    caution = listOf("허리가 굽지 않도록 가슴을 편다."),
                    mets = 4.0
                )
            )
            val kettlebellSwingId = exerciseDao.insert(
                Exercise(
                    id = 72,
                    name = "케틀벨 스윙",
                    part = "유산소, 하체, 팔",
                    equip = "케틀벨",
                    imagePath = "kettlebell_swing",
                    startPosition = listOf("케틀벨을 다리 사이에 두고 선다."),
                    exerciseMotion = listOf("골반 힘으로 케틀벨을 가슴 높이까지 스윙한다."),
                    breathing = listOf("스윙 위로 갈 때 내쉬고, 아래로 갈 때 들이쉰다."),
                    caution = listOf("허리 굽힘 주의."),
                    mets = 6.8
                )
            )
            val backSquatId = exerciseDao.insert(
                Exercise(
                    id = 73,
                    name = "백 스쿼트",
                    part = "하체",
                    equip = "맨몸",
                    imagePath = "back_squat",
                    startPosition = listOf("발을 어깨너비로 벌리고 선다."),
                    exerciseMotion = listOf("엉덩이를 뒤로 빼며 무릎을 굽혀 앉는다.", "허벅지가 평행될 때까지 내려갔다가 올라온다."),
                    breathing = listOf("내려갈 때 들이쉬고, 올라올 때 내쉰다."),
                    caution = listOf("무릎이 안쪽으로 모이지 않도록 한다."),
                    mets = 5.0
                )
            )
            val bandLateralRaiseId = exerciseDao.insert(
                Exercise(
                    id = 74,
                    name = "밴드 레터럴 레이즈",
                    part = "어깨",
                    equip = "세라밴드",
                    imagePath = "band_lateral_raise",
                    startPosition = listOf("밴드 중앙을 발로 밟고 양손을 몸 옆에 둔다."),
                    exerciseMotion = listOf("팔을 옆으로 들어 올려 어깨 높이에서 1초 유지 후 내린다."),
                    breathing = listOf("올릴 때 내쉬고, 내릴 때 들이쉬기."),
                    caution = listOf("목 부상 방지를 위해 승모 긴장 최소화."),
                    mets = 4.0
                )
            )

            val overheadLungeId = exerciseDao.insert(
                Exercise(
                    id = 78,
                    name = "오버헤드 런지",
                    part = "하체, 어깨",
                    equip = "맨몸",
                    imagePath = "overhead_lunge",
                    startPosition = listOf("두 팔을 머리 위로 곧게 들어 올린다."),
                    exerciseMotion = listOf("앞으로 런지를 하며 팔을 곧게 유지.", "앞발로 밀어 올라오며 원위치."),
                    breathing = listOf("내려갈 때 들이쉬고, 올라올 때 내쉰다."),
                    caution = listOf("팔이 흔들리지 않도록 어깨를 고정한다."),
                    mets = 4.5
                )
            )
            val standingChestPressId = exerciseDao.insert(
                Exercise(
                    id = 79,
                    name = "밴드 스탠딩 체스트 프레스",
                    part = "가슴",
                    equip = "세라밴드",
                    imagePath = "standing_chest_press", //
                    startPosition = listOf("밴드를 등 뒤 기둥에 고정 후 손잡이를 잡는다."),
                    exerciseMotion = listOf("팔꿈치를 90°로 굽히고 전방으로 밀어 가슴을 수축.", "천천히 다시 굽혀 원위치."),
                    breathing = listOf("밀 때 내쉬고, 돌아올 때 들이쉰다."),
                    caution = listOf("어깨가 들리지 않도록 한다."),
                    mets = 4.2
                )
            )
            val bandSeatedRowId = exerciseDao.insert(
                Exercise(
                    id = 80,
                    name = "밴드 시티드 로우",
                    part = "등",
                    equip = "밴드",
                    imagePath = "band_seated_row",
                    startPosition = listOf(
                        "앉은 상태에서 밴드를 잡아 한쪽 발에 걸어준다.",
                        "허리를 세우고 가슴을 편다."
                    ),
                    exerciseMotion = listOf(
                        "팔꿈치가 옆구리를 스쳐 지나가게 당긴다.",
                        "등에 힘을 유지한 상태에서 시작 자세로 돌아간다."
                    ),
                    breathing = listOf("팔을 당길 때 숨을 내쉬고, 팔을 펼 때 숨을 들이쉰다."),
                    caution = listOf(
                        "어깨가 앞으로 굽지 않도록 가슴을 계속 편다.",
                        "허리가 말리지 않도록 코어에 힘을 준다."
                    ),
                    mets = 4.5,
                )
            )
            val facePullId = exerciseDao.insert(
                Exercise(
                    id = 81,
                    name = "밴드 페이스 풀",
                    part = "어깨, 등",
                    equip = "세라밴드",
                    imagePath = "face_pull",
                    startPosition = listOf("밴드를 얼굴 높이에 고정, 손잡이를 잡는다."),
                    exerciseMotion = listOf(
                        "팔꿈치를 옆으로 벌려 밴드를 얼굴 쪽으로 당긴다.",
                        "견갑골을 모으며 1초 정지 후 천천히 원위치."
                    ),
                    breathing = listOf("당길 때 내쉬고, 원위치하며 들이쉬기."),
                    caution = listOf("허리가 과도히 젖혀지지 않도록."),
                    mets = 4.5
                )
            )
            val dumbbellLungeId = exerciseDao.insert(
                Exercise(
                    id = 82,
                    name = "덤벨 런지",
                    part = "하체",
                    equip = "덤벨",
                    imagePath = "dumbbell_lunge",
                    startPosition = listOf(
                        "덤벨을 양손에 잡고 다리는 골반 너비로 벌린다.",
                        "가슴을 펴고 허리를 세운다."
                    ),
                    exerciseMotion = listOf(
                        "한쪽 발을 앞으로 내디디면서 반대쪽 발의 뒤꿈치를 세운다.",
                        "양쪽 무릎의 각도가 90도가 될 때까지 내려간다.",
                        "가슴을 펴고 허리를 세운 상태를 유지하며 시작 자세로 돌아간다.",
                        "반대쪽 다리도 똑같이 진행한다."
                    ),
                    breathing = listOf("내려갈 때 숨을 들이쉬고 올라갈 때 숨을 내쉰다."),
                    caution = listOf(
                        "상체가 앞으로 기울어지지 않도록 주의한다.",
                        "무릎을 굽힐 때 앞으로 내디딘 발의 무릎이 발보다 앞으로 나오지 않도록 주의한다.",
                        "무게중심이 지나치게 앞으로 쏠리지 않도록 한다."
                    ),
                    mets = 5.5,
                )
            )
            val plankId = exerciseDao.insert(
                Exercise(
                    id = 83,
                    name = "플랭크",
                    part = "복근",
                    equip = "맨몸",
                    imagePath = "plank",
                    startPosition = listOf(
                        "양팔을 어깨너비로 벌리고 무릎은 편 상태로 엎드린다.",
                        "몸이 일직선이 되도록 엉덩이와 코어에 힘을 준다."
                    ),
                    exerciseMotion = listOf(
                        "양팔을 어깨너비로 벌리고 무릎을 편 상태로 발 앞꿈치로 몸을 지지하며 엎드린다.",
                        "머리부터 발까지 일직선이 되도록 엉덩이를 올린다.",
                        "허리가 아래로 내려가지 않도록 배꼽을 위로 당기며 동작을 실시한다."
                    ),
                    breathing = listOf("숨을 깊게 들이마시고 천천히 내쉰다."),
                    caution = listOf("등이 굽지 않도록 몸이 일직선을 유지하며 복부에 힘을 주고 버틴다."),
                    mets = 4.0,
                )
            )
            val ptDrillId = exerciseDao.insert(
                Exercise(
                    id = 84,
                    name = "PT체조",
                    part = "유산소",
                    equip = "맨몸",
                    imagePath = "pt_drill",
                    startPosition = listOf("정렬자세로 선다."),
                    exerciseMotion = listOf("팔벌려뛰기·런지·스쿼트 등 체조 동작을 구령에 맞춰 연속 수행."),
                    breathing = listOf("구령에 맞춰 호흡."),
                    caution = listOf("반동 과다 사용 주의."),
                    mets = 6.5,
                    isNoise = true,
                    isTimeType = true
                )
            )
            val chestFlyId = exerciseDao.insert(
                Exercise(
                    id = 85,
                    name = "밴드 체스트 플라이",
                    part = "가슴",
                    equip = "세라밴드",
                    imagePath = "chest_fly", //
                    startPosition = listOf("밴드 양끝을 잡고 팔을 앞에 모은다."),
                    exerciseMotion = listOf("팔을 양옆으로 벌려 가슴을 늘린 뒤 모으며 수축."),
                    breathing = listOf("모을 때 내쉬고, 벌릴 때 들이쉰다."),
                    caution = listOf("팔꿈치를 너무 펴지 않는다."),
                    mets = 4.0
                )
            )
            val dumbbellOverheadPressId = exerciseDao.insert(
                Exercise(
                    id = 87,
                    name = "덤벨 오버헤드 프레스",
                    part = "어깨, 팔",
                    equip = "덤벨",
                    imagePath = "dumbbell_overhead_press",
                    startPosition = listOf("덤벨을 어깨 높이에서 잡는다."),
                    exerciseMotion = listOf("팔을 위로 밀어 덤벨을 머리 위로 올린다.", "귀 옆으로 덤벨이 지나가도록 내린다."),
                    breathing = listOf("올릴 때 내쉬고, 내릴 때 들이쉬기."),
                    caution = listOf("허리 과신전 주의."),
                    mets = 6.0
                )
            )
            val gobletSquatIdArm = exerciseDao.insert(
                Exercise(
                    id = 88,
                    name = "덤벨 고블릿 스쿼트",
                    part = "팔, 하체",
                    equip = "덤벨",
                    imagePath = "goblet_squat",
                    startPosition = listOf("가슴 앞에 덤벨을 세로로 잡고 선다."),
                    exerciseMotion = listOf("엉덩이를 뒤로 빼며 스쿼트, 하부에서 팔로 덤벨을 지탱.", "발로 밀어 올라온다."),
                    breathing = listOf("내릴 때 들이쉬고, 올라올 때 내쉬기."),
                    caution = listOf("팔꿈치가 무릎 안쪽을 따라 내려가도록한다."),
                    mets = 4.6
                )
            )
            val bandPushUpId = exerciseDao.insert(
                Exercise(
                    id = 92,
                    name = "밴드 푸쉬업",
                    part = "가슴, 팔",
                    equip = "세라밴드",
                    imagePath = "band_push_up", //
                    startPosition = listOf("세라밴드를 등 뒤에 걸고 푸쉬업 자세."),
                    exerciseMotion = listOf("밴드 저항을 이기며 팔을 굽혀 내려갔다 밀어 올린다."),
                    breathing = listOf("내릴 때 들이쉬고, 올릴 때 내쉰다."),
                    mets = 5.2
                )
            )
            val bandBentOverRowId = exerciseDao.insert(
                Exercise(
                    id = 93,
                    name = "밴드 벤트 오버 로우",
                    part = "팔, 등",
                    equip = "세라밴드",
                    imagePath = "bent_over_row_band",
                    startPosition = listOf("밴드를 밟고 상체를 45° 숙인다."),
                    exerciseMotion = listOf("팔꿈치를 뒤로 당겨 밴드를 끌어당겼다가 천천히 펴 원위치."),
                    breathing = listOf("당길 때 내쉬고, 펴며 들이쉬기."),
                    caution = listOf("허리 중립 유지."),
                    mets = 5.5
                )
            )
            val kettlebellShoulderPressId = exerciseDao.insert(
                Exercise(
                    id = 94,
                    name = "케틀벨 숄더 프레스",
                    part = "어깨, 팔",
                    equip = "케틀벨",
                    imagePath = "kettlebell_shoulder_press",
                    startPosition = listOf("케틀벨을 랙 포지션으로 어깨에 둔다."),
                    exerciseMotion = listOf("팔을 곧게 펴 케틀벨을 머리 위로 올린다.", "천천히 내려 랙 포지션."),
                    breathing = listOf("밀 때 내쉬고, 내릴 때 들이쉬기."),
                    caution = listOf("손목이 꺾이지 않도록."),
                    mets = 6.2
                )
            )
            val bandRussianTwistId = exerciseDao.insert(
                Exercise(
                    id = 96,
                    name = "밴드 러시안 트위스트",
                    part = "복근",
                    equip = "밴드",
                    imagePath = "band_russian_twist",
                    startPosition = listOf(
                        "바닥에 앉아 무릎을 살짝 구부린다.",
                        "밴드를 잡아 몸 앞에서 고정한다."
                    ),
                    exerciseMotion = listOf(
                        "몸통을 좌우로 회전하며 밴드를 당긴다.",
                        "복부에 힘을 주고 허리를 곧게 편다."
                    ),
                    breathing = listOf("몸통을 돌릴 때 숨을 내쉬고 돌아올 때 숨을 들이쉰다."),
                    caution = listOf(
                        "허리가 말리지 않도록 코어에 힘을 준다.",
                        "과도한 몸통 회전은 피한다."
                    ),
                    mets = 4.5,
                )
            )

            val burpeeId = exerciseDao.insert(
                Exercise(
                    id = 97,
                    name = "버피",
                    part = "유산소, 복근, 하체",
                    equip = "맨몸",
                    imagePath = "burpee",
                    startPosition = listOf("선 자세에서 시작."),
                    exerciseMotion = listOf("스쿼트 → 플랭크 → 푸쉬업 → 점프를 연속 수행."),
                    breathing = listOf("점프할 때 내쉬고, 플랭크로 내려갈 때 들이쉰다."),
                    caution = listOf("반동 대신 전신 근육 사용."),
                    mets = 7.5,
                    isNoise = true
                )
            )
            val dumbbellFlyId = exerciseDao.insert(
                Exercise(
                    id = 99,
                    name = "덤벨 플라이",
                    part = "가슴",
                    equip = "덤벨",
                    imagePath = "dumbbell_fly", //
                    startPosition = listOf("벤치에 누워 팔을 약간 굽혀 덤벨을 머리 위로 든다."),
                    exerciseMotion = listOf("팔을 옆으로 벌려 가슴을 늘리고 천천히 모으며 수축."),
                    breathing = listOf("모을 때 내쉬고, 벌릴 때 들이쉰다."),
                    mets = 5.8
                )
            )
            val supermanRowId = exerciseDao.insert(
                Exercise(
                    id = 100,
                    name = "슈퍼맨 로우",
                    part = "등, 복근, 하체",
                    equip = "밴드",
                    imagePath = "superman_row",
                    startPosition = listOf(
                        "바닥을 보고 엎드린다.",
                        "밴드를 어깨너비보다 조금 넓게 잡고 머리 위로 뻗는다."
                    ),
                    exerciseMotion = listOf(
                        "가슴과 머리를 들면서 팔을 쇄골 쪽으로 당긴다.",
                        "팔을 앞으로 뻗으면서 시작 자세로 돌아간다."
                    ),
                    breathing = listOf("상체를 올릴 때 숨을 내쉬고 상체를 내릴 때 숨을 들이쉰다."),
                    caution = listOf("무리해서 상체를 들지 않도록 조심한다."),
                    mets = 5.0,
                )
            )

            val singleKBSPressId = exerciseDao.insert(
                Exercise(
                    id = 101,
                    name = "원암 케틀벨 숄더 프레스",
                    part = "어깨, 팔",
                    equip = "케틀벨",
                    imagePath = "single_kb_shoulder_press",
                    startPosition = listOf("한 손에 케틀벨을 랙 포지션."),
                    exerciseMotion = listOf("팔을 위로 밀어 케틀벨을 머리 위로 올리고 내려온다."),
                    breathing = listOf("올릴 때 내쉬고, 내릴 때 들이쉬기."),
                    caution = listOf("몸이 기울지 않도록 코어 고정."),
                    mets = 6.2
                )
            )
            val bandBicycleCrunchId = exerciseDao.insert(
                Exercise(
                    id = 103,
                    name = "밴드 바이시클 크런치",
                    part = "복근",
                    equip = "밴드",
                    imagePath = "band_bicycle_crunch",
                    startPosition = listOf(
                        "바닥에 누워 무릎을 구부리고 발을 들어 올린다.",
                        "밴드를 발에 걸고 고정한다.",
                        "손은 머리 뒤에 두거나 가볍게 머리를 받친다."
                    ),
                    exerciseMotion = listOf(
                        "반대쪽 팔꿈치와 무릎이 만나도록 상체를 비틀며 크런치를 실시한다.",
                        "다른 쪽 다리는 뻗어 밴드를 잡아 당긴다.",
                        "반대쪽도 같은 동작을 반복한다."
                    ),
                    breathing = listOf("상체를 비틀 때 숨을 내쉬고 돌아올 때 숨을 들이쉰다."),
                    caution = listOf(
                        "허리가 바닥에서 들리지 않도록 복부에 힘을 준다.",
                        "과도한 몸통 회전을 피한다.",
                        "밴드의 긴장 상태를 유지하며 통제된 동작을 한다."
                    ),
                    mets = 4.5,
                )
            )

            val bicycleCrunchCardioId = exerciseDao.insert(
                Exercise(
                    id = 104,
                    name = "하늘 자전거",
                    part = "유산소, 복근",
                    equip = "맨몸",
                    imagePath = "bicycle_crunch_cardio",
                    startPosition = listOf("등을 대고 누워 무릎 90°, 손은 머리 뒤."),
                    exerciseMotion = listOf("무릎-팔꿈치 교차 자전거 동작을 빠르게 반복."),
                    breathing = listOf("회전 시 내쉬고, 반대에서 들이쉬기."),
                    caution = listOf("허리 뜨지 않도록."),
                    mets = 5.2
                )
            )
            val sideBandWalkId = exerciseDao.insert(
                Exercise(
                    id = 105,
                    name = "사이드 밴드 워크",
                    part = "하체",
                    equip = "세라밴드",
                    imagePath = "side_band_walk", //
                    startPosition = listOf("무릎 위·아래에 세라밴드를 감고 반쯤 앉는다."),
                    exerciseMotion = listOf("발을 옆으로 벌리며 밴드를 늘린다.", "천천히 반대발을 모으며 이동한다."),
                    breathing = listOf("옆으로 이동할 때 내쉬고, 모을 때 들이쉰다."),
                    caution = listOf("무릎이 안쪽으로 모이지 않게 한다."),
                    mets = 3.5
                )
            )

            val shoulderTapPlankId = exerciseDao.insert(
                Exercise(
                    id = 108,
                    name = "숄더 탭 플랭크",
                    part = "어깨, 복근",
                    equip = "맨몸",
                    imagePath = "shoulder_tap_plank",
                    startPosition = listOf("플랭크 자세에서 시작."),
                    exerciseMotion = listOf("한 손으로 반대 어깨를 터치, 교대 반복."),
                    breathing = listOf("터치 시 내쉬고, 손 내릴 때 들이쉬기."),
                    caution = listOf("골반 흔들림 최소화."),
                    mets = 4.1,
                    isTimeType = true
                )
            )
            val bandWoodChopId = exerciseDao.insert(
                Exercise(
                    id = 110,
                    name = "밴드 우드 촙",
                    part = "복근",
                    equip = "밴드",
                    imagePath = "wood_chop",
                    startPosition = listOf(
                        "밴드를 머리 위쪽에 고정한다.",
                        "밴드를 잡고 발은 어깨 너비로 벌린다.",
                        "밴드를 머리 위쪽에서 잡아 몸 옆에 위치시킨다."
                    ),
                    exerciseMotion = listOf(
                        "밴드를 잡은 팔을 몸 옆으로 내리며 몸통을 회전한다.",
                        "복부에 힘을 주며 천천히 시작 자세로 돌아간다."
                    ),
                    breathing = listOf("밴드를 내릴 때 숨을 내쉬고 돌아올 때 숨을 들이쉰다."),
                    caution = listOf(
                        "허리가 과도하게 꺾이지 않도록 주의한다.",
                        "복부에 힘을 주고 천천히 통제된 동작을 한다."
                    ),
                    mets = 4.5,
                )
            )

            val mountainClimberCardioId = exerciseDao.insert(
                Exercise(
                    id = 111,
                    name = "마운틴 클라이머",
                    part = "유산소, 복근",
                    equip = "맨몸",
                    imagePath = "mountain_climber",
                    startPosition = listOf("푸쉬업 자세에서 시작."),
                    exerciseMotion = listOf("무릎을 번갈아 가슴 쪽으로 빠르게 당긴다."),
                    breathing = listOf("리듬 호흡 유지."),
                    caution = listOf("골반 위치 유지."),
                    mets = 5.8,
                    isNoise = true,
                    isTimeType = true
                )
            )
            val stepboxSquatId = exerciseDao.insert(
                Exercise(
                    id = 112,
                    name = "스탭박스 스쿼트",
                    part = "하체",
                    equip = "스텝박스",
                    imagePath = "squat_stepper",
                    startPosition = listOf("스텝박스 앞에 선다."),
                    exerciseMotion = listOf("박스 위에 올라가면서 스쿼트 동작을 수행한다.", "다시 내려와 재차 반복."),
                    breathing = listOf("올라갈 때 내쉬고, 내려올 때 들이쉰다."),
                    caution = listOf("무릎 충격을 완화하기 위해 부드럽게 착지한다."),
                    mets = 5.0
                )

            )
            val birdDogId = exerciseDao.insert(
                Exercise(
                    id = 114,
                    name = "버드독",
                    part = "등, 하체, 복근",
                    equip = "맨몸",
                    imagePath = "bird_dog",
                    startPosition = listOf(
                        "손과 무릎을 바닥에 대고 손바닥은 바닥에 평평하게 어깨 너비로 벌린다.",
                        "코어를 이완하여 허리와 복부가 자연스러운 자세를 유지한다."
                    ),
                    exerciseMotion = listOf(
                        "복부에 힘을 주고 오른팔과 왼다리를 몸과 일직선이 되도록 들어 올린다.",
                        "3~5초간 자세를 유지한다.",
                        "시작 자세로 돌아온다.",
                        "반대쪽 왼팔과 오른다리로 동일 동작을 반복한다.",
                        "팔과 다리를 번갈아 가며 교대로 실시한다."
                    ),
                    breathing = listOf("동작을 수행하며 자연스럽게 호흡한다."),
                    caution = listOf(
                        "엉덩이와 허리가 흔들리지 않도록 유지한다.",
                        "코어에 힘을 주고 안정적인 자세를 유지한다."
                    ),
                    mets = 3.5,
                    isTimeType = true
                )
            )

            val plankArmRaiseId = exerciseDao.insert(
                Exercise(
                    id = 115,
                    name = "플랭크 + 암 레이즈",
                    part = "어깨, 복근",
                    equip = "맨몸",
                    imagePath = "plank_arm_raise",
                    startPosition = listOf("플랭크 자세."),
                    exerciseMotion = listOf("한 팔을 앞으로 들어 올려 1초 유지 후 내린다.", "반대 팔 반복."),
                    breathing = listOf("올릴 때 내쉬고, 내릴 때 들이쉬기."),
                    caution = listOf("허리가 처지지 않도록."),
                    mets = 4.0,
                    isTimeType = true
                )
            )
            val bandTricepKickbackId = exerciseDao.insert(
                Exercise(
                    id = 116,
                    name = "트라이셉 킥백",
                    part = "팔",
                    equip = "세라밴드",
                    imagePath = "band_tricep_kickback",
                    startPosition = listOf("밴드를 발에 고정, 상체 45° 숙여 손잡이를 잡는다."),
                    exerciseMotion = listOf("팔꿈치를 고정하고 팔을 뒤로 펴 삼두를 수축.", "천천히 굽혀 돌아온다."),
                    breathing = listOf("팔을 펴며 내쉬고, 굽히며 들이쉬기."),
                    caution = listOf("팔꿈치 위치 고정 유지."),
                    mets = 5.5
                )
            )
            val splitJumpBoxId = exerciseDao.insert(
                Exercise(
                    id = 118,
                    name = "스플릿 점프 박스",
                    part = "유산소, 하체",
                    equip = "스텝박스",
                    imagePath = "split_jump_box",
                    startPosition = listOf("스텝박스 앞에 런지 자세."),
                    exerciseMotion = listOf("발을 교대 점프하여 다른 쪽 발을 박스 위에 올린다."),
                    breathing = listOf("점프 시 내쉬고, 착지 시 들이쉰다."),
                    caution = listOf("무릎 충격 완화."),
                    mets = 6.2,
                    isNoise = true
                )
            )
            val bandLegExtensionId = exerciseDao.insert(
                Exercise(
                    id = 119,
                    name = "밴드 레그 익스텐션",
                    part = "하체",
                    equip = "세라밴드",
                    imagePath = "band_leg_extension", //
                    startPosition = listOf("세라밴드를 발목에 고정 후 의자에 앉는다."),
                    exerciseMotion = listOf("밴드를 저항으로 삼아 다리를 펴서 무릎을 완전히 뻗는다.", "천천히 굽혀 원위치한다."),
                    breathing = listOf("다리를 펴며 내쉬고, 굽히며 들이쉰다."),
                    caution = listOf("허리가 뒤로 굴곡되지 않도록 등받이에 기대지 않는다."),
                    mets = 3.8
                )
            )
            val pushUpId = exerciseDao.insert(
                Exercise(
                    id = 120,
                    name = "푸쉬업",
                    part = "가슴, 팔",
                    equip = "맨몸",
                    imagePath = "push_up", //
                    startPosition = listOf("어깨너비 손으로 플랭크."),
                    exerciseMotion = listOf("가슴을 바닥으로 내렸다가 밀어 올린다."),
                    breathing = listOf("내릴 때 들이쉬고, 올릴 때 내쉰다."),
                    caution = listOf("팔꿈치가 과도하게 벌어지지 않도록한다."),
                    mets = 4.0
                )
            )
            val dolphinPlankId = exerciseDao.insert(
                Exercise(
                    id = 122,
                    name = "돌핀 플랭크",
                    part = "어깨, 복근",
                    equip = "맨몸",
                    imagePath = "dolphin_plank",
                    startPosition = listOf("엘보우 플랭크 자세."),
                    exerciseMotion = listOf("엉덩이를 위로 들어 돌핀 자세로 만들었다가 다시 플랭크로."),
                    breathing = listOf("올라갈 때 내쉬고, 내려갈 때 들이쉬기."),
                    caution = listOf("어깨를 귀에서 멀리."),
                    mets = 3.8
                )
            )
            val bandOverheadTricepsId = exerciseDao.insert(
                Exercise(
                    id = 123,
                    name = "오버헤드 트라이셉 익스텐션",
                    part = "팔",
                    equip = "세라밴드",
                    imagePath = "band_overhead_triceps",
                    startPosition = listOf("밴드를 발에 고정, 양손으로 머리 위 뒤쪽 잡는다."),
                    exerciseMotion = listOf("팔꿈치를 고정하고 팔을 위로 펴 삼두 수축.", "천천히 굽혀 원위치."),
                    breathing = listOf("펴며 내쉬고, 굽히며 들이쉬기."),
                    caution = listOf("허리가 과신전되지 않도록."),
                    mets = 5.8
                )
            )
            val stepAerobicId = exerciseDao.insert(
                Exercise(
                    id = 125,
                    name = "스텝 에어로빅",
                    part = "유산소",
                    equip = "스텝박스",
                    imagePath = "step_aerobic",
                    startPosition = listOf("스텝박스 앞에 선다."),
                    exerciseMotion = listOf("음악 리듬에 맞춰 다양한 스텝 패턴 수행."),
                    breathing = listOf("리듬 호흡."),
                    caution = listOf("발 전체를 박스에 올려 안정성 확보."),
                    mets = 5.5,
                    isNoise = true,
                    isTimeType = true
                )
            )
            val sideStepperId = exerciseDao.insert(
                Exercise(
                    id = 126,
                    name = "사이드 스텝퍼",
                    part = "하체",
                    equip = "스텝박스",
                    imagePath = "side_stepper",
                    startPosition = listOf("스텝박스 옆에 선다."),
                    exerciseMotion = listOf("가까운 발을 박스 위로 올리고 체중을 실어 올라선다.", "다른 발을 올린 뒤 내려오며 반복."),
                    breathing = listOf("올라갈 때 내쉬고, 내려올 때 들이쉰다."),
                    caution = listOf("발 전체를 박스에 올려 안전을 확보한다."),
                    mets = 4.6
                )
            )
            val reversePlankId = exerciseDao.insert(
                Exercise(
                    id = 127,
                    name = "리버스 플랭크",
                    part = "하체, 복근",
                    equip = "맨몸",
                    imagePath = "reverse_plank",
                    startPosition = listOf(
                        "바닥에 다리를 펴고 앉는다.",
                        "엉덩이에서 한 뼘 정도 떨어진 곳에 두 손을 바닥에 짚는다."
                    ),
                    exerciseMotion = listOf(
                        "무릎을 펴고 앉은 상태에서 양손은 어깨보다 뒤에 위치시키고 손끝은 앞을 향하도록 지지한다.",
                        "그 상태에서 엉덩이를 들어 올려 머리, 배, 다리가 일직선이 되도록 한다.",
                        "머리는 뒤로 젖혀지지 않도록 주의하며 주어진 시간 동안 온 몸에 긴장을 놓지 않고 버틴다."
                    ),
                    breathing = listOf("엉덩이를 들어 올릴 때 숨을 들이쉬고 몸을 내릴 때 숨을 내쉰다."),
                    caution = listOf(
                        "몸을 들어 올릴 때 어깨와 등을 곧게 유지하고 복부에 힘을 준다.",
                        "어깨에 무리가 가지 않도록 팔을 일직선으로 만든다.",
                        "힙이 바닥에 닿지 않고 유지할 수 있도록 코어에 집중한다."
                    ),
                    mets = 4.0,
                )
            )
            val upDownPlankId = exerciseDao.insert(
                Exercise(
                    id = 129,
                    name = "업 다운 플랭크",
                    part = "팔, 복근",
                    equip = "맨몸",
                    imagePath = "up_down_plank",
                    startPosition = listOf("플랭크 자세에서 시작."),
                    exerciseMotion = listOf("한 팔씩 팔꿈치→손바닥으로 체중을 전환하며 올라갔다 내려온다."),
                    breathing = listOf("올라올 때 내쉬고, 내려갈 때 들이쉬기."),
                    caution = listOf("골반 흔들림 최소화."),
                    mets = 4.2
                )
            )
            val plankJackId = exerciseDao.insert(
                Exercise(
                    id = 130,
                    name = "플랭크 잭",
                    part = "복근, 등",
                    equip = "맨몸",
                    imagePath = "plank_jack",
                    startPosition = listOf(
                        "양팔을 어깨너비로 벌리고 무릎을 편 상태로 엎드린다.",
                        "두 손은 어깨 아래에 두고 바닥에 고정한다.",
                        "몸이 일직선이 되도록 엉덩이와 코어에 힘을 준다."
                    ),
                    exerciseMotion = listOf(
                        "팔을 어깨너비로 벌려 엎드린 자세에서 코어에 가볍게 힘을 주어 몸을 일직선으로 만든다.",
                        "두 발로 동시에 바닥을 차며 어깨너비보다 넓게 착지한다.",
                        "다시 두 발을 동시에 바닥을 차며 시작 자세로 돌아온다."
                    ),
                    breathing = listOf("숨을 들이마시며 다리를 벌리고 내쉬며 다리를 모은다."),
                    caution = listOf(
                        "빠른 템포로 진행하면 코어에 힘이 풀려 허리에 무리가 갈 수 있다.",
                        "코어에 힘이 부족하다면 동작을 천천히 진행하고 코어에 힘을 주는 연습을 한다."
                    ),
                    mets = 5.0,
                    isNoise = true
                )
            )
            val stepBoxBurpeeId = exerciseDao.insert(
                Exercise(
                    id = 131,
                    name = "스텝박스 버피",
                    part = "유산소, 전신",
                    equip = "스텝박스",
                    imagePath = "step_box_burpee",
                    startPosition = listOf("스텝박스 앞에 서서 시작."),
                    exerciseMotion = listOf("버피 동작 중 점프 단계에서 박스 위로 점프하여 착지."),
                    breathing = listOf("점프 시 내쉬고, 내려갈 때 들이쉰다."),
                    caution = listOf("박스 모서리 안전 주의."),
                    mets = 7.8,
                    isNoise = true
                )
            )
            val backLungeStepperId = exerciseDao.insert(
                Exercise(
                    id = 132,
                    name = "백런지 스텝퍼",
                    part = "하체",
                    equip = "스텝박스",
                    imagePath = "back_lunge_stepper",
                    startPosition = listOf("스텝박스 앞에 서서 한 발을 박스 위에 올린다."),
                    exerciseMotion = listOf(
                        "박스 위에 있는 발로 체중을 지탱하며 다른 다리를 뒤로 런지한다.",
                        "박스 위로 밀어 올라와 원위치."
                    ),
                    breathing = listOf("내려갈 때 들이쉬고, 올라올 때 내쉰다."),
                    caution = listOf("무릎 정렬을 유지한다."),
                    mets = 4.5
                )
            )
            val plankTwistId = exerciseDao.insert(
                Exercise(
                    id = 133,
                    name = "플랭크 트위스트",
                    part = "어깨, 복근",
                    equip = "스텝박스",
                    imagePath = "plank_twist",
                    startPosition = listOf("스텝박스 위에 손을 두고 플랭크."),
                    exerciseMotion = listOf("엉덩이를 좌우로 회전하며 복근과 어깨 안정화."),
                    breathing = listOf("회전할 때 내쉬고, 중앙 복귀 시 들이쉬기."),
                    caution = listOf("허리가 과도히 꺾이지 않도록."),
                    mets = 4.2
                )
            )
            val armLiftPlankId = exerciseDao.insert(
                Exercise(
                    id = 134,
                    name = "암 리프트 플랭크",
                    part = "팔, 어깨, 복근",
                    equip = "맨몸",
                    imagePath = "arm_lift_plank",
                    startPosition = listOf("플랭크에서 한 팔을 앞으로 들어 올린다."),
                    exerciseMotion = listOf("1초 유지 후 내리고 반대 팔 반복."),
                    breathing = listOf("올릴 때 내쉬고, 내릴 때 들이쉬기."),
                    mets = 4.0
                )
            )
            val gymballJumpId = exerciseDao.insert(
                Exercise(
                    id = 136,
                    name = "짐볼 점핑",
                    part = "유산소",
                    equip = "짐볼",
                    imagePath = "gymball_jump",
                    startPosition = listOf("짐볼 위에 앉아 균형 유지."),
                    exerciseMotion = listOf("볼 위에서 연속으로 점프하며 유산소 운동."),
                    breathing = listOf("점프 시 내쉬고, 착지 시 들이쉰다."),
                    caution = listOf("짐볼 미끄럼 및 파손 주의."),
                    mets = 4.9,
                    isTimeType = true,
                    isNoise = true
                )
            )
            val plankLegLiftId = exerciseDao.insert(
                Exercise(
                    id = 137,
                    name = "플랭크 레그 리프트",
                    part = "하체, 복근",
                    equip = "맨몸",
                    imagePath = "plank_leg_lift",
                    startPosition = listOf("플랭크 기본 자세를 취한다."),
                    exerciseMotion = listOf(
                        "한쪽 다리를 곧게 펴서 천천히 들어 올린다.",
                        "엉덩이와 허리에 힘을 주며 1초 정지 후 내린다.",
                        "반대쪽도 반복한다."
                    ),
                    breathing = listOf("다리를 올릴 때 내쉬고, 내릴 때 들이쉰다."),
                    caution = listOf("골반이 흔들리지 않도록 코어를 고정한다."),
                    mets = 4.2
                )
            )
            val reversePlankArmKickbackId = exerciseDao.insert(
                Exercise(
                    id = 138,
                    name = "리버스 플랭크 암 킥백",
                    part = "팔, 어깨, 등",
                    equip = "맨몸",
                    imagePath = "reverse_plank_arm_kickback",
                    startPosition = listOf("리버스 플랭크 자세."),
                    exerciseMotion = listOf("한 팔을 뒤로 펴 삼두를 수축한 후 내린다.", "반대 팔 반복."),
                    breathing = listOf("펴며 내쉬고, 내리며 들이쉬기."),
                    caution = listOf("엉덩이가 처지지 않도록."),
                    mets = 4.3
                )
            )
            val dipsStepId = exerciseDao.insert(
                Exercise(
                    id = 141,
                    name = "스텝박스 딥스",
                    part = "팔, 가슴",
                    equip = "스텝박스",
                    imagePath = "dips_step",
                    startPosition = listOf("손을 스텝박스 뒤에 짚고 다리를 뻗는다."),
                    exerciseMotion = listOf("팔꿈치를 굽혀 몸을 내렸다가 밀어 올린다."),
                    breathing = listOf("내릴 때 들이쉬고, 올릴 때 내쉬기."),
                    caution = listOf("어깨 관절 과부하 주의."),
                    mets = 5.2
                )
            )
            val gymballTricepsExtensionId = exerciseDao.insert(
                Exercise(
                    id = 144,
                    name = "트라이셉스 익스텐션",
                    part = "팔",
                    equip = "짐볼, 덤벨",
                    imagePath = "gymball_triceps_extension",
                    startPosition = listOf("짐볼 위에 등을 대고 누워 덤벨을 머리 위로 든다."),
                    exerciseMotion = listOf("팔꿈치를 굽혀 덤벨을 머리 뒤로 내렸다가 팔을 펴 올린다."),
                    breathing = listOf("펴며 내쉬고, 굽히며 들이쉬기."),
                    caution = listOf("팔꿈치가 퍼지지 않도록."),
                    mets = 5.7
                )
            )
            val gymBallCrunchId = exerciseDao.insert(
                Exercise(
                    id = 145,   //104로 되어있었음!! 수정함.
                    name = "짐볼 크런치",
                    part = "복근",
                    equip = "짐볼",
                    imagePath = "gym_ball_crunch",
                    startPosition = listOf(
                        "골반과 허리, 어깨를 짐볼 위에 대고 눕는다.",
                        "다리는 어깨너비로 벌리고 발은 지면에 밀착한다.",
                        "손끝은 귀 뒤에 대고 팔꿈치는 어깨선과 일직선이 되도록 옆으로 벌린다."
                    ),
                    exerciseMotion = listOf(
                        "골반을 향해 가슴을 잡아당기듯이 머리와 어깨를 들어 올린다.",
                        "엉덩이가 아래로 처지지 않도록 주의하며 최고 지점에서 잠시 멈춘다.",
                        "천천히 시작 자세로 돌아와 반복한다."
                    ),
                    breathing = listOf("크런치 동작 시 숨을 내쉬고, 돌아올 때 숨을 들이쉰다."),
                    caution = listOf(
                        "엉덩이가 아래로 처지지 않도록 주의한다.",
                        "동작을 천천히 통제하며 실시한다."
                    ),
                    mets = 4.5,
                )
            )

            val kettlebellGobletSquatId = exerciseDao.insert(
                Exercise(
                    id = 146,
                    name = "케틀벨 고블릿 스쿼트",
                    part = "하체",
                    equip = "케틀벨",
                    imagePath = "kettlebell_goblet_squat",
                    startPosition = listOf(
                        "케틀벨을 몸 앞에 위치시키고 양손으로 들어준다.",
                        "다리는 어깨너비로 벌리고 발끝은 20~30도 정도 벌린다.",
                        "가슴을 펴고 허리를 세운다."
                    ),
                    exerciseMotion = listOf(
                        "케틀벨을 가슴 앞에 들고 다리를 어깨 너비로 벌린다.",
                        "엉덩이를 뒤로 빼면서 무릎을 구부려 앉는다.",
                        "다리를 펴며 원래 위치로 돌아간다."
                    ),
                    breathing = listOf("내려갈 때 숨을 들이쉬고 올라갈 때 숨을 내쉰다."),
                    caution = listOf(
                        "과도하게 상체를 앞으로 숙이지 않도록 주의한다.",
                        "허리가 꺾이지 않도록 가슴을 펴고 코어에 힘을 준다.",
                        "허리를 세운다고 과도하게 꺾지 않는다.",
                        "엉덩이를 너무 뒤로 빼지 않는다.",
                        "발이 지면에서 떨어지지 않도록 한다.",
                        "일어날 때 무릎이 안으로 모이지 않도록 한다."
                    ),
                    mets = 6.0,
                )
            )
            val gymBallWallSquatId = exerciseDao.insert(
                Exercise(
                    id = 150,
                    name = "짐볼 월 스쿼트",
                    part = "하체",
                    equip = "짐볼",
                    imagePath = "gym_ball_wall_squat",
                    startPosition = listOf(
                        "팔을 옆구리에 세우고 허리와 벽 사이에 짐볼을 놓는다.",
                        "어깨 너비로 발을 단단히 바닥에 고정한다."
                    ),
                    exerciseMotion = listOf(
                        "몸을 바닥 쪽으로 낮추고 코어를 단단히 유지하며 공을 따라 등을 굴린다.",
                        "숨을 내쉬면서 발 뒤꿈치를 밀고 천천히 제자리로 돌아온다."
                    ),
                    breathing = listOf("내릴 때 숨을 들이쉬고 올라올 때 숨을 내쉰다."),
                    caution = listOf(
                        "코어에 힘을 주고 몸의 균형을 유지한다.",
                        "등이 지나치게 굽지 않도록 주의한다."
                    ),
                    mets = 4.3,
                )
            )
            val gymBallHipBridgeId = exerciseDao.insert(
                Exercise(
                    id = 151,
                    name = "짐볼 힙 브릿지",
                    part = "하체",
                    equip = "짐볼",
                    imagePath = "gym_ball_hip_bridge",
                    startPosition = listOf(
                        "짐볼에 등을 기대고 앉는다.",
                        "두 손은 허리 옆이나 가슴 앞쪽에 포개어 위치시킨다."
                    ),
                    exerciseMotion = listOf(
                        "발로 지면을 누르고, 무릎은 바깥쪽으로 미는 힘을 만들며 동시에 엉덩이를 들어 올린다.",
                        "엉덩이를 들어올리기 전에 코어에 힘을 주어 척추를 안정시킨다.",
                        "8~10초간 자세를 유지한다.",
                        "천천히 엉덩이를 내린다."
                    ),
                    breathing = listOf(
                        "엉덩이를 들어 올릴 때 숨을 내쉬고, 내릴 때 숨을 들이쉰다."
                    ),
                    caution = listOf(
                        "목과 어깨가 과도하게 눌리지 않도록 주의한다.",
                        "허리가 과도하게 폄 상태가 되거나 부족한 폄이 없는지 확인한다.",
                        "발 위치에 따라 엉덩이와 햄스트링 활성 차이가 있으니 본인에게 맞는 위치를 찾는다.",
                        "팔로 바닥을 과하게 누르지 않도록 한다.",
                        "기본 브릿지 동작에 익숙하지 않으면 단계적으로 진행한다."
                    ),
                    mets = 4.5,
                )
            )

            // ✅ 오늘 날짜 기준으로 서버 동기화 날짜를 기반으로 plan 생성
            val todaySeoulZoneId = ZoneId.of("Asia/Seoul")
            val todayDate = LocalDate.now(todaySeoulZoneId)
            val todayMillis = todayDate.atStartOfDay(todaySeoulZoneId).toInstant().toEpochMilli()

            val planAId = exercisePlanDao.insert(ExercisePlan(plannedDate = todayMillis)).let {
                if (it == 0L) {
                    exercisePlanDao.getPlansByDate(todayMillis, todayMillis).first().id
                } else {
                    it
                }
            }

// ✅ PlanDetail: PT체조를 테스트용으로 연결
            planDetailDao.insert(
                PlanDetail(
                    exercisePlanId = planAId,
                    exerciseId = ptDrillId, // 반드시 위에서 정의된 PT체조 id 사용
                    exOrder = 1
                )
            )

// ✅ ExerciseSet: PT체조 세트 3개 추가
            for (i in 1..3) {
                exerciseSetDao.insert(
                    ExerciseSet(
                        exercisePlanId = planAId,
                        exerciseId = ptDrillId,
                        setNumber = i,
                        weight = 0,
                        reps = 12,
                        isCompleted = false
                    )
                )
            }

            ExerciseSet(
                exercisePlanId = planAId,
                exerciseId = squatId,
                setNumber = 1,
                weight = 0,
                reps = 15,
                isCompleted = false
            )




//
//            // 오늘의 ExercisePlan을 생성하는 방식 통일 (초기 데이터에도 적용)
//            val todaySeoulZoneId = ZoneId.of("Asia/Seoul")
//            // 2025년 5월 15일 00:00:00.000 KST에 해당하는 밀리초 타임스탬프 계산
//            val timestampMay19Seoul = LocalDate.of(2025, 5, 19)
//                .atStartOfDay(ZoneId.of("Asia/Seoul"))
//                .toInstant()
//                .toEpochMilli()
//            // 2025년 5월 15일 00:00:00.000 KST에 해당하는 밀리초 타임스탬프 계산
//            val timestampMay15Seoul = LocalDate.of(2025, 5, 15)
//                .atStartOfDay(ZoneId.of("Asia/Seoul"))
//                .toInstant()
//                .toEpochMilli()
//            // 오늘 날짜
//            val todayMidnightMillis = LocalDate.now(todaySeoulZoneId)
//                .atStartOfDay(todaySeoulZoneId)
//                .toInstant()
//                .toEpochMilli()
//
//            //  5월 15일 루틴 생성 시 안전하게 ID 확보 (더미)
//            val planMay15Id =
//                exercisePlanDao.insert(ExercisePlan(plannedDate = timestampMay15Seoul)).let {
//                    if (it == 0L) {
//                        exercisePlanDao.getPlansByDate(timestampMay15Seoul, timestampMay15Seoul)
//                            .first().id
//                    } else {
//                        it
//                    }
//                }
//
//            //  5월 19일 루틴 생성 시 안전하게 ID 확보 (더미)
//            val planMay19Id =
//                exercisePlanDao.insert(ExercisePlan(plannedDate = timestampMay19Seoul)).let {
//                    if (it == 0L) {
//                        exercisePlanDao.getPlansByDate(timestampMay19Seoul, timestampMay19Seoul)
//                            .first().id
//                    } else {
//                        it
//                    }
//                }
//
//            //  오늘 루틴 생성 시 안전하게 ID 확보
//            val planAId =
//                exercisePlanDao.insert(ExercisePlan(plannedDate = todayMidnightMillis)).let {
//                    if (it == 0L) {
//                        exercisePlanDao.getPlansByDate(todayMidnightMillis, todayMidnightMillis)
//                            .first().id
//                    } else {
//                        it
//                    }
//                }
//
//
//            // PlanDetail 초기 데이터 삽입 (운동 계획과 운동 연결)
//            planDetailDao.insert(
//                PlanDetail(
//                    exercisePlanId = planAId,
//                    exerciseId = squatId,
//                    exOrder = 1,
//                )
//            )
//            planDetailDao.insert(
//                PlanDetail(
//                    exercisePlanId = planAId,
//                    exerciseId = pushUpId,
//                    exOrder = 2,
//                )
//            )
//            planDetailDao.insert(
//                PlanDetail(
//                    exercisePlanId = planAId,
//                    exerciseId = lungeId,
//                    exOrder = 3,
//                )
//            )
//
//            planDetailDao.insert(
//                PlanDetail(
//                    exercisePlanId = planMay15Id,
//                    exerciseId = squatId,
//                    exOrder = 1,
//                    isCompleted = true
//                )
//            )
//            planDetailDao.insert(
//                PlanDetail(
//                    exercisePlanId = planMay15Id,
//                    exerciseId = donkeyKickId,
//                    exOrder = 2,
//                )
//            )
//            planDetailDao.insert(
//                PlanDetail(
//                    exercisePlanId = planMay15Id,
//                    exerciseId = armWalkingId,
//                    exOrder = 3,
//                    isCompleted = true
//                )
//            )
//
//            planDetailDao.insert(
//                PlanDetail(
//                    exercisePlanId = planMay19Id,
//                    exerciseId = squatId,
//                    exOrder = 1,
//                    isCompleted = true
//                )
//            )
//            planDetailDao.insert(
//                PlanDetail(
//                    exercisePlanId = planMay19Id,
//                    exerciseId = donkeyKickId,
//                    exOrder = 2,
//                    isCompleted = true
//                )
//            )
//            planDetailDao.insert(
//                PlanDetail(
//                    exercisePlanId = planMay19Id,
//                    exerciseId = armWalkingId,
//                    exOrder = 3,
//                    isCompleted = true
//                )
//            )
//
//
//            // ExerciseSet 초기 데이터 삽입 (각 운동 계획 내 운동의 세트)
//            exerciseSetDao.insert( //운동 계쇡별로, 어떤 운동을 몇 세트, 몇회, 무게로 수행하는지 초기화!
//                ExerciseSet(
//                    exercisePlanId = planAId,
//                    exerciseId = squatId,
//                    setNumber = 1,
//                    weight = 0,
//                    reps = 15
//                )
//            )
//            exerciseSetDao.insert(
//                ExerciseSet(
//                    exercisePlanId = planAId,
//                    exerciseId = squatId,
//                    setNumber = 2,
//                    weight = 0,
//                    reps = 15
//                )
//            )
//            exerciseSetDao.insert(
//                ExerciseSet(
//                    exercisePlanId = planAId,
//                    exerciseId = squatId,
//                    setNumber = 3,
//                    weight = 0,
//                    reps = 10
//                )
//            )
//            // Plan A - 푸쉬업
//            exerciseSetDao.insert(
//                ExerciseSet(
//                    exercisePlanId = planAId,
//                    exerciseId = pushUpId,
//                    setNumber = 1,
//                    weight = 0,
//                    reps = 15
//                )
//            )
//            exerciseSetDao.insert(
//                ExerciseSet(
//                    exercisePlanId = planAId,
//                    exerciseId = pushUpId,
//                    setNumber = 2,
//                    weight = 0,
//                    reps = 10
//                )
//            )
//            exerciseSetDao.insert(
//                ExerciseSet(
//                    exercisePlanId = planAId,
//                    exerciseId = pushUpId,
//                    setNumber = 3,
//                    weight = 0,
//                    reps = 10
//                )
//            )
//            // Plan A - 런지
//            exerciseSetDao.insert(
//                ExerciseSet(
//                    exercisePlanId = planAId,
//                    exerciseId = lungeId,
//                    setNumber = 1,
//                    weight = 0,
//                    reps = 60
//                )
//            )
//
//            exerciseSetDao.insert(
//                ExerciseSet(
//                    exercisePlanId = planMay15Id,
//                    exerciseId = donkeyKickId,
//                    setNumber = 1,
//                    weight = 0,
//                    reps = 60
//                )
//            )
//            exerciseSetDao.insert(
//                ExerciseSet(
//                    exercisePlanId = planMay15Id,
//                    exerciseId = squatId,
//                    setNumber = 1,
//                    weight = 0,
//                    reps = 60
//                )
//            )
//            exerciseSetDao.insert(
//                ExerciseSet(
//                    exercisePlanId = planMay15Id,
//                    exerciseId = armWalkingId,
//                    setNumber = 1,
//                    weight = 0,
//                    reps = 60
//                )
//            )
//
//            exerciseSetDao.insert(
//                ExerciseSet(
//                    exercisePlanId = planMay19Id,
//                    exerciseId = donkeyKickId,
//                    setNumber = 1,
//                    weight = 0,
//                    reps = 60
//                )
//            )
//            exerciseSetDao.insert(
//                ExerciseSet(
//                    exercisePlanId = planMay19Id,
//                    exerciseId = squatId,
//                    setNumber = 1,
//                    weight = 0,
//                    reps = 60
//                )
//            )
//            exerciseSetDao.insert(
//                ExerciseSet(
//                    exercisePlanId = planMay19Id,
//                    exerciseId = armWalkingId,
//                    setNumber = 1,
//                    weight = 0,
//                    reps = 60
//                )
//            )
        }
    }
}