package com.cookandroid.challengers



import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.cookandroid.challengers.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ⭐ 처음 시작할 때 BottomNavigationView 숨기기
        if (savedInstanceState == null) {
            binding.mainBnv.visibility = View.GONE
        }

        // NavController 가져오기
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        val navController = navHostFragment.navController

        // BottomNavigationView를 NavController에 연결
        binding.mainBnv.setupWithNavController(navController)

        // 로그인/회원가입 화면에서는 하단바 숨기기
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
                        is com.cookandroid.challengers.auth.signup.SignUpDoneFragment -> {
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
}
