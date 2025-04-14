package com.cookandroid.challengers


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cookandroid.challengers.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 뷰바인딩 초기화
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 앱 시작 시 홈 프래그먼트 표시
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_frm, HomeFragment())
            .commitAllowingStateLoss()

        // 하단 바 클릭 이벤트 처리
        binding.mainBnv.setOnItemSelectedListener { item ->
            val selectedFragment = when (item.itemId) {
                R.id.homeFragment -> HomeFragment()
                R.id.exerciseFragment -> ExerciseFragment()
                R.id.challengeFragment -> ChallengeFragment()
                R.id.recordFragment -> RecordFragment()
                R.id.storeFragment -> StoreFragment()
                else -> null
            }

            selectedFragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.main_frm, it)
                    .commitAllowingStateLoss()
                true
            } ?: false
        }
    }
}

