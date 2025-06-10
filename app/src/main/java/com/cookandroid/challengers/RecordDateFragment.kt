package com.cookandroid.challengers

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.databinding.FragmentRecordDateBinding
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.tabs.TabLayout
import kotlin.jvm.java

class RecordDateFragment : Fragment() {

    private var _binding: FragmentRecordDateBinding? = null
    private val binding get() = _binding!!

    private lateinit var recordDateViewModel: RecordDateViewModel // 이 화면의 통계 데이터용
    private lateinit var storeViewModel: StoreViewModel      // 아바타 이미지를 공유받을 용도

    private val bodyParts = listOf("가슴", "등", "하체", "어깨", "팔", "복근", "유산소")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordDateBinding.inflate(inflater, container, false)
        // ViewModel 인스턴스 생성
        recordDateViewModel = ViewModelProvider(this).get(RecordDateViewModel::class.java)

        // ★★★ Activity 범위의 공유 StoreViewModel 가져오기 ★★★
        storeViewModel = ViewModelProvider(requireActivity()).get(StoreViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChart()
        setupListeners()
        observeViewModel()

        // 초기 데이터 로드
        recordDateViewModel.loadRadarData("week")
    }

    private fun setupListeners() {
        binding.recordCalendarButton.setOnClickListener {
            findNavController().navigate(R.id.action_record_to_recordCalendar)
        }

        binding.periodExerciseTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val period = when (tab.position) {
                    0 -> "week"
                    1 -> "month"
                    2 -> "year"
                    else -> "all"
                }
                recordDateViewModel.loadRadarData(period)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeViewModel() {
        recordDateViewModel.summaryText.observe(viewLifecycleOwner) { text ->
            binding.topWorkoutTextView.text = text
        }
        recordDateViewModel.radarData.observe(viewLifecycleOwner) { dataMap ->
            if (dataMap != null) {
                updateRadarChart(dataMap)
            }
        }

        storeViewModel.characterBitmap.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                // 비트맵이 있으면 아바타 이미지로 설정
                binding.sivAvatar.setImageBitmap(bitmap)
            } else {
                // 비트맵이 없으면(초기 상태 등) 기본 샘플 이미지로 설정
                binding.sivAvatar.setImageResource(R.drawable.avartar_sample)
            }
        }
    }

    private fun setupChart() {
        binding.radarChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            rotationAngle = 0f
            isRotationEnabled = false
            isHighlightPerTapEnabled = false
            webLineWidth = 1f
            webColor = ContextCompat.getColor(requireContext(), R.color.gray)
            xAxis.apply {
                textSize = 13f
                textColor = ContextCompat.getColor(requireContext(), android.R.color.black)
                yOffset = 0f
                xOffset = 0f
                setDrawGridLines(false)
                setDrawAxisLine(true)
            }
            yAxis.apply {
                axisMinimum = 0f
                setDrawLabels(false)
                setDrawAxisLine(false)
                setDrawGridLines(false)
            }
        }
    }


    private fun updateRadarChart(partTimeMap: Map<String, Float>) {
        val entries = bodyParts.map { part -> RadarEntry(partTimeMap[part] ?: 0f) }

        val totalVolume = partTimeMap.values.sum()
        binding.radarChart.xAxis.valueFormatter = IndexAxisValueFormatter(
            bodyParts.map { part ->
                val percent = if (totalVolume > 0) ((partTimeMap[part] ?: 0f) / totalVolume * 100).toInt() else 0
                "$part\n$percent%" // "부위\n백분율%" 형태로 라벨 설정
            }
        )

        val dataSet = RadarDataSet(entries, "").apply {
            color = ContextCompat.getColor(requireContext(), R.color.blue)
            fillColor = ContextCompat.getColor(requireContext(), R.color.blue)
            setDrawFilled(true)
            setDrawValues(false)
            lineWidth = 2f
        }

        binding.radarChart.data = RadarData(dataSet)
        binding.radarChart.invalidate() // 차트 새로고침
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}