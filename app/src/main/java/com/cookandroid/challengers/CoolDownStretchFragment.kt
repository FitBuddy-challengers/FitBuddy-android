package com.cookandroid.challengers

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.cookandroid.challengers.data.CoolDownStretch
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentCoolDownStretchBinding
import com.cookandroid.challengers.viewmodel.StopwatchViewModel
import kotlinx.coroutines.launch
import kotlin.collections.getOrNull
import android.content.Context // Context 임포트 추가

class CoolDownStretchFragment : Fragment() {

    private var _binding: FragmentCoolDownStretchBinding? = null
    private val binding get() = _binding!!

    private lateinit var stretchList: List<CoolDownStretch>
    private var currentIndex = 0
    private val stretchDuration = 20_000L // 20초
    private var timer: CountDownTimer? = null
    private var isPlaying = false

    private val stopwatchViewModel: StopwatchViewModel by activityViewModels()

    private var completionTimestamp: Long = 0L
    private var totalWorkoutDuration: Long = 0L

    // --- SharedPreferences 정의부 ---
    companion object {
        // ExerciseFragment와 동일한 키 값을 사용하거나, 이 프래그먼트만의 키를 정의할 수 있습니다.
        // 여기서는 ExerciseFragment와 동일한 키를 사용하여 운동 전체 진행 상태를 초기화한다고 가정합니다.
        private const val PREFS_PROGRESS = "exercise_progress"
        private const val KEY_IN_PROGRESS = "is_in_progress"
        private const val KEY_SAVED_PLAN_ID = "current_plan_id_prefs" // ExerciseFragment에서 사용하는 키
        private const val KEY_SAVED_EXERCISE_INDEX = "current_exercise_index_prefs"
        private const val KEY_SAVED_SCHEDULE_ID = "current_schedule_id_prefs"
        private const val KEY_SAVED_EXERCISE_ID = "current_exercise_id_prefs"
        // CoolDownStretchFragment에서 직접 사용하지 않더라도, clear 시 필요할 수 있는 키들
        private const val KEY_SET_INDEX = "current_set_index"
        private const val KEY_START_TIME = "start_time"
        private const val KEY_ELAPSED_TIME = "stopwatch_elapsed_time_prefs"
        private const val KEY_EXERCISE_NAME = "current_exercise_name_prefs"
        private const val KEY_IMAGE_PATH = "current_image_path_prefs"
        private const val KEY_EQUIP = "current_equip_prefs"
    }

