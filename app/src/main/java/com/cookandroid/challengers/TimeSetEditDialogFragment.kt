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
        if (scheduleId != -1L) { // scheduleId가 유효한 경우에만 호출
            RetrofitClient.scheduleApi.getTimeSets(scheduleId).enqueue(object : Callback<List<TimeSetDto>> {
                override fun onResponse(call: Call<List<TimeSetDto>>, response: Response<List<TimeSetDto>>) {
                    if (response.isSuccessful) {
                        val serverSetsDto = response.body()
                        if (serverSetsDto != null) {
                            val uiSets = serverSetsDto.mapIndexed { index, dto ->
                                // dto.seconds (총 초)를 시, 분, 초로 변환
                                val totalSecondsFromServer = dto.seconds
                                val hours = totalSecondsFromServer / 3600
                                val remainderSecondsAfterHours = totalSecondsFromServer % 3600
                                val minutes = remainderSecondsAfterHours / 60
                                val seconds = remainderSecondsAfterHours % 60

                                TimeSetUiModel(
                                    setNumber = dto.setNumber, // 서버에서 받은 setNumber 사용 또는 index + 1
                                    hours = hours,
                                    minutes = minutes,
                                    seconds = seconds,
                                    weight = dto.weight
                                )
                            }.toMutableList()

                            // 어댑터에 새 데이터로 다시 설정 (또는 submitList 같은 메소드 사용)
                            adapter = TimeSetAdapter(uiSets) // 새 데이터로 어댑터 재생성
                            binding.recyclerViewSetList.adapter = adapter // 어댑터 다시 연결
                            // 또는 adapter.submitList(uiSets) 와 같은 메소드가 있다면 사용
                        } else {
                            // response.body()가 null인 경우 처리
                            Log.e("TimeSetEdit", "서버 응답 성공했으나 body가 null")
                            Toast.makeText(requireContext(), "세트 데이터를 불러오는 데 실패했습니다. (body null)", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("TimeSetEdit", "서버 응답 실패: ${response.code()} - ${response.message()}")
                        Toast.makeText(requireContext(), "세트 데이터를 불러오지 못했습니다: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<TimeSetDto>>, t: Throwable) {
                    Log.e("TimeSetEdit", "서버 연결 실패", t)
                    Toast.makeText(requireContext(), "네트워크 오류로 세트를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Log.e("TimeSetEdit", "유효하지 않은 scheduleId: $scheduleId")
            Toast.makeText(requireContext(), "잘못된 스케줄 정보입니다.", Toast.LENGTH_SHORT).show()
            // 필요하다면 dismiss() 호출 또는 사용자에게 다른 피드백
        }
        // 🔹 세트 추가 버튼
        binding.buttonAddSet.setOnClickListener {
            adapter.addSet()
        }

        // 🔹 저장 버튼
        binding.buttonSaveSet.setOnClickListener {
            // adapter.getSetList()가 List<TimeSetUiModel> (시,분,초 포함)을 반환한다고 가정
            val updatedUiSets = adapter.getSetList()
            val dtoList = updatedUiSets.mapIndexed { index, uiModel ->
                // TimeSetUiModel (시,분,초)을 TimeSetDto (총 초)로 변환
                val totalSecondsForServer = (uiModel.hours * 3600) + (uiModel.minutes * 60) + uiModel.seconds
                TimeSetDto(
                    setNumber = uiModel.setNumber, // 또는 index + 1, 서버 DTO의 요구사항에 따라 결정
                    seconds = totalSecondsForServer,
                    weight = uiModel.weight,
                    isCompleted = false
                )
            }

            if (scheduleId != -1L) {
                RetrofitClient.scheduleApi.updateTimeSets(scheduleId, dtoList)
                    .enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            if (response.isSuccessful) {
                                Toast.makeText(requireContext(), "세트가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                                // 성공 시 ExerciseEditFragment에 결과를 전달하기 위한 새로운 requestKey 사용
                                parentFragmentManager.setFragmentResult("dialog_sets_updated", Bundle.EMPTY) // 👈 이렇게 변경
                                dismiss()
                            }else {
                                Log.e("TimeSetEdit", "저장 실패: ${response.code()} - ${response.message()}")
                                Toast.makeText(requireContext(), "저장에 실패했습니다: ${response.code()}", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            Log.e("TimeSetEdit", "저장 중 서버 연결 실패", t)
                            Toast.makeText(requireContext(), "네트워크 오류로 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    })
            } else {
                Log.e("TimeSetEdit", "저장 시도 시 유효하지 않은 scheduleId: $scheduleId")
                Toast.makeText(requireContext(), "잘못된 스케줄 정보로 저장할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TimeSetEditDialogFragment"

        fun newInstance(scheduleId: Long) = TimeSetEditDialogFragment().apply {
            arguments = bundleOf("scheduleId" to scheduleId)
        }
    }
}