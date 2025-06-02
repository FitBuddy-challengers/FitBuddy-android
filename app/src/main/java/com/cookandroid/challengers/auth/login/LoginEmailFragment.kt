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
import com.cookandroid.challengers.auth.login.LoginService
import com.cookandroid.challengers.auth.login.LoginRequest
import com.cookandroid.challengers.auth.login.LoginResponse
import com.cookandroid.challengers.HomeFragment
import com.cookandroid.challengers.auth.LoginActivity

import com.cookandroid.challengers.databinding.FragmentLoginEmailBinding
import com.cookandroid.challengers.util.UserPreference

class LoginEmailFragment : Fragment() {

    private var _binding: FragmentLoginEmailBinding? = null
    private val binding get() = _binding!!

    private val loginService = RetrofitClient.loginService

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
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnEmailSignin.setOnClickListener {
            val email = binding.tilLoginAddress.editText?.text.toString()
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
                    val userId = response.body()!!.user!!.id
                    Log.d("Login", "로그인 성공 - userId: $userId")

                    // SharedPreferences에 userId 저장 -> 자동로그인을 위해!
                    val userPref = UserPreference(requireContext())
                    userPref.saveLogin(userId)

                    Toast.makeText(requireContext(), "로그인 성공!", Toast.LENGTH_SHORT).show()
                    (requireActivity() as? LoginActivity)?.navigateToMain()


                } else {
                    Toast.makeText(requireContext(), "이메일 또는 비밀번호가 일치하지 않아요.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "서버 연결 실패", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isBlank()) {
            Toast.makeText(requireContext(), "이메일을 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isBlank()) {
            Toast.makeText(requireContext(), "비밀번호를 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 8) {
            Toast.makeText(requireContext(), "비밀번호는 8자 이상이어야 해요.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}