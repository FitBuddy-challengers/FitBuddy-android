package com.cookandroid.challengers


import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.cookandroid.challengers.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
        navController = navHostFragment.navController

        navController.setGraph(R.navigation.nav_graph_main)
        binding.mainBnv.setupWithNavController(navController)

        val isLoggedIn = intent.getBooleanExtra("isLoggedIn", false)

        if (savedInstanceState == null) {
            if (isLoggedIn) {
                navController.navigate(R.id.homeFragment)
                binding.mainBnv.visibility = View.VISIBLE
            } else {
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
                        is ExerciseListFragment -> {
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