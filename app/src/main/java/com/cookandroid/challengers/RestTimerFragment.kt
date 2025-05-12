package com.cookandroid.challengers

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.util.concurrent.TimeUnit
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class RestTimerFragment : BottomSheetDialogFragment() { // 휴식 타이머

    companion object {
        const val TAG = "RestTimerFragment"
        private const val ARG_AUTO_START = "autoStart"
        // autoStart = true 면 바로 타이머 작동, false 면 멈춘 상태로 시작
        fun newInstance(autoStart: Boolean) = RestTimerFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_AUTO_START, autoStart)
            }
        }
    }


    private var autoStart = false
    private lateinit var buttonStartStop: Button
    private lateinit var buttonMinus10Seconds: Button
    private lateinit var buttonPlus10Seconds: Button
    private lateinit var progressBarTimer: CircularProgressIndicator
    private lateinit var textRemainingTime: TextView
    private lateinit var textSetTime: TextView
    private lateinit var layoutTimeOptions: LinearLayout

    // 설정된 시간
    private var selectedTime: Long = 30_000L
    private var initialTime: Long = 30_000L  // 기본 30초
    private var timer: CountDownTimer? = null
    private var onFinished: (() -> Unit)? = null
    private var isManuallySet = false // 버튼으로 시간을 설정했는지 여부 추적
    private var isTimerRunning = false

    // 완료 콜백
    fun setOnTimerFinishedListener(listener: () -> Unit) {
        onFinished = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        autoStart = arguments?.getBoolean(ARG_AUTO_START, false) ?: false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheet = (dialogInterface as BottomSheetDialog)
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            bottomSheet?.background = ContextCompat.getDrawable(requireContext(), R.drawable.bottom_sheet_background)

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                val desiredHeight = resources.getDimensionPixelSize(R.dimen.rest_timer_peek_height) // 정의할 고정 높이 dimen
                behavior.peekHeight = desiredHeight
                behavior.maxHeight = desiredHeight
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_rest_timer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뷰 바인딩
        progressBarTimer = view.findViewById(R.id.progressBarTimer)
        textRemainingTime = view.findViewById(R.id.textRemainingTime)
        textSetTime = view.findViewById(R.id.textSetTime)
        buttonMinus10Seconds = view.findViewById(R.id.buttonMinus10Seconds)
        buttonPlus10Seconds = view.findViewById(R.id.buttonPlus10Seconds)
        buttonStartStop = view.findViewById(R.id.buttonStartStop)
        layoutTimeOptions = view.findViewById(R.id.layoutTimeOptions)

        // 초기 UI
        updateTimerDisplay()
        updateStartStopButtonUI(running = autoStart && !isManuallySet) // 수동 설정 시 시작 안 함

        // 시간 옵션 버튼 클릭 리스너
        for (i in 0 until layoutTimeOptions.childCount) {
            val button = layoutTimeOptions.getChildAt(i) as Button
            button.setOnClickListener {
                val timeString = button.text.toString()
                selectedTime = timeStringToMilliseconds(timeString)
                initialTime = selectedTime
                updateTimerDisplay()
                updateTimeButtonStyles(button)
                isManuallySet = true // 버튼으로 시간 설정
                if (isTimerRunning) {
                    restartInternalTimer()
                }
            }
        }

        // 시간 조정 버튼
        buttonMinus10Seconds.setOnClickListener {
            selectedTime = (selectedTime - 10_000L).coerceAtLeast(1_000L)
            initialTime = selectedTime
            updateTimerDisplay()
            resetTimeButtonStyles() // 시간 변경 시 버튼 스타일 초기화
            isManuallySet = true // 버튼으로 시간 설정
            if (isTimerRunning) {
                restartInternalTimer()
            }
        }
        buttonPlus10Seconds.setOnClickListener {
            selectedTime += 10_000L
            initialTime = selectedTime
            updateTimerDisplay()
            resetTimeButtonStyles() // 시간 변경 시 버튼 스타일 초기화
            isManuallySet = true // 버튼으로 시간 설정
            if (isTimerRunning) {
                restartInternalTimer()
            }

        }

        // 시작/중단 버튼
        buttonStartStop.setOnClickListener {
            if (timer == null) {
                startInternalTimer()
                isManuallySet = false // 타이머 시작 후 수동 설정 상태 해제
            } else {
                stopInternalTimer()
                dismiss()
                onFinished?.invoke()
            }
        }

        // autoStart 시 바로 실행 (수동 설정이 아니었을 경우)
        if (autoStart && !isManuallySet) {
            startInternalTimer()
        }
    }

    private fun timeStringToMilliseconds(timeString: String): Long {
        val parts = timeString.split(":")
        val minutes = parts[0].toLong()
        val seconds = parts[1].toLong()
        return (minutes * 60 + seconds) * 1000L
    }

    private fun startInternalTimer() { // 타이머 시작
        isTimerRunning = true
        updateStartStopButtonUI(running = true)
        timer = object : CountDownTimer(selectedTime, 100L) {
            override fun onTick(millisUntilFinished: Long) {
                selectedTime = millisUntilFinished
                updateTimerDisplay()
            }

            override fun onFinish() {
                timer = null
                isTimerRunning = false
                dismiss()
                onFinished?.invoke()
            }
        }.start()
    }

    private fun stopInternalTimer() { // 타이머 중지
        timer?.cancel()
        timer = null
        isTimerRunning = false
        updateStartStopButtonUI(running = false)
    }

    private fun restartInternalTimer() { // 타이머 재시작
        stopInternalTimer()
        startInternalTimer()
    }

    private fun updateStartStopButtonUI(running: Boolean) { // 타이머 버튼
        buttonStartStop.text = if (running) "휴식 중단" else "휴식 시작"
        val tint = if (running)
            ContextCompat.getColor(requireContext(), R.color.red)
        else ContextCompat.getColor(requireContext(), R.color.blue)
        buttonStartStop.backgroundTintList = ColorStateList.valueOf(tint)
    }

    private fun updateTimerDisplay() {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(selectedTime)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(selectedTime) % 60
        textRemainingTime.text = String.format("%02d:%02d", minutes, seconds)

        textSetTime.text = String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(initialTime),
            TimeUnit.MILLISECONDS.toSeconds(initialTime) % 60
        )

        val progress = if (initialTime > 0)
            ((initialTime - selectedTime).toFloat() / initialTime.toFloat() * 100).toInt()
        else 100
        progressBarTimer.progress = progress
    }

    private fun updateTimeButtonStyles(selectedButton: Button) {
        resetTimeButtonStyles()
        selectedButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.blue))
        selectedButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
    }

    private fun resetTimeButtonStyles() {
        for (i in 0 until layoutTimeOptions.childCount) {
            val button = layoutTimeOptions.getChildAt(i) as Button
            button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), android.R.color.white))
            button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        }
    }
}