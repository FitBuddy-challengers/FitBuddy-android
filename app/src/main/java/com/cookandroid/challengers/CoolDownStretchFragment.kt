package com.cookandroid.challengers

import android.os.Bundle
import android.os.CountDownTimer
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

class CoolDownStretchFragment : Fragment() {

    private var _binding: FragmentCoolDownStretchBinding? = null
    private val binding get() = _binding!!

    private lateinit var stretchList: List<CoolDownStretch>
    private var currentIndex = 0
    private val stretchDuration = 20_000L // 20초
    private var timer: CountDownTimer? = null
    private var isPlaying = false

    private val stopwatchViewModel: StopwatchViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCoolDownStretchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
        viewLifecycleOwner.lifecycleScope.launch {
            stretchList = db.coolDownStretchDao().getAllStretches()
            showStretch(currentIndex)
            setupButtons()
            setupStopwatch()
        }

        binding.skipStretchButton.setOnClickListener {
            // 스트레칭 생략 버튼 클릭 시 운동 완료 처리
            Toast.makeText(requireContext(), "스트레칭을 생략하고 운동을 완료합니다.", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_coolDownStretch_to_home)
        // TODO: 실제 운동 완료 로직 (추후 구현)
        }
    }

    // ⏱️ 스톱워치: 운동 전체 시간
    private fun setupStopwatch() {
        stopwatchViewModel.elapsedTime.observe(viewLifecycleOwner) { time ->
            binding.stopwatchTextView.text = stopwatchViewModel.formatElapsedTime(time)
        }

        stopwatchViewModel.isRunning.observe(viewLifecycleOwner) { isRunning ->
            binding.pauseButton.setImageResource(
                if (isRunning) R.drawable.ic_pause_black else R.drawable.ic_play_black
            )
        }

        if (stopwatchViewModel.elapsedTime.value == 0L && stopwatchViewModel.isRunning.value == false) {
            stopwatchViewModel.startStopwatch()
        }

        binding.pauseButton.setOnClickListener {
            if (stopwatchViewModel.isRunning.value == true) {
                stopwatchViewModel.pauseStopwatch()
            } else {
                stopwatchViewModel.startStopwatch(stopwatchViewModel.elapsedTime.value ?: 0L)
            }
        }
    }

    // ▶️ 스트레칭 타이머: 20초
    private fun playStretchTimer() {
        isPlaying = true
        binding.btnPlayPause.setImageResource(R.drawable.ic_pause_white)

        timer?.cancel()
        timer = object : CountDownTimer(stretchDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.timerText.text = String.format("00:%02d", seconds)
            }

            override fun onFinish() {
                isPlaying = false
                binding.btnPlayPause.setImageResource(R.drawable.ic_play_white)
                goToNext()
            }
        }.start()
    }

    private fun pauseStretchTimer() {
        isPlaying = false
        timer?.cancel()
        binding.btnPlayPause.setImageResource(R.drawable.ic_play_white)
    }

    private fun setupButtons() {
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
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun showStretch(index: Int) {
        val stretch = stretchList[index]
        binding.exerciseNameTextView.text = stretch.name

        stretch.imagePath?.let {
            val resId = resources.getIdentifier(it, "drawable", requireContext().packageName)
            Glide.with(requireContext())
                .load(resId)
                .transition(withCrossFade())
                .placeholder(R.drawable.ic_launcher_background) // 로딩 중 표시할 이미지 (선택 사항)
                .error(R.drawable.ic_launcher_background)     // 에러 발생 시 표시할 이미지 (선택 사항)
                .into(binding.stretchImageView)
        } ?: run {
            // imagePath가 null인 경우 기본 이미지 설정 (선택 사항)
            binding.stretchImageView.setImageResource(R.drawable.ic_launcher_background)
        }

        binding.timerText.text = "00:20"
        binding.stretchNumber.text = "${index + 1}/${stretchList.size}"
        updateNextText()
    }

    private fun updateNextText() {
        val next = stretchList.getOrNull(currentIndex + 1)?.name
        binding.nextStretchName.text = next ?: "마지막 스트레칭"
    }

    private fun goTo(index: Int) {
        currentIndex = index
        showStretch(index)
        pauseStretchTimer()
        playStretchTimer()
    }

    private fun goToNext() {
        if (currentIndex < stretchList.size - 1) {
            goTo(currentIndex + 1)
        } else {
            Toast.makeText(requireContext(), "오늘의 운동 완료!", Toast.LENGTH_SHORT).show()
            //TODO: 운동 완료 이후
            //findNavController().navigate(R.id.action_coolDownStretch_to_home)
        }
    }

    override fun onDestroyView() {
        timer?.cancel()
        _binding = null
        super.onDestroyView()
    }
}
