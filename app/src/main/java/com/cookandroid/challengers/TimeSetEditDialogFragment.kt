package com.cookandroid.challengers


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.databinding.FragmentExerciseEditSetBinding
import com.cookandroid.challengers.api.RetrofitClient.TimeSetDto
import com.cookandroid.challengers.api.RetrofitClient.TimeSetUiModel
import com.cookandroid.challengers.databinding.FragmentExerciseEditTimeBinding

import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TimeSetEditDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentExerciseEditTimeBinding? = null
    private val binding get() = _binding!!

    private var scheduleId: Long = -1L
    private lateinit var adapter: TimeSetAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            scheduleId = it.getLong("scheduleId")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExerciseEditTimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 🔹 초기 어댑터 설정 (빈 리스트로 먼저)
        adapter = TimeSetAdapter(mutableListOf())
        binding.recyclerViewSetList.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewSetList.adapter = adapter

        // 🔹 서버에서 세트 불러오기
        RetrofitClient.scheduleApi.getTimeSets(scheduleId).enqueue(object : Callback<List<TimeSetDto>> {
            override fun onResponse(call: Call<List<TimeSetDto>>, response: Response<List<TimeSetDto>>) {
                if (response.isSuccessful) {
                    val sets = response.body()?.mapIndexed { index, dto ->
                        TimeSetUiModel(index + 1, dto.seconds / 60, dto.weight)
                    }?.toMutableList() ?: mutableListOf()

                    adapter = TimeSetAdapter(sets)
                    binding.recyclerViewSetList.adapter = adapter
                } else {
                    Log.e("TimeSetEdit", "서버 응답 실패: ${response.code()}")
                    Toast.makeText(requireContext(), "세트 데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<TimeSetDto>>, t: Throwable) {
                Log.e("TimeSetEdit", "서버 연결 실패", t)
                Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
        })

        // 🔹 세트 추가 버튼
        binding.buttonAddSet.setOnClickListener {
            adapter.addSet()
        }

        // 🔹 저장 버튼
        binding.buttonSaveSet.setOnClickListener {
            val updatedSets = adapter.getSetList().mapIndexed { index, set ->
                TimeSetDto(setNumber = index + 1, seconds = set.minutes * 60, weight = set.weight)
            }

            RetrofitClient.scheduleApi.updateTimeSets(scheduleId, updatedSets)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "세트가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                            dismiss()
                        } else {
                            Log.e("TimeSetEdit", "저장 실패: ${response.code()}")
                            Toast.makeText(requireContext(), "저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.e("TimeSetEdit", "저장 실패", t)
                        Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(scheduleId: Long) = TimeSetEditDialogFragment().apply {
            arguments = bundleOf("scheduleId" to scheduleId)
        }
    }
}