    private val prefs by lazy {
        requireContext().getSharedPreferences(PREFS_PROGRESS, Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // arguments로부터 데이터 수신
        arguments?.let {
            completionTimestamp = it.getLong("completion_time_millis", 0L)
            totalWorkoutDuration = it.getLong("total_duration_millis", 0L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCoolDownStretchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
        viewLifecycleOwner.lifecycleScope.launch {
            stretchList = db.coolDownStretchDao().getAllStretches()
            if (stretchList.isNotEmpty()) {
                showStretch(currentIndex)
                setupButtons()
            } else {
                Log.e("CoolDownStretchFragment", "스트레칭 목록이 비어있습니다.")
                Toast.makeText(requireContext(), "표시할 스트레칭 정보가 없습니다.", Toast.LENGTH_LONG).show()
                try {
                    findNavController().navigate(R.id.action_coolDownStretch_to_home)
                } catch (e: Exception) {
                    Log.e("CoolDownStretchFragment", "네비게이션 오류 (홈으로 이동 실패)", e)
                    if (findNavController().previousBackStackEntry != null) {
                        findNavController().popBackStack()
                    }
                }
            }
            setupStopwatch()
        }

        binding.skipStretchButton.setOnClickListener {
            Toast.makeText(requireContext(), "스트레칭을 생략하고 운동을 완료합니다.", Toast.LENGTH_SHORT).show()
            completeWorkoutSession() // ★ 운동 완료 처리 함수 호출
        }
    }

    private fun setupStopwatch() {
        stopwatchViewModel.elapsedTime.observe(viewLifecycleOwner) { time ->
            _binding?.let {
                it.stopwatchTextView.text = stopwatchViewModel.formatElapsedTime(time)
            }
        }
        stopwatchViewModel.isRunning.observe(viewLifecycleOwner) { isRunning ->
            _binding?.let {
                it.pauseButton.setImageResource(
                    if (isRunning) R.drawable.ic_pause_black else R.drawable.ic_play_black
                )
            }
        }
    }

    private fun playStretchTimer() {
        if (!isAdded || _binding == null) return
        isPlaying = true
        binding.btnPlayPause.setImageResource(R.drawable.ic_pause_white)

        timer?.cancel()
        timer = object : CountDownTimer(stretchDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (!isAdded || _binding == null) {
                    this.cancel()
                    return
                }
                val seconds = millisUntilFinished / 1000
                binding.timerText.text = String.format("00:%02d", seconds)
            }

            override fun onFinish() {
                if (!isAdded || _binding == null) return
                isPlaying = false
                binding.btnPlayPause.setImageResource(R.drawable.ic_play_white)
                goToNext()
            }
        }.start()
    }

    private fun pauseStretchTimer() {
        isPlaying = false
        timer?.cancel()
        if (isAdded && _binding != null) {
            binding.btnPlayPause.setImageResource(R.drawable.ic_play_white)
        }
    }

    private fun setupButtons() {
        if (!isAdded || _binding == null) return
        binding.btnPlayPause.setOnClickListener {
            if (isPlaying) pauseStretchTimer() else playStretchTimer()
        }
        binding.btnNext.setOnClickListener {
            goToNext()
        }
        binding.btnPrev.setOnClickListener {
            if (currentIndex > 0) goTo(currentIndex - 1)
        }
        binding.backButton.setOnClickListener {
            if (findNavController().previousBackStackEntry != null) {
                findNavController().popBackStack()
            }
        }
    }

    private fun showStretch(index: Int) {
        if (!isAdded || _binding == null) return
        if (stretchList.isEmpty() || index < 0 || index >= stretchList.size) {
            Log.e("CoolDownStretchFragment", "showStretch: Invalid index or empty list. Index: $index, Size: ${stretchList.size}")
            Toast.makeText(requireContext(), "스트레칭 정보를 표시할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val stretch = stretchList[index]
        binding.exerciseNameTextView.text = stretch.name

        stretch.imagePath?.let {
            val resId = try {
                resources.getIdentifier(it, "drawable", requireContext().packageName)
            } catch (e: Exception) { 0 }

            Glide.with(requireContext())
                .load(if (resId != 0) resId else R.drawable.ic_fitbuddy_logo)
                .transition(withCrossFade())
                .placeholder(R.drawable.ic_fitbuddy_logo)
                .error(R.drawable.ic_fitbuddy_logo)
                .into(binding.stretchImageView)
        } ?: run {
            binding.stretchImageView.setImageResource(R.drawable.ic_fitbuddy_logo)
        }

        binding.timerText.text = "00:20"
        binding.stretchNumber.text = "${index + 1}/${stretchList.size}"
        updateNextText()
    }

    // 유선 10.18 수정: 마지막 스트레칭에서 '다음 스트레칭' 안 보이게
    private fun updateNextText() {
        if (!isAdded || _binding == null) return

        // 현재 스트레칭이 마지막 항목인지 확인
        if (currentIndex >= stretchList.size - 1) {
            binding.nextStretch.visibility = View.GONE
        } else {
            binding.nextStretch.visibility = View.VISIBLE

            // 다음 스트레칭 객체를 가져옵니다.
            val nextStretch = stretchList.getOrNull(currentIndex + 1)
            if (nextStretch == null) {
                // 예외 처리: 다음 스트레칭이 없는 경우 숨김
                binding.nextStretch.visibility = View.GONE
                return
            }

            // 1. 다음 스트레칭 이름 설정
            binding.nextStretchName.text = nextStretch.name

            // 2. 다음 스트레칭 이미지 설정
            nextStretch.imagePath?.let { path ->
                val resId = try {
                    resources.getIdentifier(path, "drawable", requireContext().packageName)
                } catch (e: Exception) { 0 }

                Glide.with(requireContext())
                    .load(if (resId != 0) resId else R.drawable.ic_fitbuddy_logo) // 이미지가 없으면 기본 로고 표시
                    .into(binding.nextStretchImageView) // 새로 추가한 ID 사용

            } ?: run {
                // 이미지 경로가 null인 경우 기본 로고 표시
                binding.nextStretchImageView.setImageResource(R.drawable.ic_fitbuddy_logo)
            }
        }
    }

    private fun goTo(index: Int) {
        if (!isAdded || _binding == null) return
        if (index >= 0 && index < stretchList.size) {
            currentIndex = index
            showStretch(index)
            pauseStretchTimer()
            playStretchTimer()
        } else {
            Log.w("CoolDownStretchFragment", "goTo: Invalid index $index, list size ${stretchList.size}")
        }
    }

    private fun goToNext() {
        if (!isAdded || _binding == null) return
        if (currentIndex < stretchList.size - 1) {
            goTo(currentIndex + 1)
        } else {
            timer?.cancel()
            isPlaying = false
            binding.btnPlayPause.setImageResource(R.drawable.ic_play_white)
            binding.timerText.text = "완료!"
            Toast.makeText(requireContext(), "오늘의 운동 완료!", Toast.LENGTH_SHORT).show()
            completeWorkoutSession() // ★ 운동 완료 처리 함수 호출
        }
    }

    // ★ 운동 세션 전체 완료 처리 함수 수정
    private fun completeWorkoutSession() {
        // SharedPreferences 정리는 ExerciseDoingFragment에서 이미 처리됨

        Log.d("CoolDownStretchFragment", "Cooldown session finished. Navigating to photo upload.")
        try {
            // 사진 인증 화면으로 네비게이션
            if (findNavController().currentDestination?.id == R.id.coolDownStretchFragment) {
                val args = Bundle().apply {
                    putLong("completion_time_millis", completionTimestamp)

                    //ViewModel에서 최신 운동 시간을 직접 가져와 전달
                    val finalWorkoutDuration = stopwatchViewModel.elapsedTime.value ?: totalWorkoutDuration
                    putLong("total_duration_millis", finalWorkoutDuration)
                }
                // 네비게이션 그래프에 정의된 실제 액션 ID로 변경해야 합니다.
                val actionId = R.id.action_coolDownStretch_to_challengeUpload
                findNavController().navigate(actionId, args)
                Log.i("CoolDownStretchFragment", "Navigating to ChallengeUploadPhotoFragment with duration: $totalWorkoutDuration")
            }
        } catch (e: Exception) {
            Log.e("CoolDownStretchFragment", "네비게이션 오류 (사진 업로드로 이동 실패)", e)
            // 실패 시 홈으로 이동하는 등의 예외 처리
            if (isAdded) findNavController().navigate(R.id.action_coolDownStretch_to_home)
        }
    }

    override fun onDestroyView() {
        timer?.cancel()
        timer = null
        _binding = null
        super.onDestroyView()
    }
}
