package com.cookandroid.challengers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.cookandroid.challengers.data.Exercise
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentExerciseDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExerciseDetailFragment : Fragment() {

    private var _binding: FragmentExerciseDetailBinding? = null
    private val binding get() = _binding!!

    private var exerciseId: Long = -1L
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
        exerciseId = arguments?.getLong(ARG_EXERCISE_ID) ?: -1L
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

        // 운동 데이터 가져오기
        lifecycleScope.launch(Dispatchers.IO) {
            val exercise = db.exerciseDao().getExerciseById(exerciseId)
            exercise?.let {
                currentExercise = it
                withContext(Dispatchers.Main) {
                    bindExerciseData(it)
                }
            }
        }

        // 뒤로 가기 버튼
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 즐겨찾기(북마크) 버튼 클릭 처리
        binding.favoriteButton.setOnClickListener {
            toggleFavorite()
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
        val newFavoriteStatus = !currentExercise.isFavorite
        currentExercise = currentExercise.copy(isFavorite = newFavoriteStatus)

        lifecycleScope.launch(Dispatchers.IO) {
            db.exerciseDao().update(currentExercise)
            withContext(Dispatchers.Main) {
                binding.favoriteButton.isSelected = newFavoriteStatus
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
