package com.cookandroid.challengers.ui.record

import android.os.Bundle
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class RecordWeightFragment : Fragment() {

    private var _binding: FragmentRecordWeightBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var weightChart: LineChart
    private lateinit var fatChart: LineChart
    private var weightRecords: List<WeightRecord> = emptyList()

    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")

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

        setupChart(weightChart)
        setupChart(fatChart)

        observeWeightRecords()

        setupPeriodToggleButtons()
    }

    private fun setupPeriodToggleButtons() {
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
        // 초기 선택 설정 (1주)
        binding.btnWeekWeight.isChecked = true
        binding.btnWeekFat.isChecked = true
    }

    private fun updateWeightChartByPeriod(checkedId: Int) {
        val filteredRecords = filterRecordsByPeriod(checkedId)
        updateChart(weightChart, filteredRecords, "kg", binding.tvWeightDateRange, binding.tvWeightAverage)
    }

    private fun updateFatChartByPeriod(checkedId: Int) {
        val filteredRecords = filterRecordsByPeriod(checkedId)
        updateChart(fatChart, filteredRecords, "%", binding.tvFatDateRange, binding.tvFatAverage)
    }

    private fun filterRecordsByPeriod(checkedId: Int): List<WeightRecord> {
        val now = LocalDate.now()
        return when (checkedId) {
            binding.btnWeekWeight.id, binding.btnWeekFat.id -> {
                val startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                weightRecords.filter { !it.date.isBefore(startOfWeek) && !it.date.isAfter(now) }
            }
            binding.btnMonthWeight.id, binding.btnMonthFat.id -> {
                val startOfMonth = now.withDayOfMonth(1)
                weightRecords.filter { !it.date.isBefore(startOfMonth) && !it.date.isAfter(now) }
            }
            binding.btnYearWeight.id, binding.btnYearFat.id -> {
                val startOfYear = now.withDayOfYear(1)
                weightRecords.filter { !it.date.isBefore(startOfYear) && !it.date.isAfter(now) }
            }
            binding.btnAllWeight.id, binding.btnAllFat.id -> weightRecords
            else -> weightRecords
        }
    }

    private fun setupChart(chart: LineChart) {
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false
    }

    private fun updateChart(
        chart: LineChart,
        records: List<WeightRecord>,
        unit: String,
        dateRangeTextView: TextView,
        averageTextView: TextView
    ) {
        if (records.isEmpty()) {
            chart.clear()
            chart.invalidate()
            dateRangeTextView.text = ""
            averageTextView.text = ""
            return
        }

        val sortedRecords = records.sortedBy { it.date }
        val entries = ArrayList<Entry>()
        for (i in sortedRecords.indices) {
            val value = when (chart.id) {
                binding.weightChart.id -> sortedRecords[i].weight.toFloat()
                binding.fatChart.id -> sortedRecords[i].bodyFatPercentage?.toFloat() ?: Float.NaN
                else -> Float.NaN
            }
            if (!value.isNaN()) {
                entries.add(Entry(i.toFloat(), value))
            }
        }

        val dataSet = LineDataSet(entries, unit)
        dataSet.color = resources.getColor(android.R.color.holo_blue_light, null)
        dataSet.setCircleColor(resources.getColor(android.R.color.holo_blue_dark, null))
        dataSet.setDrawCircles(true)
        dataSet.setDrawValues(false)

        val lineData = LineData(dataSet)
        chart.data = lineData

        // X축 포맷터 설정 (날짜)
        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                if (value >= 0 && value < sortedRecords.size) {
                    return sortedRecords[value.toInt()].date.format(DateTimeFormatter.ofPattern("MM/dd"))
                }
                return ""
            }
        }

        // Y축 최소/최대 값 설정 (데이터 기반)
        val minVal = entries.minByOrNull { it.y }?.y ?: 0f
        val maxVal = entries.maxByOrNull { it.y }?.y ?: 100f // 기본 최댓값
        chart.axisLeft.axisMinimum = minVal * 0.9f // 약간의 패딩
        chart.axisLeft.axisMaximum = maxVal * 1.1f

        chart.invalidate()

        // 날짜 범위 표시
        val firstDate = sortedRecords.first().date.format(DateTimeFormatter.ofPattern("yy년 M월 d일"))
        val lastDate = sortedRecords.last().date.format(DateTimeFormatter.ofPattern("yy년 M월 d일"))
        dateRangeTextView.text = "$firstDate ~ $lastDate"

        // 평균 값 계산 및 표시
        if (entries.isNotEmpty()) {
            val average = entries.sumOf { it.y.toDouble() } / entries.size
            averageTextView.text = String.format("평균 %.1f %s", average, unit)
        } else {
            averageTextView.text = ""
        }
    }

    private fun observeWeightRecords() {
        lifecycleScope.launch(Dispatchers.IO) {
            db.weightRecordDao().getAllRecords().collectLatest { records -> // 여기에서 접근
                withContext(Dispatchers.Main) {
                    weightRecords = records
                    updateWeightChartByPeriod(binding.periodToggleWeight.checkedButtonId)
                    updateFatChartByPeriod(binding.periodToggleFat.checkedButtonId)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}