/*package com.cookandroid.challengers

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cookandroid.challengers.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}*/

//안드로이드 요즘 개발 트렌드는 액티비티를 무조건 최소화! 메인 액티비티만 남기는 경우가 많기에 이 경우는 프래그먼트로 전환!