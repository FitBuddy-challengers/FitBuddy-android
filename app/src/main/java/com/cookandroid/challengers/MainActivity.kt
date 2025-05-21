package com.cookandroid.challengers

import android.os.Bundle
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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 최초 계획이 없으면 하나 생성
        val db = AppDatabase.getDatabase(this, lifecycleScope)
        lifecycleScope.launch {
            val planDao = db.exercisePlanDao()

            val zoneId = ZoneId.of("Asia/Seoul")

            val todayStart = LocalDate.now(zoneId)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()

            val todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1  // 오늘 끝
            val todayPlans = planDao.getPlansByDate(todayStart, todayEnd)

            if (todayPlans.isEmpty()) {
                val insertedId = planDao.insert(ExercisePlan(plannedDate = todayStart))

                if (insertedId == 0L) {
                    // 이론상 도달하지 않아야 하지만, insert 시점에 race condition 방지
                    android.util.Log.w("MainActivity", "❗ 이미 오늘 날짜의 plan이 존재 (insert skipped)")
                } else {
                    android.util.Log.i("MainActivity", "✅ 오늘 날짜의 plan 생성됨 (id=$insertedId)")
                }
            } else {
                android.util.Log.i("MainActivity", "✅ 오늘 날짜의 plan 이미 존재 (id=${todayPlans.first().id})")
            }
        }


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


    fun showBottomNav() {
        binding.mainBnv.visibility = View.VISIBLE
    }
}
