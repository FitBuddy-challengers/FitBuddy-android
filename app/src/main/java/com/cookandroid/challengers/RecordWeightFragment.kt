package com.cookandroid.challengers

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.databinding.FragmentRecordWeightBinding
import com.cookandroid.challengers.api.RetrofitClient.WeightRecordDto
import com.cookandroid.challengers.viewmodel.RecordWeightViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.tabs.TabLayout
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.collections.filter
import kotlin.collections.sortedBy

class RecordWeightFragment : Fragment() {

    private var _binding: FragmentRecordWeightBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: RecordWeightViewModel

    private lateinit var weightChart: LineChart
    private lateinit var fatChart: LineChart
    private lateinit var skeletalMuscleChart: LineChart

    private val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")
    private val fullDateFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일", Locale.getDefault())

    private lateinit var highlightWeightDataSet: LineDataSet
    private lateinit var highlightFatDataSet: LineDataSet
    private lateinit var highlightMuscleDataSet: LineDataSet

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordWeightBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(RecordWeightViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(RecordWeightViewModel::class.java)

        weightChart = binding.weightChart
        fatChart = binding.fatChart
        skeletalMuscleChart = binding.muscleChart

        setupChart(weightChart)
        setupChart(fatChart)
        setupChart(skeletalMuscleChart)

        highlightWeightDataSet = createHighlightDataSet()
        highlightFatDataSet = createHighlightDataSet()
        highlightMuscleDataSet = createHighlightDataSet()

        setupInteractiveChart(weightChart, WeightMarkerView(requireContext()), highlightWeightDataSet)
        setupInteractiveChart(fatChart, FatMarkerView(requireContext()), highlightFatDataSet)
        setupInteractiveChart(skeletalMuscleChart, MuscleMarkerView(requireContext()), highlightMuscleDataSet)

        observeViewModel() // ★★★ DAO 대신 ViewModel 관찰 ★★★
        setupTabLayoutListeners()

        binding.fabAddWeight.setOnClickListener {
            if (isAdded) RecordAddWeightFragment().show(parentFragmentManager, "AddWeight")
        }
    }

    private fun observeViewModel() {
        viewModel.allRecords.observe(viewLifecycleOwner) { allRecords ->
            // 데이터가 변경될 때마다 현재 선택된 탭 기준으로 모든 차트를 다시 그림
            binding.periodWeightTabLayout.selectedTabPosition.let { pos ->
                updateChart(weightChart, filterRecords(allRecords, pos), "kg", binding.tvWeightDateRange, binding.tvWeightAverage, highlightWeightDataSet)
            }
            binding.periodFatTabLayout.selectedTabPosition.let { pos ->
                updateChart(fatChart, filterRecords(allRecords, pos), "%", binding.tvFatDateRange, binding.tvFatAverage, highlightFatDataSet)
            }
            binding.periodMuscleTabLayout.selectedTabPosition.let { pos ->
                updateChart(skeletalMuscleChart, filterRecords(allRecords, pos), "kg", binding.tvMuscleDateRange, binding.tvMuscleAverage, highlightMuscleDataSet)
            }
        }
    }

    private fun setupTabLayoutListeners() {
        val onTabSelectedListener = object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // 탭이 선택될 때마다 ViewModel에 저장된 최신 데이터로 차트를 다시 그림
                viewModel.allRecords.value?.let { allRecords ->
                    val position = tab?.position ?: return@let
                    when (tab.parent) {
                        binding.periodWeightTabLayout -> updateChart(weightChart, filterRecords(allRecords, position), "kg", binding.tvWeightDateRange, binding.tvWeightAverage, highlightWeightDataSet)
                        binding.periodFatTabLayout -> updateChart(fatChart, filterRecords(allRecords, position), "%", binding.tvFatDateRange, binding.tvFatAverage, highlightFatDataSet)
                        binding.periodMuscleTabLayout -> updateChart(skeletalMuscleChart, filterRecords(allRecords, position), "kg", binding.tvMuscleDateRange, binding.tvMuscleAverage, highlightMuscleDataSet)
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) { onTabSelected(tab) }
        }

        binding.periodWeightTabLayout.addOnTabSelectedListener(onTabSelectedListener)
        binding.periodFatTabLayout.addOnTabSelectedListener(onTabSelectedListener)
        binding.periodMuscleTabLayout.addOnTabSelectedListener(onTabSelectedListener)

        // 초기 탭을 '1개월'로 선택 (observeViewModel이 호출되면서 초기 차트가 그려짐)
        binding.periodWeightTabLayout.getTabAt(1)?.select()
        binding.periodFatTabLayout.getTabAt(1)?.select()
        binding.periodMuscleTabLayout.getTabAt(1)?.select()
    }

    private fun filterRecords(allRecords: List<WeightRecordDto>, tabPosition: Int): List<WeightRecordDto> {
        if (tabPosition == -1) return emptyList() // 선택된 탭이 없는 경우

        val now = LocalDate.now()
        val start = when (tabPosition) {
            0 -> now.minusDays(7)    // 1주
            1 -> now.minusMonths(1)  // 1개월
            2 -> now.minusYears(1)   // 1년
            else -> return allRecords // 전체
        }
        return allRecords.filter {
            val recordDate = LocalDate.parse(it.date) // String -> LocalDate로 변환
            !recordDate.isBefore(start) && !recordDate.isAfter(now)
        }
    }

