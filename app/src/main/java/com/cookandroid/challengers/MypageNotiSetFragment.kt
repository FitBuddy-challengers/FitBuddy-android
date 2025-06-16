package com.cookandroid.challengers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cookandroid.challengers.databinding.FragmentMypageNotiSetBinding
import com.cookandroid.challengers.util.UserPreference
import java.util.concurrent.TimeUnit

class MypageNotiSetFragment : Fragment() {

    private var _binding: FragmentMypageNotiSetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageNotiSetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userPref = UserPreference(requireContext())

        // 스위치 초기 상태
        binding.switchNotification.isChecked = userPref.isNotificationEnabled()

        // 뒤로 가기
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // 스위치 상태 변경 시 저장 및 WorkManager 설정
        binding.switchNotification.setOnCheckedChangeListener { _, isChecked ->
            userPref.setNotificationEnabled(isChecked)

            if (isChecked) {
                // 알림 예약
                val workRequest = PeriodicWorkRequestBuilder<InactivityWorker>(1, TimeUnit.DAYS).build()
                WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                    "InactivityCheckWork",
                    ExistingPeriodicWorkPolicy.UPDATE,  // 덮어쓰기
                    workRequest
                )
            } else {
                // 알림 취소
                WorkManager.getInstance(requireContext()).cancelUniqueWork("InactivityCheckWork")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}