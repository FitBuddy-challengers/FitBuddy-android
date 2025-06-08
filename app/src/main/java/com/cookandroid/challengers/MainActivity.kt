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
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.ActivityMainBinding
import com.cookandroid.challengers.network.dto.DummyPlanRequest
import com.cookandroid.challengers.network.dto.DummyPlanResponse
import com.cookandroid.challengers.util.UserPreference // UserPreference 임포트
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.navigation.NavOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var userPreference: UserPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userPreference = UserPreference(this)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        navController = navHostFragment.navController
        navController.setGraph(R.navigation.nav_graph_main)
        //binding.mainBnv.setupWithNavController(navController)
        binding.mainBnv.setOnItemSelectedListener { item ->
            val currentDest = navController.currentDestination?.id
            when (item.itemId) {
                R.id.homeFragment -> {
                    if (currentDest != R.id.homeFragment) {
                        navController.navigate(
                            R.id.homeFragment,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph_main, false)
                                .setLaunchSingleTop(true)
                                .build()
                        )
                    }
                    true
                }
                R.id.exerciseFragment -> {
                    if (currentDest != R.id.exerciseFragment) {
                        navController.navigate(
                            R.id.exerciseFragment,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph_main, false)
                                .setLaunchSingleTop(true)
                                .build()
                        )
                    }
                    true
                }
                R.id.challengeFragment -> {
                    if (currentDest != R.id.challengeFragment) {
                        navController.navigate(R.id.challengeFragment)
                    }
                    true
                }
                R.id.recordFragment -> {
                    if (currentDest != R.id.recordFragment) {
                        navController.navigate(R.id.recordFragment)
                    }
                    true
                }
                R.id.storeFragment -> {
                    if (currentDest != R.id.storeFragment) {
                        navController.navigate(R.id.storeFragment)
                    }
                    true
                }
                else -> false
            }
        }

        // 1. Intent에서 userId를 먼저 확인 (LoginActivity에서 직접 로그인 성공 시 전달)
        val userIdFromIntent = intent.getIntExtra("userId", -1)
        val isLoggedInFromIntent = intent.getBooleanExtra("isLoggedIn", false)

        // 2. Intent에 userId가 없으면 SharedPreferences에서 확인 (자동 로그인 시도)
        val userIdFromPrefs = userPreference.getUserId()
        var finalUserId = -1 // 최종적으로 사용할 userId

        if (isLoggedInFromIntent && userIdFromIntent != -1) {
            // LoginActivity에서 명시적으로 전달한 userId 사용
            finalUserId = userIdFromIntent
            Log.d("MainActivity", "로그인 성공 후 진입 (Intent). UserId: $finalUserId")
            // UserPreference에도 이 ID가 저장되어 있어야 함 (LoginActivity에서 저장)
            // 만약의 경우를 대비해 여기서도 한번 더 저장하거나, LoginActivity에서 확실히 저장하도록 함.
            if (userPreference.getUserId() != finalUserId) {
                userPreference.saveLogin(finalUserId) // UserPreference 동기화
            }
        } else if (userIdFromPrefs != -1) {
            // SharedPreferences에 저장된 ID로 자동 로그인
            finalUserId = userIdFromPrefs
            Log.d("MainActivity", "자동 로그인 성공 (Prefs). UserId: $finalUserId")
        } else {
            Log.d("MainActivity", "로그인 정보 없음 (Intent 및 Prefs).")
        }


        if (savedInstanceState == null) {
            if (finalUserId != -1) { // 유효한 사용자 ID가 있는 경우 (직접 로그인 또는 자동 로그인)
                navController.navigate(R.id.homeFragment)
                binding.mainBnv.visibility = View.VISIBLE

                checkExerciseTable()
                // ★★★ createDummyPlanOnServer 호출 시 확정된 finalUserId 전달 ★★★
                createDummyPlanOnServer(finalUserId)
            } else {
                // 로그인 안 된 상태 (NavGraph의 startDestination으로 이동)
                binding.mainBnv.visibility = View.GONE
            }
        }

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
                        is HomeAichatFragment,
                        is ExerciseListFragment,
                        is ExerciseDoingFragment,
                        is ExerciseDetailFragment,
                        is ExerciseAddFragment,
                        is ExerciseEditSetFragment,
                        is RestTimerFragment,
                        is CoolDownStretchFragment,
                        is RecordAddWeightFragment,
                        is ChallengeUploadPhotoFragment -> {
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

    private fun checkExerciseTable() {
        val db = AppDatabase.getDatabase(this, lifecycleScope)
        lifecycleScope.launch(Dispatchers.IO) {
            val count = db.exerciseDao().getCount()
            Log.d("MainActivity", "📦 현재 exercise 테이블 개수: $count")
        }
    }

    // ★★★ 함수 시그니처에 userId 파라미터 추가 ★★★
    private fun createDummyPlanOnServer(userIdToUse: Int) {
        if (userIdToUse == -1) {
            Log.e("DummyPlan", "createDummyPlanOnServer: 유효하지 않은 사용자 ID (-1). 더미 플랜 생성을 건너<0xEB><0x9B><0x84>니다.")
            return
        }

        val date = LocalDate.now().toString()
        // DummyPlanRequest DTO에 userId 필드가 Int 타입으로 정의되어 있어야 함
        val request = DummyPlanRequest(userId = userIdToUse, date = date)

        Log.d("DummyPlan", "서버에 더미 플랜 생성 요청: userId=$userIdToUse, date=$date")

        RetrofitClient.exerciseApi.createDummyPlan(request)
            .enqueue(object : Callback<DummyPlanResponse> {
                override fun onResponse(
                    call: Call<DummyPlanResponse>,
                    response: Response<DummyPlanResponse>
                ) {
                    if (response.isSuccessful) {
                        val planId = response.body()?.planId
                        val dateStr = response.body()?.date
                        Log.d("DummyPlan", "✅ 더미 운동 계획 생성 응답 - planId: $planId, date: $dateStr, message: ${response.body()?.message}")
                        // planId 저장 로직 추가 가능
                    } else {
                        Log.w("DummyPlan", "⚠️ 더미 운동 계획 생성 서버 응답 오류: ${response.code()} - ${response.message()}")
                        try {
                            val errorBody = response.errorBody()?.string()
                            Log.w("DummyPlan", "Error body: $errorBody")
                        } catch (e: Exception) {
                            Log.e("DummyPlan", "Error body parsing failed", e)
                        }
                    }
                }

                override fun onFailure(call: Call<DummyPlanResponse>, t: Throwable) {
                    Log.e("DummyPlan", "❌ 더미 운동 계획 생성 서버 요청 실패: ${t.message}")
                }
            })
    }

    fun showBottomNav() {
        binding.mainBnv.visibility = View.VISIBLE
    }

    fun setBottomNavSelected(menuItemId: Int) {
        binding.mainBnv.selectedItemId = menuItemId
    }
}