    private fun updateChart(
        chart: LineChart,
        records: List<WeightRecordDto>,
        unit: String,
        dateRangeView: TextView,
        avgView: TextView,
        highlightSet: LineDataSet
    ) {
        if (records.isEmpty()) {
            chart.clear()
            chart.invalidate()
            dateRangeView.text = "기록 없음"
            avgView.text = "평균 -"
            return
        }

        val sorted = records.sortedBy { LocalDate.parse(it.date) }
        val entries = sorted.mapIndexed { i, r ->
            val y = when (chart) {
                weightChart -> r.weight.toFloat()
                fatChart -> r.bodyFatPercentage?.toFloat() ?: Float.NaN
                else -> r.skeletalMuscleMass?.toFloat() ?: Float.NaN
            }
            Entry(i.toFloat(), y)
        }.filter { !it.y.isNaN() }

        if (entries.isEmpty()) {
            chart.clear(); chart.invalidate()
            dateRangeView.text = "해당 기간 기록 없음"; avgView.text = "평균 -"
            return
        }

        val mainSet = LineDataSet(entries, unit).apply {
            color = ContextCompat.getColor(requireContext(), R.color.blue)
            setDrawCircles(true)
            circleRadius = 5f
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.blue))
            setDrawCircleHole(true)
            circleHoleRadius = 3f
            circleHoleColor = Color.WHITE
            setDrawValues(false)
            lineWidth = 2f
            setDrawHighlightIndicators(false)
        }

        chart.data = LineData(mainSet, highlightSet)

        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String =
                sorted.getOrNull(value.toInt())?.let { LocalDate.parse(it.date).format(dateFormatter) } ?: ""
        }
        chart.xAxis.setLabelCount(entries.size.coerceAtMost(5), false) // 라벨 개수 최대 5개로 제한

        val ys = entries.map { it.y }
        chart.axisLeft.axisMinimum = (ys.minOrNull() ?: 0f) * 0.9f
        chart.axisLeft.axisMaximum = (ys.maxOrNull() ?: 100f) * 1.1f
        chart.axisLeft.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(v: Float): String = String.format(Locale.getDefault(), "%.0f", v)
        }

        dateRangeView.text = "${LocalDate.parse(sorted.first().date).format(fullDateFormatter)} ~ ${LocalDate.parse(sorted.last().date).format(fullDateFormatter)}"
        if (ys.isNotEmpty()) {
            val avg = ys.average()
            avgView.text = String.format(Locale.getDefault(), "평균 %.1f %s", avg, unit)
        }

        chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupChart(chart: LineChart) {
        chart.apply {
            description.isEnabled = false

            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            legend.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                setDrawAxisLine(true)
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                setDrawAxisLine(false)
            }
            axisRight.isEnabled = false
        }
    }

    private fun createHighlightDataSet(): LineDataSet =
        LineDataSet(ArrayList(), "").apply {
            setDrawCircles(true)
            circleRadius = 6f
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.blue))
            setDrawCircleHole(false)
            setDrawValues(false)
            lineWidth = 0f
        }

    private fun setupInteractiveChart(
        chart: LineChart,
        marker: MarkerView,
        highlightSet: LineDataSet
    ) {
        chart.marker = marker
        chart.isHighlightPerTapEnabled = true
        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry, h: Highlight) {
                highlightSet.clear()
                highlightSet.addEntry(Entry(e.x, e.y))
                chart.data.notifyDataChanged()
                chart.notifyDataSetChanged()
                chart.highlightValue(h)
            }

            override fun onNothingSelected() {
                highlightSet.clear()
                chart.data.notifyDataChanged()
                chart.notifyDataSetChanged()
                chart.invalidate()
            }
        })
    }
}

// MarkerViews
class WeightMarkerView(context: Context) : MarkerView(context, R.layout.marker_view) {
    private val tvContent: TextView = findViewById(R.id.tvContent)
    override fun getOffset(): MPPointF = MPPointF(-(width / 2).toFloat(), -height.toFloat())
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let { tvContent.text = String.format(Locale.getDefault(), "체중: %.1fkg", it.y) }
        super.refreshContent(e, highlight)
    }
}

class FatMarkerView(context: Context) : MarkerView(context, R.layout.marker_view) {
    private val tvContent: TextView = findViewById(R.id.tvContent)
    override fun getOffset(): MPPointF = MPPointF(-(width / 2).toFloat(), -height.toFloat())
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let { tvContent.text = String.format(Locale.getDefault(), "체지방: %.1f%%", it.y) }
        super.refreshContent(e, highlight)
    }
}

class MuscleMarkerView(context: Context) : MarkerView(context, R.layout.marker_view) {
    private val tvContent: TextView = findViewById(R.id.tvContent)
    override fun getOffset(): MPPointF = MPPointF(-(width / 2).toFloat(), -height.toFloat())
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let { tvContent.text = String.format(Locale.getDefault(), "골격근량: %.1fkg", it.y) }
        super.refreshContent(e, highlight)
    }
}
