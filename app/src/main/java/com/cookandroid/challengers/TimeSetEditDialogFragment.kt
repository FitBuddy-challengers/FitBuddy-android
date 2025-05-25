package com.cookandroid.challengers


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        // 1. 서버에서 기존 세트 불러오기
        RetrofitClient.scheduleApi.getTimeSets(scheduleId).enqueue(object : Callback<List<TimeSetDto>> {
            override fun onResponse(call: Call<List<RetrofitClient.TimeSetDto>>, response: Response<List<TimeSetDto>>) {
                if (response.isSuccessful) {
                    val sets = response.body()?.mapIndexed { index, dto ->
                        RetrofitClient.TimeSetUiModel(index + 1, dto.seconds / 60, dto.weight)
                    }?.toMutableList() ?: mutableListOf()

                    adapter = TimeSetAdapter(sets)
                    binding.recyclerViewSetList.adapter = adapter
                }
            }

            override fun onFailure(call: Call<List<RetrofitClient.TimeSetDto>>, t: Throwable) {
                Log.e("TimeSetEdit", "서버에서 세트 가져오기 실패", t)
            }
        })

        // 2. 세트 추가
        binding.buttonAddSet.setOnClickListener {
            adapter.addSet()
        }

        // 3. 저장
        binding.buttonSaveSet.setOnClickListener {
            val updatedSets = adapter.getSetList().mapIndexed { index, it ->
                TimeSetDto(setNumber = index + 1, seconds = it.minutes * 60, weight = it.weight)
            }

            RetrofitClient.scheduleApi.updateTimeSets(scheduleId, updatedSets)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            dismiss()
                        } else {
                            Log.e("TimeSetEdit", "서버 응답 오류: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.e("TimeSetEdit", "저장 실패", t)
                    }
                })
        }
    }

    companion object {
        fun newInstance(scheduleId: Long) = TimeSetEditDialogFragment().apply {
            arguments = bundleOf("scheduleId" to scheduleId)
        }
    }
}