package com.cookandroid.challengers

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.data.Exercise
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentExerciseDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExerciseDetailFragment : Fragment() {

    private var _binding: FragmentExerciseDetailBinding? = null
    private val binding get() = _binding!!

//    private var exerciseId: Long = -1L
    private lateinit var db: AppDatabase
    private lateinit var currentExercise: Exercise

    companion object {
        private const val ARG_EXERCISE_ID = "exerciseId"

        fun newInstance(exerciseId: Long) = ExerciseDetailFragment().apply {
            arguments = Bundle().apply { putLong(ARG_EXERCISE_ID, exerciseId) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getParcelable<Exercise>("exerciseObject")?.let {
            currentExercise = it
            Log.d("ID_CHECK", "3. [상세 화면] arguments로부터 받은 운동: ${currentExercise.name}, ID: ${currentExercise.id}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext(), lifecycleScope)

        // 전달받은 currentExercise 객체가 초기화되었는지 확인
        if (::currentExercise.isInitialized) {
            // 1. 객체를 직접 전달받은 경우 (기존 로직)
            bindExerciseData(currentExercise)
        } else {
            // 2. ID만 전달받은 경우 (Edit 화면에서 넘어올 때)
            val exerciseId = arguments?.getLong("exerciseId", -1L) ?: -1L

            if (exerciseId != -1L) {
                fetchExerciseData(exerciseId) // 서버 통신 함수 호출
            } else {
                // 3. 둘 다 없는 경우 예외 처리
                Toast.makeText(requireContext(), "운동 정보를 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
                return
            }
        }

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.favoriteButton.setOnClickListener {
            toggleFavorite()
        }
    }
    private fun fetchExerciseData(targetId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 서버에서 전체 운동 목록 가져오기
                val response = RetrofitClient.exerciseApi.getAllExercises()

                if (response.isSuccessful && response.body() != null) {
                    // ID로 해당 운동 찾기
                    val foundDto = response.body()!!.find { it.id == targetId }

                    withContext(Dispatchers.Main) {
                        if (_binding == null) return@withContext

                        if (foundDto != null) {
                            // DTO -> Exercise 객체로 변환 (ChangeFragment와 동일한 로직)
                            currentExercise = Exercise(
                                id = foundDto.id,
                                name = foundDto.name ?: "",
                                part = foundDto.part ?: "",
                                equip = foundDto.equip ?: "",
                                imagePath = foundDto.image_path,
                                startPosition = foundDto.start_position,
                                exerciseMotion = foundDto.exercise_motion,
                                breathing = foundDto.breathing,
                                caution = foundDto.caution,
                                mets = foundDto.mets ?: 0.0,
                                isFavorite = foundDto.isFavorite ?: false,
                                isTimeType = foundDto.isTimeType ?: false,
                                isHidden = foundDto.isHidden ?: false,
                                isNoise = foundDto.is_noise ?: false
                            )
                            // 화면에 데이터 표시
                            bindExerciseData(currentExercise)
                        } else {
                            Toast.makeText(requireContext(), "해당 운동을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        if (_binding != null) {
                            Toast.makeText(requireContext(), "서버 통신 오류: ${response.code()}", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ExerciseDetail", "데이터 로드 실패", e)
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        Toast.makeText(requireContext(), "오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun bindExerciseData(exercise: Exercise) {
        binding.titleText.text = exercise.name
        binding.favoriteButton.isSelected = exercise.isFavorite

        // 이미지 로드
        val resId = resources.getIdentifier(exercise.imagePath ?: "", "drawable", requireContext().packageName)
        if (resId != 0) {
            Glide.with(requireContext())
                .load(resId)
                .placeholder(R.drawable.ic_fitbuddy_logo) // 로딩 중 표시할 이미지 (선택 사항)
                .error(R.drawable.ic_fitbuddy_logo)     // 에러 발생 시 표시할 이미지 (선택 사항)
                .transition(withCrossFade())
                .into(binding.imagePlaceholder)
        } else {
            binding.imagePlaceholder.setImageResource(R.drawable.ic_fitbuddy_logo)
        }

        // 시작 자세 텍스트
        val exerciseStartPositionList = exercise.startPosition
        if (exerciseStartPositionList.isNullOrEmpty() || exerciseStartPositionList.all { it.isBlank() }) {
            binding.startPositionTitle.visibility = View.GONE
            binding.startPositionContent.visibility = View.GONE
        } else {
            binding.startPositionTitle.visibility = View.VISIBLE
            binding.startPositionContent.visibility = View.VISIBLE

            if (exerciseStartPositionList.size > 1) {
                val numberedText = exerciseStartPositionList.mapIndexed { index, text ->
                    "${index + 1}. $text"
                }.joinToString("\n")
                binding.startPositionContent.text = numberedText
            } else {
                binding.startPositionContent.text = exerciseStartPositionList.firstOrNull() ?: ""
            }
        }

        // 운동 설명 텍스트
        val exerciseMotionList = exercise.exerciseMotion
        if (exerciseMotionList.isNullOrEmpty() || exerciseMotionList.all { it.isBlank() }) {
            binding.exerciseMotionTitle.visibility = View.GONE
            binding.exerciseMotionContent.visibility = View.GONE
        } else {
            binding.exerciseMotionTitle.visibility = View.VISIBLE
            binding.exerciseMotionContent.visibility = View.VISIBLE

            if (exerciseMotionList.size > 1) {
                val numberedText = exerciseMotionList.mapIndexed { index, text ->
                    "${index + 1}. $text"
                }.joinToString("\n")
                binding.exerciseMotionContent.text = numberedText
            } else {
                binding.exerciseMotionContent.text = exerciseMotionList.firstOrNull() ?: ""
            }
        }

        // 호흡법 텍스트
        val breathingList = exercise.breathing
        if (breathingList.isNullOrEmpty() || breathingList.all { it.isBlank() }) {
            binding.breathingTitle.visibility = View.GONE
            binding.breathingContent.visibility = View.GONE
        } else {
            binding.breathingTitle.visibility = View.VISIBLE
            binding.breathingContent.visibility = View.VISIBLE

            if (breathingList.size > 1) {
                val numberedText = breathingList.mapIndexed { index, text ->
                    "${index + 1}. $text"
                }.joinToString("\n")
                binding.breathingContent.text = numberedText
            } else {
                binding.breathingContent.text = breathingList.firstOrNull() ?: ""
            }
        }

        // 주의사항 텍스트
        val cuationList = exercise.caution
        if (cuationList.isNullOrEmpty() || cuationList.all { it.isBlank() }) {
            binding.cautionTitle.visibility = View.GONE
            binding.cautionContent.visibility = View.GONE
        } else {
            binding.cautionTitle.visibility = View.VISIBLE
            binding.cautionContent.visibility = View.VISIBLE

            if (cuationList.size > 1) {
                val numberedText = cuationList.mapIndexed { index, text ->
                    "${index + 1}. $text"
                }.joinToString("\n")
                binding.cautionContent.text = numberedText
            } else {
                binding.cautionContent.text = cuationList.firstOrNull() ?: ""
            }
        }
    }

    private fun toggleFavorite() {
        // currentExercise가 초기화되지 않았으면 아무것도 하지 않음 (안전장치)
        if (!::currentExercise.isInitialized) return

        val newFavoriteStatus = !currentExercise.isFavorite

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 🔥 서버에 즐겨찾기 상태 변경을 요청합니다. (로컬 DB 대신)
                val response = RetrofitClient.exerciseApi.toggleExerciseFavorite(
                    currentExercise.id,
                    RetrofitClient.ToggleFavoriteRequest(newFavoriteStatus)
                )

                // 2. 서버 응답이 성공하면 UI를 업데이트하고 결과를 알립니다.
                if (response.isSuccessful) {
                    // 현재 객체의 상태를 서버 응답에 맞춰 업데이트합니다.
                    currentExercise = currentExercise.copy(isFavorite = newFavoriteStatus)

                    withContext(Dispatchers.Main) {
                        // 프래그먼트가 화면에 없을 때 UI를 건드리지 않도록 방어합니다.
                        if (_binding == null) return@withContext

                        // UI(버튼 모양)에 변경 사항을 반영합니다.
                        binding.favoriteButton.isSelected = newFavoriteStatus
                        Toast.makeText(
                            requireContext(),
                            if (newFavoriteStatus) "즐겨찾기에 추가되었습니다." else "즐겨찾기에서 해제되었습니다.",
                            Toast.LENGTH_SHORT
                        ).show()

                        // ✨ 3. 이전 화면(ExerciseListFragment)에 상태가 변경되었음을 알립니다.
                        // 이렇게 하면 뒤로 갔을 때 목록의 별 모양이 바로 갱신됩니다.
                        parentFragmentManager.setFragmentResult("favorite_status_updated", Bundle().apply {
                            putLong("exerciseId", currentExercise.id)
                            putBoolean("isFavorite", newFavoriteStatus)
                        })
                    }
                } else {
                    // 서버 통신 실패 시 사용자에게 알립니다.
                    withContext(Dispatchers.Main) {
                        if (_binding != null) Toast.makeText(requireContext(), "즐겨찾기 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // 네트워크 오류 등 예외 발생 시 사용자에게 알립니다.
                withContext(Dispatchers.Main) {
                    if (_binding != null) Toast.makeText(requireContext(), "오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
