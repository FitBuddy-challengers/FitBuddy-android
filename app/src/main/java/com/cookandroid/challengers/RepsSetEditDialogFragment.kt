package com.cookandroid.challengers


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.api.RetrofitClient.RepsSetDto
import com.cookandroid.challengers.api.RetrofitClient.RepsSetUiModel
import com.cookandroid.challengers.databinding.FragmentExerciseEditSetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RepsSetEditDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentExerciseEditSetBinding? = null
    private val binding get() = _binding!!

    private var scheduleId: Long = -1L
    private lateinit var adapter: RepsSetAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            scheduleId = it.getLong("scheduleId")
            Log.d("세트수정", "✅ onCreate()에서 받은 scheduleId = $scheduleId")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExerciseEditSetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 1. RecyclerView 레이아웃 매니저 설정
        binding.recyclerViewSetList.layoutManager = LinearLayoutManager(requireContext())

        // 2. 어댑터 초기화 (빈 리스트)
        adapter = RepsSetAdapter(mutableListOf())
        binding.recyclerViewSetList.adapter = adapter

        // 3. 서버에서 세트 불러오기
        RetrofitClient.scheduleApi.getRepsSets(scheduleId).enqueue(object : Callback<List<RepsSetDto>> {
            override fun onResponse(call: Call<List<RepsSetDto>>, response: Response<List<RepsSetDto>>) {
                if (response.isSuccessful) {
                    val sets = response.body()?.mapIndexed { index, dto ->
                        RepsSetUiModel(index + 1, dto.reps, dto.weight)
                    } ?: listOf()
                    parentFragmentManager.setFragmentResult("sets_updated", bundleOf())
                    //dismiss() // 저장 성공 시 결과 전달

                    Log.d("세트수정", "✅ 받아온 세트 수: ${sets.size}")

                    // ✅ 어댑터 내부 리스트만 갱신
                    adapter.updateSets(sets.toMutableList())
                    adapter.notifyDataSetChanged()
                } else {
                    Log.e("RepsSetEdit", "❌ 서버 응답 오류: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<RepsSetDto>>, t: Throwable) {
                Log.e("RepsSetEdit", "❌ 세트 불러오기 실패", t)
            }
        })

        // 4. 세트 추가
        binding.buttonAddSet.setOnClickListener {
            adapter.addSet()
        }

        // 5. 저장
        binding.buttonSaveSet.setOnClickListener {
            val updatedSets = adapter.getSetList().mapIndexed { index, item ->
                RepsSetDto(setNumber = index + 1, reps = item.reps, weight = item.weight)
            }

            RetrofitClient.scheduleApi.updateRepsSets(scheduleId, updatedSets)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            dismiss()
                        } else {
                            Log.e("RepsSetEdit", "서버 응답 오류: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        Log.e("RepsSetEdit", "저장 실패", t)
                    }
                })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(scheduleId: Long) = RepsSetEditDialogFragment().apply {
            arguments = bundleOf("scheduleId" to scheduleId)
        }
    }


}
