package com.cookandroid.challengers

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
import androidx.navigation.fragment.findNavController
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

        setupChart(weightChart)
        setupChart(fatChart)

        observeWeightRecords()
        setupPeriodToggleButtons()

        binding.fabAddWeight.setOnClickListener {
            RecordAddWeightFragment().show(parentFragmentManager, "AddWeight")
        }

    }

    private fun setupPeriodToggleButtons() {
        binding.btnWeekWeight.isChecked = true
        binding.btnWeekFat.isChecked = true

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
    }

    private fun updateWeightChartByPeriod(checkedId: Int) {
        if (_binding == null || isDetached) return
        val filteredRecords = filterRecordsByPeriod(checkedId)
        if (_binding != null && !isDetached) { // 추가: 뷰가 소멸되지 않았는지 확인
            updateChart(weightChart, filteredRecords, "kg", binding.tvWeightDateRange, binding.tvWeightAverage)
        }
    }

    private fun updateFatChartByPeriod(checkedId: Int) {
        if (_binding == null || isDetached) return
        val filteredRecords = filterRecordsByPeriod(checkedId)
        if (_binding != null && !isDetached) {  // 추가: 뷰가 소멸되지 않았는지 확인
            updateChart(fatChart, filteredRecords, "%", binding.tvFatDateRange, binding.tvFatAverage)
        }
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
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = false
        chart.isDragXEnabled = true
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
            if (_binding != null) { // 뷰가 null이 아닌 경우에만 텍스트 뷰 업데이트
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

        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                if (value >= 0 && value < sortedRecords.size) {
                    return sortedRecords[value.toInt()].date.format(dateFormatter)
                }
                return ""
            }
        }

        val minVal = entries.minByOrNull { it.y }?.y ?: 0f
        val maxVal = entries.maxByOrNull { it.y }?.y ?: 100f
        chart.axisLeft.axisMinimum = minVal * 0.9f
        chart.axisLeft.axisMaximum = maxVal * 1.1f

        chart.invalidate()

        if (_binding != null) { // 뷰가 null이 아닌 경우에만 텍스트 뷰 업데이트
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
