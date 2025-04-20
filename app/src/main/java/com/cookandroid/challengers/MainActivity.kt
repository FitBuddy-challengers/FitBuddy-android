package com.cookandroid.challengers



import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.cookandroid.challengers.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 앱 처음 실행 시 LoginFragment로 이동
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, LoginFragment())
                .commit()
        }

        // 하단바 클릭 시 다른 프래그먼트로 이동
        binding.mainBnv.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment? = when (item.itemId) {
                R.id.homeFragment -> HomeFragment()
                R.id.exerciseFragment -> ExerciseFragment()
                R.id.challengeFragment -> ChallengeFragment()
                R.id.recordFragment -> RecordFragment()
                R.id.storeFragment -> StoreFragment()
                else -> null
            }

            selectedFragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_view, it)
                    .commitAllowingStateLoss()
                true
            } ?: false
        }

        // 로그인/회원가입 화면에서는 하단바 숨기기
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                    super.onFragmentResumed(fm, f)
                    when (f) {
                        is LoginFragment,
                        is LoginEmailFragment,
                        is SignUpEmailFragment,
                        is SignUpPasswordFragment,
                        is SignUpOtpFragment,
                        is SignUpDoneFragment -> {
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


