package com.cookandroid.challengers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.databinding.FragmentMypageInfoSetBinding
import com.cookandroid.challengers.model.ProfileData
import com.cookandroid.challengers.util.UserPreference
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log

class MypageInfoSetFragment : Fragment() {

    private var _binding: FragmentMypageInfoSetBinding? = null
    private val binding get() = _binding!!

    private lateinit var userPreference: UserPreference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageInfoSetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userPreference = UserPreference(requireContext())
        val email = userPreference.getUserEmail()

        // 프로필 불러오기
        RetrofitClient.profileApiService.getProfile(email)
            .enqueue(object : Callback<ProfileData> {
                override fun onResponse(call: Call<ProfileData>, response: Response<ProfileData>) {
                    if (response.isSuccessful) {
                        val profile = response.body()
                        profile?.let {
                            binding.etUserName.setText(it.name)
                            binding.etHeight.setText(it.height.toString())
                            binding.etWeight.setText(it.weight.toString())
                            binding.etDisease.setText(it.diseases.joinToString(", "))
                            binding.etPreferredWorkout.setText(it.preferred_workouts.joinToString(", "))
                            binding.etEquipment.setText(it.equipment.joinToString(", "))

                            setSpinner(binding.spinnerAgeGroup, it.age_group)
                            setSpinner(binding.spinnerGender, it.gender)
                            setSpinner(binding.spinnerWorkoutLevel, it.workout_level)
                        }
                    } else {
                        Toast.makeText(requireContext(), "불러오기 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ProfileData>, t: Throwable) {
                    Log.e("MypageInfoSet", "불러오기 실패", t)
                    Toast.makeText(requireContext(), "오류: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // 수정 후 저장
        binding.btnProfileEdit.setOnClickListener {
            val updatedProfile = ProfileData(
                email = email,
                name = binding.etUserName.text.toString(),
                age_group = binding.spinnerAgeGroup.selectedItem.toString(),
                gender = binding.spinnerGender.selectedItem.toString(),
                height = binding.etHeight.text.toString().toIntOrNull() ?: 0,
                weight = binding.etWeight.text.toString().toIntOrNull() ?: 0,
                diseases = binding.etDisease.text.toString().split(",").map { it.trim() },
                workout_level = binding.spinnerWorkoutLevel.selectedItem.toString(),
                preferred_workouts = binding.etPreferredWorkout.text.toString().split(",").map { it.trim() },
                equipment = binding.etEquipment.text.toString().split(",").map { it.trim() }
            )

            RetrofitClient.profileApiService.updateProfile(updatedProfile)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "수정 완료", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "수정 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.e("MypageInfoSet", "수정 실패", t)
                        Toast.makeText(requireContext(), "통신 오류: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun setSpinner(spinner: Spinner, value: String) {
        val adapter = spinner.adapter
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i) == value) {
                spinner.setSelection(i)
                return
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}