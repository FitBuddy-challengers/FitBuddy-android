package com.cookandroid.challengers

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.cookandroid.challengers.data.CoolDownStretch
import com.cookandroid.challengers.data.ExercisePlan
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.data.ExerciseSet
import com.cookandroid.challengers.data.ExerciseSetEntity
import com.cookandroid.challengers.data.PlanDetail
import com.cookandroid.challengers.network.dto.DummyPlanRequest
import com.cookandroid.challengers.network.dto.DummyPlanResponse
import kotlinx.coroutines.Dispatchers

import java.time.format.DateTimeFormatter

import com.cookandroid.challengers.network.dto.PlanDto
import com.cookandroid.challengers.network.dto.ScheduleDto
import com.cookandroid.challengers.network.dto.TodayPlanResponse
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        // 최초 계획이 없으면 하나 생성
//        val db = AppDatabase.getDatabase(this, lifecycleScope)
//        lifecycleScope.launch {
//            val planDao = db.exercisePlanDao()
//
//            val zoneId = ZoneId.of("Asia/Seoul")
//
//            val todayStart = LocalDate.now(zoneId)
//                .atStartOfDay(zoneId)
//                .toInstant()
//                .toEpochMilli()
//
//            val todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1  // 오늘 끝
//            val todayPlans = planDao.getPlansByDate(todayStart, todayEnd)
//
//            if (todayPlans.isEmpty()) {
//                val insertedId = planDao.insert(ExercisePlan(plannedDate = todayStart))
//
//                if (insertedId == 0L) {
//                    // 이론상 도달하지 않아야 하지만, insert 시점에 race condition 방지
//                    android.util.Log.w("MainActivity", "❗ 이미 오늘 날짜의 plan이 존재 (insert skipped)")
//                } else {
//                    android.util.Log.i("MainActivity", "✅ 오늘 날짜의 plan 생성됨 (id=$insertedId)")
//                }
//            } else {
//                android.util.Log.i("MainActivity", "✅ 오늘 날짜의 plan 이미 존재 (id=${todayPlans.first().id})")
//            }
//        }
        //-> 이제 roomDB가 아닌 실제 데이터베이스로 저장! 화면에는 지장없으니 걱정하지 말아요!(05.22 채윤지 수정)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Navigation Component 설정
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        navController = navHostFragment.navController
        navController.setGraph(R.navigation.nav_graph_main)
        binding.mainBnv.setupWithNavController(navController)

        // 로그인 여부에 따라 초기 노출
        val isLoggedIn = intent.getBooleanExtra("isLoggedIn", false)
        if (savedInstanceState == null) {
            if (isLoggedIn) {
                navController.navigate(R.id.homeFragment)
                binding.mainBnv.visibility = View.VISIBLE

                // Step 1: 서버 → Room 동기화 시도
                syncWithServerDatabase()

                // Step 2: 동기화 후 더미 없으면 생성
                createDummyPlanOnServer()
            } else {
                binding.mainBnv.visibility = View.GONE
            }
        }

        // 프래그먼트 교체될 때마다 BottomNav 보여줄지 말지 결정
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                    super.onFragmentResumed(fm, f)
                    when (f) {
                        is com.cookandroid.challengers.auth.login.LoginFragment,
                        is com.cookandroid.challengers.auth.login.LoginEmailFragment,
                        is com.cookandroid.challengers.auth.signup.SignUpEmailFragment,
                        is com.cookandroid.challengers.auth.signup.SignUpPasswordFragment,
                        is com.cookandroid.challengers.auth.signup.SignUpOtpFragment,
                        is com.cookandroid.challengers.auth.signup.SignUpDoneFragment,
                        is ExerciseListFragment,
                        is ExerciseDoingFragment,
                        is ExerciseDetailFragment,
                        is ExerciseAddFragment,
                        is ExerciseEditSetFragment,
                        is RestTimerFragment,
                        is CoolDownStretchFragment,
                        is RecordAddWeightFragment -> {
                            // 다이얼로그로 띄우는 Fragment 들
                            binding.mainBnv.visibility = View.GONE
                        }

                        else -> {
                            binding.mainBnv.visibility = View.VISIBLE
                        }
                    }
                }
            }, true
        )
    }

    private fun createDummyPlanOnServer() {
        val userId = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getInt("userId", -1)

        if (userId == -1) {
            Log.e("DummyPlan", "유저 ID 없음 - 더미 생성 건너뜀")
            return
        }

        val date = LocalDate.now().toString() // YYYY-MM-DD

        val request = DummyPlanRequest(userId = userId, date = date)


        //서버 시준으로 플랜이 없는 경우, 사용자의 편의성을 위해 더미 플랜을 생성
        RetrofitClient.exerciseApi.createDummyPlan(request)
            .enqueue(object : Callback<DummyPlanResponse> {
                override fun onResponse(
                    call: Call<DummyPlanResponse>,
                    response: Response<DummyPlanResponse>
                ) {
                    if (response.isSuccessful) {
                        val planId = response.body()?.planId
                        val dateStr = response.body()?.date

                        // 날짜 파싱 추가
                        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        val date = dateStr?.let { LocalDate.parse(it, dateFormatter) }

                        Log.d("DummyPlan", "✅ 더미 운동 계획 생성됨 - planId: $planId, date: $date")
                    } else {
                        Log.w("DummyPlan", "⚠️ 서버 응답 오류: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<DummyPlanResponse>, t: Throwable) {
                    Log.e("DummyPlan", "❌ 서버 요청 실패: ${t.message}")
                }
            })
    }


    fun showBottomNav() {
        binding.mainBnv.visibility = View.VISIBLE
    }


    //Room DB 보완! 앱 실행시 서버 DB를 기준으로 room을 구성함.
    private fun syncWithServerDatabase() {
        val db = AppDatabase.getDatabase(this@MainActivity, lifecycleScope)
        val userId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getInt("userId", -1)
        if (userId == -1) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.scheduleApi.getTodayPlan(userId)
                if (response.isSuccessful) {
                    val todayPlan = response.body()

                    if (todayPlan != null) {
                        // ✅ 1. 실제 플랜 존재 → Room 초기화 및 저장
                        val planId = todayPlan.plan.id.toLong()

                        db.exercisePlanDao().deleteAll()
                        db.planDetailDao().deleteAll()
                        db.exerciseSetDao().deleteAll()

                        val dateMillis = LocalDate.parse(todayPlan.plan.start_date)
                            .atStartOfDay(ZoneId.of("Asia/Seoul"))
                            .toInstant()
                            .toEpochMilli()

                        db.exercisePlanDao().insert(
                            ExercisePlan(id = planId, plannedDate = dateMillis)
                        )

                        // 추가: planId를 SharedPreferences에 저장
                        getSharedPreferences("UserPrefs", MODE_PRIVATE)
                            .edit()
                            .putLong("todayPlanId", planId)
                            .apply()


                        for ((index, sched) in todayPlan.schedules.withIndex()) {
                            val exerciseId = sched.exercise_id.toLong()
                            val scheduleId = sched.id.toLong()

                            // ✅ PlanDetail 저장 (Room 화면용)
                            val planDetail = PlanDetail(
                                exercisePlanId = planId,
                                exerciseId = exerciseId,
                                exOrder = index + 1
                            )
                            db.planDetailDao().insert(planDetail)

                            // ✅ ExerciseSet 저장 (Room 세트용)
                            for (setNumber in 1..3) {
                                val exerciseSet = ExerciseSet(
                                    exercisePlanId = planId,
                                    exerciseId = exerciseId,
                                    setNumber = setNumber,
                                    weight = 0,
                                    reps = 12,
                                    isCompleted = false,
                                    isHighlighted = (setNumber == 1 && index == 0)
                                )
                                db.exerciseSetDao().insert(exerciseSet)

                                // ✅ 서버 DTO 로그 출력
                                val serverSet = ExerciseSetEntity(
                                    scheduleId = scheduleId,
                                    exerciseId = exerciseId,
                                    setNumber = setNumber,
                                    reps = 12,
                                    weight = 0
                                )
                                Log.d("MainActivity", "서버 세트 DTO 생성: $serverSet")
                            }
                        }



                    } else {
                        // ✅ 2. 오늘 플랜 없음 → 더미 생성
                        withContext(Dispatchers.Main) {
                            createDummyPlanOnServer()
                        }
                    }
                } else {
                    Log.e("SyncRoom", "서버 응답 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SyncRoom", "서버 동기화 실패: ${e.message}")
            }
        }
    }
}
