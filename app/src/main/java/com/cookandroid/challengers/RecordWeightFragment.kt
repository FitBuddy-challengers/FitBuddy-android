package com.cookandroid.challengers

import android.graphics.Color // Color 클래스 임포트
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cookandroid.challengers.data.WeightRecord
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentRecordWeightBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class RecordWeightFragment : Fragment() {

    private var _binding: FragmentRecordWeightBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var weightChart: LineChart
    private lateinit var fatChart: LineChart
    private lateinit var skeletalMuscleChart: LineChart
    private var weightRecords: MutableList<WeightRecord> = mutableListOf()
    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")
    private val fullDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordWeightBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
        weightChart = binding.weightChart
        fatChart = binding.fatChart
        skeletalMuscleChart = binding.muscleChart

        setupChart(weightChart)
        setupChart(fatChart)
        setupChart(skeletalMuscleChart)

        observeWeightRecords()
        setupPeriodToggleButtons()

        binding.fabAddWeight.setOnClickListener {
            RecordAddWeightFragment().show(parentFragmentManager, "AddWeight")
        }
    }

    private fun setupPeriodToggleButtons() {
        binding.btnWeekWeight.isChecked = true
        binding.btnWeekFat.isChecked = true
        binding.btnWeekMuscle.isChecked = true

        binding.periodToggleWeight.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                updateWeightChartByPeriod(checkedId)
            }
        }
        binding.periodToggleFat.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                updateFatChartByPeriod(checkedId)
            }
        }
        binding.periodToggleMuscle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                updateSkeletalMuscleChartByPeriod(checkedId)
            }
        }
    }

    private fun updateWeightChartByPeriod(checkedId: Int) {
        if (_binding == null || isDetached) return
        val filteredRecords = filterRecordsByPeriod(checkedId)
        if (_binding != null && !isDetached) {
            updateChart(weightChart, filteredRecords, "kg", binding.tvWeightDateRange, binding.tvWeightAverage)
        }
    }

    private fun updateFatChartByPeriod(checkedId: Int) {
        if (_binding == null || isDetached) return
        val filteredRecords = filterRecordsByPeriod(checkedId)
        if (_binding != null && !isDetached) {
            updateChart(fatChart, filteredRecords, "%", binding.tvFatDateRange, binding.tvFatAverage)
        }
    }

    private fun updateSkeletalMuscleChartByPeriod(checkedId: Int) {
        if (_binding == null || isDetached) return
        val filteredRecords = filterRecordsByPeriod(checkedId)
        if (_binding != null && !isDetached) {
            updateChart(skeletalMuscleChart, filteredRecords, "kg", binding.tvMuscleDateRange, binding.tvMuscleAverage)
        }
    }

    private fun filterRecordsByPeriod(checkedId: Int): List<WeightRecord> {
        val now = LocalDate.now()
        return when (checkedId) {
            binding.btnWeekWeight.id, binding.btnWeekFat.id, binding.btnWeekMuscle.id -> {
                val startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                weightRecords.filter { !it.date.isBefore(startOfWeek) && !it.date.isAfter(now) }
            }
            binding.btnMonthWeight.id, binding.btnMonthFat.id, binding.btnMonthMuscle.id -> {
                val startOfMonth = now.withDayOfMonth(1)
                weightRecords.filter { !it.date.isBefore(startOfMonth) && !it.date.isAfter(now) }
            }
            binding.btnYearWeight.id, binding.btnYearFat.id, binding.btnYearMuscle.id -> {
                val startOfYear = now.withDayOfYear(1)
                weightRecords.filter { !it.date.isBefore(startOfYear) && !it.date.isAfter(now) }
            }
            binding.btnAllWeight.id, binding.btnAllFat.id, binding.btnAllMuscle.id -> weightRecords
            else -> weightRecords
        }
    }

    private fun setupChart(chart: LineChart) {
        chart.description.isEnabled = false // 설명 비활성화
        chart.setTouchEnabled(true) // 터치 가능
        chart.isDragEnabled = true // 드래그 가능
        chart.setScaleEnabled(false) // 확대/축소 불가
        chart.setPinchZoom(false) // 핀치 줌 불가 (확대/축소 불가와 동일)
        chart.xAxis.position = XAxis.XAxisPosition.TOP_INSIDE // X축 레이블을 그래프 상단에 표시
        chart.xAxis.setDrawGridLines(false) // X축 그리드 라인 비활성화
        chart.axisRight.isEnabled = false // 오른쪽 Y축 비활성화
        chart.legend.isEnabled = false // 범례 비활성화
        chart.isDragXEnabled = true // X축(좌우) 이동 가능
        chart.axisLeft.setDrawGridLines(false) // 왼쪽 Y축 그리드 라인 비활성화 (이미지처럼 배경이 흰색 그리드 없도록)
        chart.xAxis.setDrawAxisLine(false) // X축 라인 비활성화 (이미지처럼 X축 라인 없음)
        chart.axisLeft.setDrawAxisLine(false) // Y축 라인 비활성화 (이미지처럼 Y축 라인 없음)
        chart.setNoDataText("") // 데이터 없을 때 텍스트 표시 안 함
    }

    private fun updateChart(
        chart: LineChart,
        records: List<WeightRecord>,
        unit: String,
        dateRangeTextView: TextView,
        averageTextView: TextView
    ) {
        if (isDetached) return

        if (records.isEmpty()) {
            chart.clear()
            chart.invalidate()
            if (_binding != null) {
                dateRangeTextView.text = ""
                averageTextView.text = ""
            }
            return
        }

        val sortedRecords = records.sortedBy { it.date }
        val entries = ArrayList<Entry>()
        for (i in sortedRecords.indices) {
            val value = when (chart.id) {
                binding.weightChart.id -> sortedRecords[i].weight.toFloat()
                binding.fatChart.id -> sortedRecords[i].bodyFatPercentage?.toFloat() ?: Float.NaN
                binding.muscleChart.id -> sortedRecords[i].skeletalMuscleMass?.toFloat() ?: Float.NaN
                else -> Float.NaN
            }
            if (!value.isNaN()) {
                entries.add(Entry(i.toFloat(), value))
            }
        }

        val dataSet = LineDataSet(entries, unit)
        dataSet.color = resources.getColor(android.R.color.holo_blue_light, null) // 라인 색상
        dataSet.setCircleColor(resources.getColor(android.R.color.holo_blue_dark, null)) // 원 내부 색상
        dataSet.setDrawCircles(true) // 원 그리기
        dataSet.setDrawValues(false) // 값 표시 안 함
        dataSet.circleRadius = 5f // 원 크기
        dataSet.circleHoleColor = Color.WHITE // 원 내부의 구멍 색상을 흰색으로 설정
        dataSet.circleHoleRadius = 3f // 원 내부 구멍 크기 (테두리를 만들기 위해)
        dataSet.setDrawFilled(false) // 그래프 아래 채우기 비활성화
        dataSet.lineWidth = 2f // 라인 두께

        val lineData = LineData(dataSet)
        chart.data = lineData

        // X축 값 포맷터 (날짜)
        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                if (value >= 0 && value < sortedRecords.size) {
                    return sortedRecords[value.toInt()].date.format(dateFormatter)
                }
                return ""
            }
        }
        chart.xAxis.setLabelCount(entries.size, true) // 모든 날짜 레이블이 표시되도록 강제

        // Y축 범위 조정
        val minVal = entries.minByOrNull { it.y }?.y ?: 0f
        val maxVal = entries.maxByOrNull { it.y }?.y ?: 100f
        chart.axisLeft.axisMinimum = minVal * 0.9f
        chart.axisLeft.axisMaximum = maxVal * 1.1f

        chart.invalidate() // 차트 갱신

        if (_binding != null) {
            val firstDate = sortedRecords.first().date.format(fullDateFormatter)
            val lastDate = sortedRecords.last().date.format(fullDateFormatter)
            dateRangeTextView.text = "$firstDate ~ $lastDate"

            if (entries.isNotEmpty()) {
                val average = entries.sumOf { it.y.toDouble() } / entries.size
                averageTextView.text = String.format("평균 %.1f %s", average, unit)
            } else {
                averageTextView.text = ""
            }
        }
    }

    private fun observeWeightRecords() {
        lifecycleScope.launch(Dispatchers.IO) {
            db.weightRecordDao().getAllRecords().collectLatest { records ->
                withContext(Dispatchers.Main) {
                    if (_binding != null && !isDetached) {
                        weightRecords.clear()
                        weightRecords.addAll(records)
                        updateWeightChartByPeriod(binding.periodToggleWeight.checkedButtonId)
                        updateFatChartByPeriod(binding.periodToggleFat.checkedButtonId)
                        updateSkeletalMuscleChartByPeriod(binding.periodToggleMuscle.checkedButtonId)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}