//package com.cookandroid.challengers
//
//
//
//import android.os.Bundle
//import android.util.Log
//import android.view.View
//import androidx.appcompat.app.AppCompatActivity
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.FragmentManager
//import androidx.lifecycle.lifecycleScope
//import androidx.navigation.NavController
//import androidx.navigation.fragment.NavHostFragment
//import androidx.navigation.ui.setupWithNavController
//import com.cookandroid.challengers.api.RetrofitClient
//import com.cookandroid.challengers.data.db.AppDatabase
//import com.cookandroid.challengers.databinding.ActivityMainBinding
//
//import com.cookandroid.challengers.network.dto.DummyPlanRequest
//import com.cookandroid.challengers.network.dto.DummyPlanResponse
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//
//
//
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//import java.time.LocalDate
//import java.time.format.DateTimeFormatter
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//    private lateinit var navController: NavController
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Navigation 설정
//        val navHostFragment =
//            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
//        navController = navHostFragment.navController
//        navController.setGraph(R.navigation.nav_graph_main)
//        binding.mainBnv.setupWithNavController(navController)
//
//        val isLoggedIn = intent.getBooleanExtra("isLoggedIn", false)
//        if (savedInstanceState == null) {
//            if (isLoggedIn) {
//                navController.navigate(R.id.homeFragment)
//                binding.mainBnv.visibility = View.VISIBLE
//
//                // ❗Room 초기 insert는 AppDatabaseCallback.onCreate()에서 자동 실행됨
//                // ✅ exercise 테이블이 비어 있는지 확인만 (옵션)
//                checkExerciseTable()
//
//                // ✅ 서버 기반 더미 플랜 생성
//                createDummyPlanOnServer()
//            } else {
//                binding.mainBnv.visibility = View.GONE
//            }
//        }
//
//        supportFragmentManager.registerFragmentLifecycleCallbacks(
//            object : FragmentManager.FragmentLifecycleCallbacks() {
//                override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
//                    super.onFragmentResumed(fm, f)
//                    when (f) {
//                        is com.cookandroid.challengers.auth.login.LoginFragment,
//                        is com.cookandroid.challengers.auth.login.LoginEmailFragment,
//                        is com.cookandroid.challengers.auth.signup.SignUpEmailFragment,
//                        is com.cookandroid.challengers.auth.signup.SignUpPasswordFragment,
//                        is com.cookandroid.challengers.auth.signup.SignUpOtpFragment,
//                        is com.cookandroid.challengers.auth.signup.SignUpDoneFragment,
//                        is ExerciseListFragment,
//                        is ExerciseDoingFragment,
//                        is ExerciseDetailFragment,
//                        is ExerciseAddFragment,
//                        is ExerciseEditSetFragment,
//                        is RestTimerFragment,
//                        is CoolDownStretchFragment,
//                        is RecordAddWeightFragment -> {
//                            binding.mainBnv.visibility = View.GONE
//                        }
//                        else -> {
//                            binding.mainBnv.visibility = View.VISIBLE
//                        }
//                    }
//                }
//            }, true
//        )
//    }
//
//    // 🔍 선택적 확인용: insert는 하지 않음
//    private fun checkExerciseTable() {
//        val db = AppDatabase.getDatabase(this, lifecycleScope)
//        lifecycleScope.launch(Dispatchers.IO) {
//            val count = db.exerciseDao().getCount()
//            Log.d("MainActivity", "📦 현재 exercise 테이블 개수: $count")
//        }
//    }
//
//    private fun createDummyPlanOnServer() {
//        val userId = getSharedPreferences("UserPrefs", MODE_PRIVATE)
//            .getInt("userId", -1)
//
//        if (userId == -1) {
//            Log.e("DummyPlan", "유저 ID 없음 - 더미 생성 건너뜀")
//            return
//        }
//
//        val date = LocalDate.now().toString()
//        val request = DummyPlanRequest(userId = userId, date = date)
//
//        RetrofitClient.exerciseApi.createDummyPlan(request)
//            .enqueue(object : Callback<DummyPlanResponse> {
//                override fun onResponse(
//                    call: Call<DummyPlanResponse>,
//                    response: Response<DummyPlanResponse>
//                ) {
//                    if (response.isSuccessful) {
//                        val planId = response.body()?.planId
//                        val dateStr = response.body()?.date
//                        val parsedDate = dateStr?.let {
//                            LocalDate.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
//                        }
//                        Log.d("DummyPlan", "✅ 더미 운동 계획 생성됨 - planId: $planId, date: $parsedDate")
//                    } else {
//                        Log.w("DummyPlan", "⚠️ 서버 응답 오류: ${response.code()}")
//                    }
//                }
//
//                override fun onFailure(call: Call<DummyPlanResponse>, t: Throwable) {
//                    Log.e("DummyPlan", "❌ 서버 요청 실패: ${t.message}")
//                }
//            })
//    }
//
//    fun showBottomNav() {
//        binding.mainBnv.visibility = View.VISIBLE
//    }
//
//    private fun clearExerciseTable() {
//        val db = AppDatabase.getDatabase(this, lifecycleScope)
//        lifecycleScope.launch(Dispatchers.IO) {
//            db.exerciseDao().deleteAll()
//            Log.d("MainActivity", "🧹 RoomDB exercise 테이블 초기화 완료")
//        }
//    }
//}