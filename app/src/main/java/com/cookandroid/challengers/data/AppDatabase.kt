package com.cookandroid.challengers.data.db

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cookandroid.challengers.data.Exercise
import com.cookandroid.challengers.data.ExerciseDao
import com.cookandroid.challengers.data.ExercisePlan
import com.cookandroid.challengers.data.ExercisePlanDao
import com.cookandroid.challengers.data.ExerciseSet
import com.cookandroid.challengers.data.ExerciseSetDao
import com.cookandroid.challengers.data.PlanDetail
import com.cookandroid.challengers.data.PlanDetailDao
import com.cookandroid.challengers.data.ChallengePersonal
import com.cookandroid.challengers.data.ChallengePersonalDao
import com.cookandroid.challengers.data.CoolDownStretch
import com.cookandroid.challengers.data.CoolDownStretchDao
import com.cookandroid.challengers.data.WeightRecord
import com.cookandroid.challengers.data.WeightRecordDao
import com.cookandroid.challengers.data.converters.ListConverter
import com.cookandroid.challengers.data.converters.LocalDateConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

@Database(
    entities = [ExercisePlan::class, Exercise::class, ExerciseSet::class, WeightRecord::class,
                PlanDetail::class, ChallengePersonal::class, CoolDownStretch::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(ListConverter::class, LocalDateConverter::class)

abstract class AppDatabase : RoomDatabase() {

    abstract fun coolDownStretchDao(): CoolDownStretchDao
    abstract fun exercisePlanDao(): ExercisePlanDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseSetDao(): ExerciseSetDao
    abstract fun planDetailDao(): PlanDetailDao
    abstract fun challengePersonalDao(): ChallengePersonalDao
    abstract fun weightRecordDao(): WeightRecordDao

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
                    populateInitialData(
                        database.exerciseDao(),
                        database.coolDownStretchDao(),
                        database.challengePersonalDao(),
                        database.exercisePlanDao(),
                        database.planDetailDao(),
                        database.exerciseSetDao(),
                        database.weightRecordDao()
                    )
                }
            }
        }

        private suspend fun populateInitialData(
            exerciseDao: ExerciseDao,
            coolDownStretchDao: CoolDownStretchDao,
            challengePersonalDao: ChallengePersonalDao,
            exercisePlanDao: ExercisePlanDao,
            planDetailDao: PlanDetailDao,
            exerciseSetDao: ExerciseSetDao,
            weightRecordDao: WeightRecordDao
        ) {
            // 개인챌린지 초기데이터
            val challenge1 = ChallengePersonal(name = "스쿼트 10번 하기", coinReward = 500, targetCount = 10, currentCount = 4)
            val challenge2 = ChallengePersonal(name = "런지 5회 하기", coinReward = 300, targetCount = 5, prerequisiteChallengeId = 1) // 스쿼트 10번 완료 후 활성화
            val challenge3 = ChallengePersonal(name = "푸시업 3번 하기", coinReward = 400, targetCount = 3)

            challengePersonalDao.insert(challenge1)
            challengePersonalDao.insert(challenge2)
            challengePersonalDao.insert(challenge3)

            val stretch1 = CoolDownStretch(name = "상체 스트레칭", imagePath = "upper_body_stretch", stOrder = 1)
            val stretch2 = CoolDownStretch(name = "암 써클링 어깨 스트레칭", imagePath = "arm_circling_shoulders", stOrder = 2)
            val stretch3 = CoolDownStretch(name = "라잉 햄스트링 스트레칭", imagePath = "lying_hamstring_stretch", stOrder = 3)

            coolDownStretchDao.insert(stretch1)
            coolDownStretchDao.insert(stretch2)
            coolDownStretchDao.insert(stretch3)

            val weightRecord1 = WeightRecord(date = LocalDate.of(2025, 5, 10), weight = 57.3, bodyFatPercentage = 14.5, skeletalMuscleMass = 23.4)
            val weightRecord2 = WeightRecord(date = LocalDate.of(2025, 5, 12), weight = 56.1, bodyFatPercentage = 14.2, skeletalMuscleMass = 23.6)

            weightRecordDao.insert(weightRecord1)
            weightRecordDao.insert(weightRecord2)

            // Exercise 초기 데이터 삽입
            val donkeyKickId = exerciseDao.insert( //
                Exercise(
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

            val lungeId = exerciseDao.insert( //
                Exercise(
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


            val squatId = exerciseDao.insert( //
                Exercise(
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


            val hipExtensionId = exerciseDao.insert( //
                Exercise(
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


            val sideLegRaiseId = exerciseDao.insert( //
                Exercise(
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


            val StepperId = exerciseDao.insert( //
                Exercise(
                    name = "스텝퍼",
                    part = "하체",
                    equip = "스텝박스",
                    imagePath = "stepper",
                    startPosition = listOf("스텝박스 앞에 선다."),
                    exerciseMotion = listOf(
                        "오른발을 박스 위에 올리고 체중을 실어 올라선다.",
                        "왼발을 이어 올린 뒤, 오른발부터 내려온다.",
                        "교대로 반복."
                    ),
                    mets = 4.8
                )
            )
            val legRaiseId = exerciseDao.insert(
                Exercise(
                    name = "레그레이즈",
                    part = "복근",
                    equip = "맨몸",
                    imagePath = "leg_raise",

                    // 시작 자세
                    startPosition = listOf(
                        "등을 대고 매트에 눕고, 양다리는 곧게 편다.",
                        "팔은 몸통 옆에 두어 손바닥으로 바닥을 가볍게 누르며 안정화한다.",
                        "허리가 과도하게 뜨지 않도록 복부에 힘을 주고 골반을 중립 위치로 유지한다."
                    ),

                    // 운동 동작
                    exerciseMotion = listOf(
                        "복근 힘으로 양다리를 천천히 들어 올려 약 70–90°까지 올린다.",
                        "정점에서 1초간 복근을 수축하며 유지한다.",
                        "같은 속도로 다리를 천천히 내려 시작 위치 바로 위(바닥에 닿지 않을 정도)까지 내린다.",
                        "반복 횟수 또는 시간(예: 12회 × 3세트, 혹은 30초)을 설정해 반복 수행한다."
                    ),

                    // 호흡법
                    breathing = listOf(
                        "다리를 올릴 때 숨을 내쉬고, 내릴 때 숨을 들이쉰다."
                    ),

                    // 주의 사항
                    caution = listOf(
                        "허리가 바닥에서 뜨지 않도록 복부에 힘을 지속적으로 유지한다.",
                        "다리를 내릴 때 반동을 사용하지 말고 천천히 제어한다.",
                        "목과 어깨에 힘이 과도하게 들어가지 않도록 이완한다."
                    ),

                    // METs (Compendium 기준: lie-supine leg lifts ≈ 3.8 METs → 반올림)
                    mets = 3.8
                )
            )


            val cycleId = exerciseDao.insert( //
                Exercise(
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


            val gluteBridgeId = exerciseDao.insert( //
                Exercise(
                    name = "글루트 브릿지",
                    part = "하체",
                    equip = "맨몸",
                    imagePath = "glute_bridge",
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

            val reverseLungeId = exerciseDao.insert(
                Exercise(
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

            val sideLungeId = exerciseDao.insert(
                Exercise(
                    name = "사이드 런지",
                    part = "하체",
                    equip = "맨몸",
                    imagePath = "side_lunge",
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
            val overheadLungeId = exerciseDao.insert(
                Exercise(
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
            val backSquatId = exerciseDao.insert(
                Exercise(
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
            val frontSquatId = exerciseDao.insert(
                Exercise(
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
            val sideBandWalkId = exerciseDao.insert(
                Exercise(
                    name = "사이드 밴드 워크",
                    part = "하체",
                    equip = "세라밴드",
                    imagePath = "side_band_walk",
                    startPosition = listOf("무릎 위·아래에 세라밴드를 감고 반쯤 앉는다."),
                    exerciseMotion = listOf("발을 옆으로 벌리며 밴드를 늘린다.", "천천히 반대발을 모으며 이동한다."),
                    breathing = listOf("옆으로 이동할 때 내쉬고, 모을 때 들이쉰다."),
                    caution = listOf("무릎이 안쪽으로 모이지 않게 한다."),
                    mets = 3.5
                )

            )
            val squatStepperId = exerciseDao.insert(
                Exercise(
                    name = "스쿼트 스텝퍼",
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
            val bandLegExtensionId = exerciseDao.insert(
                Exercise(
                    name = "밴드 레그 익스텐션",
                    part = "하체",
                    equip = "세라밴드",
                    imagePath = "band_leg_extension",
                    startPosition = listOf("세라밴드를 발목에 고정 후 의자에 앉는다."),
                    exerciseMotion = listOf("밴드를 저항으로 삼아 다리를 펴서 무릎을 완전히 뻗는다.", "천천히 굽혀 원위치한다."),
                    breathing = listOf("다리를 펴며 내쉬고, 굽히며 들이쉰다."),
                    caution = listOf("허리가 뒤로 굴곡되지 않도록 등받이에 기대지 않는다."),
                    mets = 3.8
                )

            )
            val sideStepperId = exerciseDao.insert(
                Exercise(
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
            val backLungeStepperId = exerciseDao.insert(
                Exercise(
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
            val plankLegLiftId = exerciseDao.insert(
                Exercise(
                    name = "플랭크 레그 리프트",
                    part = "하체, 복근",
                    equip = "맨몸",
                    imagePath = "plank_leg_lift",
                    startPosition = listOf("플랭크 기본 자세를 취한다."),
                    exerciseMotion = listOf(
                        "한쪽 다리를 곧게 펴서 천천히 들어 올린다.",
                        "엉덩이와 허리에 힘을 주며 1초 정지 후 내린다.",
                        "반대쪽 반복."
                    ),
                    breathing = listOf("다리를 올릴 때 내쉬고, 내릴 때 들이쉰다."),
                    caution = listOf("골반이 흔들리지 않도록 코어를 고정한다."),
                    mets = 4.2
                )

            )
            val widePushUpId = exerciseDao.insert(
                Exercise(
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
            val dumbbellBenchPressId = exerciseDao.insert(
                Exercise(
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
            val dumbbellSqueezePressId = exerciseDao.insert(
                Exercise(
                    name = "덤벨 스퀴즈 프레스",
                    part = "가슴",
                    equip = "덤벨",
                    imagePath = "dumbbell_squeeze_press",
                    startPosition = listOf("벤치에 누워 덤벨 두 개를 가슴 중앙에 붙인다."),
                    exerciseMotion = listOf("덤벨을 서로 밀어내며 위로 눌러 올린다.", "천천히 내리며 덤벨끼리 닿은 상태 유지."),
                    breathing = listOf("올릴 때 내쉬고, 내릴 때 들이쉰다."),
                    caution = listOf("손목을 곧게 유지한다."),
                    mets = 6.2
                )

            )
            val clapPushUpId = exerciseDao.insert(
                Exercise(
                    name = "클랩 푸쉬업",
                    part = "가슴, 팔",
                    equip = "맨몸",
                    imagePath = "clap_push_up",
                    startPosition = listOf("스탠다드 푸쉬업 자세를 취한다."),
                    exerciseMotion = listOf("폭발적으로 밀어 올려 공중에서 손뼉을 치고 착지."),
                    breathing = listOf("올라올 때 내쉬고, 착지 후 들이쉰다."),
                    caution = listOf("손목 충격 주의."),
                    mets = 6.5
                )

            )
            val declinePushUpId = exerciseDao.insert(
                Exercise(
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
            val inclinePushUpId = exerciseDao.insert(
                Exercise(
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
            val archerPushUpId = exerciseDao.insert(
                Exercise(
                    name = "아처 푸쉬업",
                    part = "가슴, 팔",
                    equip = "맨몸",
                    imagePath = "archer_push_up",
                    startPosition = listOf("손을 넓게 벌려 플랭크."),
                    exerciseMotion = listOf("한 팔을 굽혀 가슴을 내리며 반대팔은 곧게 편다.", "밀어 올리며 반대쪽 반복."),
                    breathing = listOf("내릴 때 들이쉬고, 올릴 때 내쉰다."),
                    caution = listOf("골반이 돌아가지 않도록."),
                    mets = 5.5
                )

            )
            val oneArmPushUpId = exerciseDao.insert(
                Exercise(
                    name = "원암 푸쉬업",
                    part = "가슴, 팔",
                    equip = "맨몸",
                    imagePath = "one_arm_push_up",
                    startPosition = listOf("발을 넓게 벌리고 한 손은 등 뒤에 둔다."),
                    exerciseMotion = listOf("한 팔로 몸을 지탱해 내려갔다 올라온다."),
                    breathing = listOf("내릴 때 들이쉬고, 올릴 때 내쉰다."),
                    caution = listOf("어깨 안정성 확보."),
                    mets = 7.0
                )

            )
            val frontSquatIdChest = exerciseDao.insert(
                Exercise(
                    name = "프론트 스쿼트",
                    part = "가슴, 하체",
                    equip = "맨몸",
                    imagePath = "front_squat",
                    startPosition = listOf("발 어깨너비, 팔을 교차해 어깨에 얹는다."),
                    exerciseMotion = listOf("엉덩이를 뒤로 빼지 않고 무릎을 굽혀 내려갔다 올라온다."),
                    breathing = listOf("내릴 때 들이쉬고, 올릴 때 내쉰다."),
                    caution = listOf("상체가 앞으로 기울지 않도록."),
                    mets = 4.8
                )

            )

            val standingChestPressId = exerciseDao.insert(
                Exercise(
                    name = "스탠딩 체스트 프레스",
                    part = "가슴",
                    equip = "세라밴드",
                    imagePath = "standing_chest_press",
                    startPosition = listOf("밴드를 등 뒤 기둥에 고정 후 손잡이를 잡는다."),
                    exerciseMotion = listOf("팔꿈치를 90°로 굽히고 전방으로 밀어 가슴을 수축.", "천천히 다시 굽혀 원위치."),
                    breathing = listOf("밀 때 내쉬고, 돌아올 때 들이쉰다."),
                    caution = listOf("어깨가 들리지 않도록."),
                    mets = 4.2
                )

            )
            val chestFlyId = exerciseDao.insert(
                Exercise(
                    name = "체스트 플라이",
                    part = "가슴",
                    equip = "세라밴드",
                    imagePath = "chest_fly",
                    startPosition = listOf("밴드 양끝을 잡고 팔을 앞에 모은다."),
                    exerciseMotion = listOf("팔을 양옆으로 벌려 가슴을 늘린 뒤 모으며 수축."),
                    breathing = listOf("모을 때 내쉬고, 벌릴 때 들이쉰다."),
                    caution = listOf("팔꿈치를 너무 펴지 않는다."),
                    mets = 4.0
                )

            )
            val bandPushUpId = exerciseDao.insert(
                Exercise(
                    name = "밴드 푸쉬업",
                    part = "가슴, 팔",
                    equip = "세라밴드",
                    imagePath = "band_push_up",
                    startPosition = listOf("세라밴드를 등 뒤에 걸고 푸쉬업 자세."),
                    exerciseMotion = listOf("밴드 저항을 이기며 팔을 굽혀 내려갔다 밀어 올린다."),
                    breathing = listOf("내릴 때 들이쉬고, 올릴 때 내쉰다."),
                    caution = listOf("코어 고정."),
                    mets = 5.2
                )

            )
            val dumbbellFlyId = exerciseDao.insert(
                Exercise(
                    name = "덤벨 플라이",
                    part = "가슴",
                    equip = "덤벨",
                    imagePath = "dumbbell_fly",
                    startPosition = listOf("벤치에 누워 팔을 약간 굽혀 덤벨을 머리 위로 든다."),
                    exerciseMotion = listOf("팔을 옆으로 벌려 가슴을 늘리고 천천히 모으며 수축."),
                    breathing = listOf("모을 때 내쉬고, 벌릴 때 들이쉰다."),
                    caution = listOf("어깨 과신전 주의."),
                    mets = 5.8
                )

            )
            val wideArmPlankId = exerciseDao.insert(
                Exercise(
                    name = "와이드 암 플랭크",
                    part = "가슴, 어깨, 복근",
                    equip = "맨몸",
                    imagePath = "wide_arm_plank",
                    startPosition = listOf("손 간격을 넓게 두고 플랭크 자세."),
                    exerciseMotion = listOf("코어와 가슴에 힘을 주며 자세 유지."),
                    caution = listOf("허리가 처지지 않도록 한다."),
                    mets = 3.5
                )
            )

            val plankId = exerciseDao.insert(
                Exercise(
                    name = "플랭크",
                    part = "복근, 어깨",
                    equip = "맨몸",
                    imagePath = "plank",
                    startPosition = listOf(
                        "팔꿈치를 어깨 바로 아래에 두고, 전완 전체를 바닥에 댄다.",
                        "다리를 똑바로 뻗어 발끝(발가락)으로 지지한다.",
                        "머리부터 발끝까지 몸을 일직선으로 유지한다."
                    ),

                    exerciseMotion = listOf(
                        "복부와 엉덩이에 힘을 주어 허리가 처지지 않도록 버틴다.",
                        "목은 중립을 유지하고 시선은 바닥을 향하게 한다.",
                        "설정한 시간(예: 30초~60초) 동안 자세를 유지한다."
                    ),
                    caution = listOf(
                        "허리가 과도하게 휘거나 엉덩이가 들리지 않도록 코어에 힘을 유지한다.",
                        "어깨가 귀 쪽으로 올라가지 않도록 견갑골을 안정화한다.",
                        "호흡을 참지 말고 일정한 리듬으로 자연 호흡한다."
                    ),
                    mets = 3.3
                )
            )

            val pushUpId = exerciseDao.insert(
                Exercise(
                    name = "푸쉬업",
                    part = "가슴, 팔",
                    equip = "맨몸",
                    imagePath = "push_up",
                    startPosition = listOf("어깨너비 손으로 플랭크."),
                    exerciseMotion = listOf("가슴을 바닥으로 내렸다가 밀어 올린다."),
                    breathing = listOf("내릴 때 들이쉬고, 올릴 때 내쉰다."),
                    caution = listOf("팔꿈치가 과도하게 벌어지지 않도록."),
                    mets = 4.0
                )
            )
            val pullUpShoulderId = exerciseDao.insert(
                Exercise(
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
            val bentOverLateralRaiseId = exerciseDao.insert(
                Exercise(
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
            val dumbbellShoulderPressId = exerciseDao.insert(
                Exercise(
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
            val lateralRaiseId = exerciseDao.insert(
                Exercise(
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
            val frontRaiseId = exerciseDao.insert(
                Exercise(
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
            val sideLateralRaiseId = exerciseDao.insert(
                Exercise(
                    name = "사이드 레터럴 레이즈",
                    part = "어깨",
                    equip = "맨몸",
                    imagePath = "side_lateral_raise",
                    startPosition = listOf("양팔을 몸 옆으로 내려뜨린다."),
                    exerciseMotion = listOf(
                        "팔꿈치를 10° 굽힌 상태로 팔을 양옆으로 들어 올린다.",
                        "어깨 높이에서 1초 유지 후 내린다."
                    ),
                    breathing = listOf("올릴 때 내쉬고, 내릴 때 들이쉬기."),
                    caution = listOf("팔이 몸 뒤로 가지 않도록."),
                    mets = 3.8
                )

            )
            val armWalkingShoulderId = exerciseDao.insert(
                Exercise(
                    name = "암워킹",
                    part = "어깨, 복근",
                    equip = "맨몸",
                    imagePath = "arm_walking",
                    startPosition = listOf("다리를 곧게 펴고 손으로 바닥을 짚는다."),
                    exerciseMotion = listOf("손으로 앞으로 걸어 플랭크 자세 만들어 유지.", "손을 되짚어 다시 서서 시작."),
                    breathing = listOf("플랭크 도달 시 내쉬고, 일어서며 들이쉬기."),
                    caution = listOf("허리가 처지지 않도록."),
                    mets = 4.5
                )

            )
            val shoulderTapId = exerciseDao.insert(
                Exercise(
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
            val yRaiseId = exerciseDao.insert(
                Exercise(
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
            val bandFrontRaiseId = exerciseDao.insert(
                Exercise(
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
            val bandLateralRaiseId = exerciseDao.insert(
                Exercise(
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
            val facePullId = exerciseDao.insert(
                Exercise(
                    name = "페이스 풀",
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
            val dumbbellOverheadPressId = exerciseDao.insert(
                Exercise(
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
            val kettlebellShoulderPressId = exerciseDao.insert(
                Exercise(
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
            val singleKBSPressId = exerciseDao.insert(
                Exercise(
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
            val shoulderTapPlankId = exerciseDao.insert(
                Exercise(
                    name = "숄더 탭 플랭크",
                    part = "어깨, 복근",
                    equip = "맨몸",
                    imagePath = "shoulder_tap_plank",
                    startPosition = listOf("플랭크 자세에서 시작."),
                    exerciseMotion = listOf("한 손으로 반대 어깨를 터치, 교대 반복."),
                    breathing = listOf("터치 시 내쉬고, 손 내릴 때 들이쉬기."),
                    caution = listOf("골반 흔들림 최소화."),
                    mets = 4.1
                )

            )
            val plankArmRaiseId = exerciseDao.insert(
                Exercise(
                    name = "플랭크 + 암 레이즈",
                    part = "어깨, 복근",
                    equip = "맨몸",
                    imagePath = "plank_arm_raise",
                    startPosition = listOf("플랭크 자세."),
                    exerciseMotion = listOf("한 팔을 앞으로 들어 올려 1초 유지 후 내린다.", "반대 팔 반복."),
                    breathing = listOf("올릴 때 내쉬고, 내릴 때 들이쉬기."),
                    caution = listOf("허리가 처지지 않도록."),
                    mets = 4.0
                )

            )
            val dolphinPlankId = exerciseDao.insert(
                Exercise(
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
            val shoulderPressStepId = exerciseDao.insert(
                Exercise(
                    name = "숄더 프레스",
                    part = "어깨, 팔",
                    equip = "덤벨",
                    imagePath = "shoulder_press_step",
                    startPosition = listOf("앉거나 서서 덤벨을 어깨 높이로 든다."),
                    exerciseMotion = listOf("팔을 위로 밀어 덤벨을 머리 위로 올리고 내려온다."),
                    breathing = listOf("밀 때 내쉬고, 내릴 때 들이쉬기."),
                    caution = listOf("균형 유지 주의."),
                    mets = 5.8
                )

            )
            val plankTwistId = exerciseDao.insert(
                Exercise(
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
            val bicepCurlId = exerciseDao.insert(
                Exercise(
                    name = "이두 컬",
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
            val hammerCurlId = exerciseDao.insert(
                Exercise(
                    name = "해머 컬",
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
            val dumbbellKickbackId = exerciseDao.insert(
                Exercise(
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
            val reverseCurlId = exerciseDao.insert(
                Exercise(
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
            val armWalkingId = exerciseDao.insert(
                Exercise(
                    name = "암워킹",
                    part = "팔, 어깨, 복근",
                    equip = "맨몸",
                    imagePath = "arm_walking",
                    startPosition = listOf("선 자세에서 허리를 굽혀 손으로 바닥을 짚는다."),
                    exerciseMotion = listOf("손으로 앞으로 걸어 플랭크를 만든 후 손을 되짚어 일어선다."),
                    breathing = listOf("플랭크 도달 시 내쉬고, 일어설 때 들이쉬기."),
                    caution = listOf("허리가 처지지 않도록 코어 유지."),
                    mets = 4.5
                )

            )
            val dipsPushUpId = exerciseDao.insert(
                Exercise(
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
            val diamondPushUpId = exerciseDao.insert(
                Exercise(
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
            val closeGripPushUpId = exerciseDao.insert(
                Exercise(
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
            val gripTrainerId = exerciseDao.insert(
                Exercise(
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
            val bandTricepKickbackId = exerciseDao.insert(
                Exercise(
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
            val bandOverheadTricepsId = exerciseDao.insert(
                Exercise(
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
            val upDownPlankId = exerciseDao.insert(
                Exercise(
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
            val armLiftPlankId = exerciseDao.insert(
                Exercise(
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
            val reversePlankArmKickbackId = exerciseDao.insert(
                Exercise(
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
                    name = "딥스",
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
                    name = "트라이셉스 익스텐션",
                    part = "팔",
                    equip = "짐볼",
                    imagePath = "gymball_triceps_extension",
                    startPosition = listOf("짐볼 위에 등을 대고 누워 덤벨을 머리 위로 든다."),
                    exerciseMotion = listOf("팔꿈치를 굽혀 덤벨을 머리 뒤로 내렸다가 팔을 펴 올린다."),
                    breathing = listOf("펴며 내쉬고, 굽히며 들이쉬기."),
                    caution = listOf("팔꿈치가 퍼지지 않도록."),
                    mets = 5.7
                )

            )
            val bentOverRowBandArmId = exerciseDao.insert(
                Exercise(
                    name = "벤트오버 로우",
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
            val gobletSquatIdArm = exerciseDao.insert(
                Exercise(
                    name = "덤벨 고블릿 스쿼트",
                    part = "팔, 하체",
                    equip = "덤벨",
                    imagePath = "dumbbell_goblet_squat",
                    startPosition = listOf("가슴 앞에 덤벨을 세로로 잡고 선다."),
                    exerciseMotion = listOf("엉덩이를 뒤로 빼며 스쿼트, 하부에서 팔로 덤벨을 지탱.", "발로 밀어 올라온다."),
                    breathing = listOf("내릴 때 들이쉬고, 올라올 때 내쉬기."),
                    caution = listOf("팔꿈치가 무릎 안쪽을 따라 내려가도록한다."),
                    mets = 4.6
                )

            )
            val spotJumpId = exerciseDao.insert(
                Exercise(
                    name = "제자리뛰기",
                    part = "유산소",
                    equip = "맨몸",
                    imagePath = "spot_jump",
                    startPosition = listOf("어깨너비로 서서 무릎을 살짝 굽힌다."),
                    exerciseMotion = listOf("팔을 흔들며 가볍게 제자리에서 점프를 반복한다."),
                    breathing = listOf("점프할 때 숨을 내쉬고, 착지하며 들이쉰다."),
                    caution = listOf("무릎 충격을 완화하기 위해 부드럽게 착지한다."),
                    mets = 5.0
                )

            )
            val walkId = exerciseDao.insert(
                Exercise(
                    name = "걷기",
                    part = "유산소",
                    equip = "맨몸",
                    imagePath = "walking",
                    startPosition = listOf("등을 곧게 펴고 자연스럽게 선다."),
                    exerciseMotion = listOf("팔을 자연스럽게 흔들며 일정 속도로 걷는다."),
                    breathing = listOf("규칙적인 호흡 유지."),
                    caution = listOf("발뒤꿈치에서 발끝 순서로 착지한다."),
                    mets = 3.0
                )

            )
            val danceId = exerciseDao.insert(
                Exercise(
                    name = "댄스",
                    part = "유산소",
                    equip = "맨몸",
                    imagePath = "dance",
                    startPosition = listOf("편안한 균형 자세로 선다."),
                    exerciseMotion = listOf("음악에 맞춰 전신을 리드미컬하게 움직인다."),
                    breathing = listOf("리듬에 맞춰 호흡한다."),
                    caution = listOf("무리한 동작은 피한다."),
                    mets = 6.0
                )

            )
            val hulaHoopId = exerciseDao.insert(
                Exercise(
                    name = "훌라우프",
                    part = "유산소",
                    equip = "맨몸",
                    imagePath = "hula_hoop",
                    startPosition = listOf("발을 어깨너비로 벌리고 훌라우프를 허리에 둔다."),
                    exerciseMotion = listOf("허리를 원형으로 회전해 훌라우프를 지속적으로 돌린다."),
                    breathing = listOf("자연 호흡 유지."),
                    caution = listOf("허리 과사용 주의."),
                    mets = 4.5
                )

            )
            val stepBoxBasicId = exerciseDao.insert(
                Exercise(
                    name = "스텝박스",
                    part = "유산소, 하체",
                    equip = "스텝박스",
                    imagePath = "step_box",
                    startPosition = listOf("스텝박스 앞에 선다."),
                    exerciseMotion = listOf("오른발 → 왼발 순으로 박스 위에 오르고, 반대 순서로 내려온다.", "교대로 반복."),
                    breathing = listOf("오를 때 내쉬고, 내려올 때 들이쉰다."),
                    caution = listOf("발 전체를 올려 안정성을 확보한다."),
                    mets = 5.0
                )

            )
            val joggingId = exerciseDao.insert(
                Exercise(
                    name = "조깅",
                    part = "유산소",
                    equip = "맨몸",
                    imagePath = "jogging",
                    startPosition = listOf("등을 펴고 가슴을 열고 선다."),
                    exerciseMotion = listOf("편안한 속도로 일정 거리 또는 시간 동안 달린다."),
                    breathing = listOf("코로 들이쉬고 입으로 내쉰다."),
                    caution = listOf("착지 시 발볼로 부드럽게 충격 흡수."),
                    mets = 7.0
                )

            )
            val jumpRopeId = exerciseDao.insert(
                Exercise(
                    name = "줄넘기",
                    part = "유산소",
                    equip = "맨몸",
                    imagePath = "jump_rope",
                    startPosition = listOf("줄을 뒤로 두고 손잡이를 잡아 선다."),
                    exerciseMotion = listOf("손목을 돌려 줄을 넘기며 가볍게 점프한다."),
                    breathing = listOf("점프 시 내쉬고 착지하며 들이쉰다."),
                    caution = listOf("무릎에 무리 가지 않도록 낮은 점프."),
                    mets = 8.0
                )

            )
            val jumpingJackId = exerciseDao.insert(
                Exercise(
                    name = "점핑 잭",
                    part = "유산소",
                    equip = "맨몸",
                    imagePath = "jumping_jack",
                    startPosition = listOf("발을 모으고 팔을 몸 옆에 둔다."),
                    exerciseMotion = listOf("점프하며 발을 벌리고 팔을 머리 위로 올린다.", "다시 점프해 원위치."),
                    breathing = listOf("점프 시 내쉬고, 착지 시 들이쉰다."),
                    caution = listOf("무릎 부드럽게 착지."),
                    mets = 5.5
                )

            )
            val slowBurpeeId = exerciseDao.insert(
                Exercise(
                    name = "슬로우버피",
                    part = "유산소, 전신",
                    equip = "맨몸",
                    imagePath = "slow_burpee",
                    startPosition = listOf("선 자세에서 시작."),
                    exerciseMotion = listOf("스쿼트 → 플랭크 → 푸쉬업 → 스쿼트 → 점프를 천천히 수행."),
                    breathing = listOf("동작 전환마다 자연 호흡."),
                    caution = listOf("무릎과 허리에 과부하 주의."),
                    mets = 6.0
                )

            )
            val bandCardioId = exerciseDao.insert(
                Exercise(
                    name = "세라밴드",
                    part = "유산소",
                    equip = "세라밴드",
                    imagePath = "band_cardio",
                    startPosition = listOf("밴드를 발에 고정하고 손잡이를 잡는다."),
                    exerciseMotion = listOf("밴드를 당기며 제자리 스쿼트·프레스 등 동작을 연속 수행."),
                    breathing = listOf("동작 리듬에 맞춰 호흡."),
                    caution = listOf("밴드가 끊어지지 않도록 상태 확인."),
                    mets = 4.8
                )

            )
            val gymballExerciseId = exerciseDao.insert(
                Exercise(
                    name = "짐볼운동",
                    part = "유산소",
                    equip = "짐볼",
                    imagePath = "gymball_exercise",
                    startPosition = listOf("짐볼에 앉아 균형 잡는다."),
                    exerciseMotion = listOf("볼 위에서 점핑·러닝 모션을 반복한다."),
                    breathing = listOf("자연 호흡 유지."),
                    caution = listOf("짐볼 미끄럼 주의."),
                    mets = 4.5
                )

            )
            val ptDrillId = exerciseDao.insert(
                Exercise(
                    name = "PT체조",
                    part = "유산소",
                    equip = "맨몸",
                    imagePath = "pt_drill",
                    startPosition = listOf("정렬자세로 선다."),
                    exerciseMotion = listOf("팔벌려뛰기·런지·스쿼트 등 체조 동작을 구령에 맞춰 연속 수행."),
                    breathing = listOf("구령에 맞춰 호흡."),
                    caution = listOf("반동 과다 사용 주의."),
                    mets = 6.5
                )

            )
            val kettlebellSwingId = exerciseDao.insert(
                Exercise(
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
            val burpeeId = exerciseDao.insert(
                Exercise(
                    name = "버피",
                    part = "유산소, 전신",
                    equip = "맨몸",
                    imagePath = "burpee",
                    startPosition = listOf("선 자세에서 시작."),
                    exerciseMotion = listOf("스쿼트 → 플랭크 → 푸쉬업 → 점프를 연속 수행."),
                    breathing = listOf("점프할 때 내쉬고, 플랭크로 내려갈 때 들이쉰다."),
                    caution = listOf("반동 대신 전신 근육 사용."),
                    mets = 7.5
                )

            )
            val bicycleCrunchCardioId = exerciseDao.insert(
                Exercise(
                    name = "하늘 자전거",
                    part = "유산소, 복근",
                    equip = "맨몸",
                    imagePath = "bicycle_crunch",
                    startPosition = listOf("등을 대고 누워 무릎 90°, 손은 머리 뒤."),
                    exerciseMotion = listOf("무릎-팔꿈치 교차 자전거 동작을 빠르게 반복."),
                    breathing = listOf("회전 시 내쉬고, 반대에서 들이쉬기."),
                    caution = listOf("허리 뜨지 않도록."),
                    mets = 5.2
                )

            )
            val mountainClimberCardioId = exerciseDao.insert(
                Exercise(
                    name = "마운틴 클라이머",
                    part = "유산소, 복근",
                    equip = "맨몸",
                    imagePath = "mountain_climber",
                    startPosition = listOf("푸쉬업 자세에서 시작."),
                    exerciseMotion = listOf("무릎을 번갈아 가슴 쪽으로 빠르게 당긴다."),
                    breathing = listOf("리듬 호흡 유지."),
                    caution = listOf("골반 위치 유지."),
                    mets = 5.8
                )

            )
            val splitJumpBoxId = exerciseDao.insert(
                Exercise(
                    name = "스플릿 점프 박스",
                    part = "유산소, 하체",
                    equip = "스텝박스",
                    imagePath = "split_jump_box",
                    startPosition = listOf("스텝박스 앞에 런지 자세."),
                    exerciseMotion = listOf("발을 교대 점프하여 다른 쪽 발을 박스 위에 올린다."),
                    breathing = listOf("점프 시 내쉬고, 착지 시 들이쉰다."),
                    caution = listOf("무릎 충격 완화."),
                    mets = 6.2
                )

            )
            val stepAerobicId = exerciseDao.insert(
                Exercise(
                    name = "스텝 에어로빅",
                    part = "유산소",
                    equip = "스텝박스",
                    imagePath = "step_aerobic",
                    startPosition = listOf("스텝박스 앞에 선다."),
                    exerciseMotion = listOf("음악 리듬에 맞춰 다양한 스텝 패턴 수행."),
                    breathing = listOf("리듬 호흡."),
                    caution = listOf("발 전체를 박스에 올려 안정성 확보."),
                    mets = 5.5
                )

            )
            val stepBoxBurpeeId = exerciseDao.insert(
                Exercise(
                    name = "스텝박스 버피",
                    part = "유산소, 전신",
                    equip = "스텝박스",
                    imagePath = "step_box_burpee",
                    startPosition = listOf("스텝박스 앞에 서서 시작."),
                    exerciseMotion = listOf("버피 동작 중 점프 단계에서 박스 위로 점프하여 착지."),
                    breathing = listOf("점프 시 내쉬고, 내려갈 때 들이쉰다."),
                    caution = listOf("박스 모서리 안전 주의."),
                    mets = 7.8
                )

            )
            val gymballJumpId = exerciseDao.insert(
                Exercise(
                    name = "짐볼 점핑",
                    part = "유산소",
                    equip = "짐볼",
                    imagePath = "gymball_jump",
                    startPosition = listOf("짐볼 위에 앉아 균형 유지."),
                    exerciseMotion = listOf("볼 위에서 연속으로 점프하며 유산소 운동."),
                    breathing = listOf("점프 시 내쉬고, 착지 시 들이쉰다."),
                    caution = listOf("짐볼 미끄럼 및 파손 주의."),
                    mets = 4.9
                )
            )
            // ExercisePlan 초기 데이터 삽입
            val planAId =
                exercisePlanDao.insert(ExercisePlan(plannedDate = System.currentTimeMillis()))
//            val planBId = exercisePlanDao.insert(ExercisePlan(plannedDate = System.currentTimeMillis() + 86400000)) // 다음 날

            // PlanDetail 초기 데이터 삽입 (운동 계획과 운동 연결)
            planDetailDao.insert(
                PlanDetail(
                    exercisePlanId = planAId,
                    exerciseId = squatId,
                    exOrder = 1,
                    sets = 3,
                    reps = 12
                )
            )
            planDetailDao.insert(
                PlanDetail(
                    exercisePlanId = planAId,
                    exerciseId = pushUpId,
                    exOrder = 2,
                    sets = 3,
                    reps = 10
                )
            )
            planDetailDao.insert(
                PlanDetail(
                    exercisePlanId = planAId,
                    exerciseId = lungeId,
                    exOrder = 3,
                    sets = 1,
                    reps = 60
                )
            )

            // ExerciseSet 초기 데이터 삽입 (각 운동 계획 내 운동의 세트)
            // Plan A - 스쿼트
            exerciseSetDao.insert(
                ExerciseSet(
                    exerciseId = squatId,
                    setNumber = 1,
                    weight = 0,
                    reps = 15
                )
            )
            exerciseSetDao.insert(
                ExerciseSet(
                    exerciseId = squatId,
                    setNumber = 2,
                    weight = 0,
                    reps = 15
                )
            )
            exerciseSetDao.insert(
                ExerciseSet(
                    exerciseId = squatId,
                    setNumber = 3,
                    weight = 0,
                    reps = 10
                )
            )
            // Plan A - 푸쉬업
            exerciseSetDao.insert(
                ExerciseSet(
                    exerciseId = pushUpId,
                    setNumber = 1,
                    weight = 0,
                    reps = 15
                )
            )
            exerciseSetDao.insert(
                ExerciseSet(
                    exerciseId = pushUpId,
                    setNumber = 2,
                    weight = 0,
                    reps = 10
                )
            )
            exerciseSetDao.insert(
                ExerciseSet(
                    exerciseId = pushUpId,
                    setNumber = 3,
                    weight = 0,
                    reps = 10
                )
            )
            // Plan A - 런지
            exerciseSetDao.insert(
                ExerciseSet(
                    exerciseId = lungeId,
                    setNumber = 1,
                    weight = 0,
                    reps = 60
                )
            )
        }
    }
}