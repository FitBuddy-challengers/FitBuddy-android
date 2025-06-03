package com.cookandroid.challengers.auth.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.MainActivity
import com.cookandroid.challengers.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.cookandroid.challengers.api.RetrofitClient
// LoginService, LoginRequest, LoginResponse는 RetrofitClient 내부에 정의되어 있다고 가정
// import com.cookandroid.challengers.auth.login.LoginService
// import com.cookandroid.challengers.auth.login.LoginRequest
// import com.cookandroid.challengers.auth.login.LoginResponse
// import com.cookandroid.challengers.HomeFragment // 사용 안 함
import com.cookandroid.challengers.auth.LoginActivity
import com.cookandroid.challengers.databinding.FragmentLoginEmailBinding
import com.cookandroid.challengers.util.UserPreference

class LoginEmailFragment : Fragment() {

    private var _binding: FragmentLoginEmailBinding? = null
    private val binding get() = _binding!!

    // RetrofitClient에 loginService가 정의되어 있다고 가정
    private val loginService by lazy { RetrofitClient.loginService }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnEmailSignin.setOnClickListener {
            val email = binding.tilLoginAddress.editText?.text.toString().trim()
            val password = binding.tilLoginPassword.editText?.text.toString()

            if (validateInput(email, password)) {
                tryLogin(email, password)
            }
        }

        binding.btnEmailSignup.setOnClickListener {
            findNavController().navigate(R.id.action_loginEmail_to_signUpEmail)
        }
    }

    private fun tryLogin(email: String, password: String) {
        val request = LoginRequest(email, password)
        loginService.login(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body()?.user != null) {
                    val user = response.body()!!.user!!
                    val userId = user.id
                    Log.d("LoginEmailFragment", "로그인 성공 - userId: $userId, email: ${user.email}")

                    val userPref = UserPreference(requireContext())
                    userPref.saveLogin(userId)

                    Toast.makeText(requireContext(), "로그인 성공!", Toast.LENGTH_SHORT).show()
                    // ★★★ LoginActivity의 navigateToMain 함수에 userId 전달 ★★★
                    (requireActivity() as? LoginActivity)?.navigateToMain(userId) // userId 전달

                } else {
                    Log.w("LoginEmailFragment", "로그인 실패: ${response.code()} - ${response.message()}, body: ${response.errorBody()?.string()}")
                    Toast.makeText(requireContext(), "이메일 또는 비밀번호가 일치하지 않아요.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Log.e("LoginEmailFragment", "서버 연결 실패", t)
                Toast.makeText(requireContext(), "서버 연결에 실패했습니다. 네트워크 상태를 확인해주세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isBlank()) {
            Toast.makeText(requireContext(), "이메일을 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "올바른 이메일 형식이 아니에요.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isBlank()) {
            Toast.makeText(requireContext(), "비밀번호를 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


//package com.cookandroid.challengers.auth.login
//
//
//import android.content.Intent
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.fragment.app.Fragment
//import androidx.navigation.fragment.findNavController
//import com.cookandroid.challengers.MainActivity
//import com.cookandroid.challengers.R
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//import com.cookandroid.challengers.api.RetrofitClient
//import com.cookandroid.challengers.auth.login.LoginService
//import com.cookandroid.challengers.auth.login.LoginRequest
//import com.cookandroid.challengers.auth.login.LoginResponse
//import com.cookandroid.challengers.HomeFragment
//import com.cookandroid.challengers.auth.LoginActivity
//
//import com.cookandroid.challengers.databinding.FragmentLoginEmailBinding
//import com.cookandroid.challengers.util.UserPreference
//
//class LoginEmailFragment : Fragment() {
//
//    private var _binding: FragmentLoginEmailBinding? = null
//    private val binding get() = _binding!!
//
//    private val loginService = RetrofitClient.loginService
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?,
//    ): View {
//        _binding = FragmentLoginEmailBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        binding.btnBack.setOnClickListener {
//            requireActivity().onBackPressedDispatcher.onBackPressed()
//        }
//
//        binding.btnEmailSignin.setOnClickListener {
//            val email = binding.tilLoginAddress.editText?.text.toString()
//            val password = binding.tilLoginPassword.editText?.text.toString()
//
//            if (validateInput(email, password)) {
//                tryLogin(email, password)
//            }
//        }
//
//        binding.btnEmailSignup.setOnClickListener {
//            findNavController().navigate(R.id.action_loginEmail_to_signUpEmail)
//        }
//    }
//
//    private fun tryLogin(email: String, password: String) {
//        val request = LoginRequest(email, password)
//        loginService.login(request).enqueue(object : Callback<LoginResponse> {
//            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
//                if (response.isSuccessful && response.body()?.user != null) {
//                    val userId = response.body()!!.user!!.id
//                    Log.d("Login", "로그인 성공 - userId: $userId")
//
//                    // SharedPreferences에 userId 저장 -> 자동로그인을 위해!
//                    val userPref = UserPreference(requireContext())
//                    userPref.saveLogin(userId)
//
//                    Toast.makeText(requireContext(), "로그인 성공!", Toast.LENGTH_SHORT).show()
//                    (requireActivity() as? LoginActivity)?.navigateToMain()
//
//
//                } else {
//                    Toast.makeText(requireContext(), "이메일 또는 비밀번호가 일치하지 않아요.", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
//                Toast.makeText(requireContext(), "서버 연결 실패", Toast.LENGTH_SHORT).show()
//            }
//        })
//    }
//
//    private fun validateInput(email: String, password: String): Boolean {
//        if (email.isBlank()) {
//            Toast.makeText(requireContext(), "이메일을 입력해 주세요.", Toast.LENGTH_SHORT).show()
//            return false
//        }
//        if (password.isBlank()) {
//            Toast.makeText(requireContext(), "비밀번호를 입력해 주세요.", Toast.LENGTH_SHORT).show()
//            return false
//        }
//        if (password.length < 8) {
//            Toast.makeText(requireContext(), "비밀번호는 8자 이상이어야 해요.", Toast.LENGTH_SHORT).show()
//            return false
//        }
//        return true
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}