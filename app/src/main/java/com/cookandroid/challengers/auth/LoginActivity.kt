package com.cookandroid.challengers.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.cookandroid.challengers.MainActivity
import com.cookandroid.challengers.R
import com.cookandroid.challengers.databinding.ActivityLoginBinding
import com.cookandroid.challengers.util.UserPreference

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userPref = UserPreference(this)
        val savedUserId = userPref.getUserId()
        if (savedUserId != -1) { // SharedPreferences에 저장된 ID가 있으면 자동 로그인
            Log.d("LoginActivity", "✅ 자동 로그인 시도. UserId from Prefs: $savedUserId")





            navigateToMain(savedUserId) // ★ 자동 로그인 시에도 userId 전달
            return // MainActivity로 이동 후 LoginActivity는 종료
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container_view) as NavHostFragment
        navController = navHostFragment.navController
        // NavGraph는 XML에서 설정되어 있다면 여기서 setGraph 불필요
    }

    // ★★★ userId 파라미터를 받도록 수정 ★★★
    fun navigateToMain(userId: Int) {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra("isLoggedIn", true)
        intent.putExtra("userId", userId)   // ★★★ MainActivity에 userId 전달 ★★★
        startActivity(intent)
        finish() // LoginActivity 종료
    }
}


//package com.cookandroid.challengers.auth
//
//import android.content.Intent
//import android.os.Bundle
//import android.util.Log
//import androidx.appcompat.app.AppCompatActivity
//import androidx.navigation.NavController
//import androidx.navigation.fragment.NavHostFragment
//import com.cookandroid.challengers.MainActivity
//import com.cookandroid.challengers.R
//import com.cookandroid.challengers.databinding.ActivityLoginBinding
//import com.cookandroid.challengers.util.UserPreference
//
//class LoginActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityLoginBinding
//    private lateinit var navController: NavController
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // 자동 로그인 체크 추가
//        val savedUserId = UserPreference(this).getUserId()
//        if (savedUserId != -1) {
//            Log.d("자동로그인", "✅ 자동 로그인 userId: $savedUserId")
//            navigateToMain()
//            return
//        }
//
//
//        binding = ActivityLoginBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // NavController 연결
//        val navHostFragment = supportFragmentManager
//            .findFragmentById(R.id.fragment_container_view) as NavHostFragment
//        navController = navHostFragment.navController
//    }
//
//    // 로그인 성공 or 회원가입 완료 후 메인으로 이동하는 함수
//    /*fun navigateToMain() {
//        val intent = Intent(this, MainActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        startActivity(intent)
//    }
//    */
//    fun navigateToMain() {
//        val intent = Intent(this, MainActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        intent.putExtra("isLoggedIn", true)
//        startActivity(intent)
//    }
//
//}