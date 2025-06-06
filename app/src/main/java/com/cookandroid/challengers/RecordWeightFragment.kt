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
import androidx.lifecycle.lifecycleScope
import com.cookandroid.challengers.data.WeightRecord
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentRecordWeightBinding
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    private val fullDateFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일", Locale.getDefault())

    // 하이라이트용 DataSet
    private lateinit var highlightWeightDataSet: LineDataSet
    private lateinit var highlightFatDataSet: LineDataSet
    private lateinit var highlightMuscleDataSet: LineDataSet

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

        // 차트 기본 설정
        setupChart(weightChart)
        setupChart(fatChart)
        setupChart(skeletalMuscleChart)

        // 하이라이트 DataSet 초기화
        highlightWeightDataSet = createHighlightDataSet()
        highlightFatDataSet = createHighlightDataSet()
        highlightMuscleDataSet = createHighlightDataSet()

        // 상호작용 설정
        setupInteractiveChart(weightChart, WeightMarkerView(requireContext()), highlightWeightDataSet)
        setupInteractiveChart(fatChart, FatMarkerView(requireContext()), highlightFatDataSet)
        setupInteractiveChart(skeletalMuscleChart, MuscleMarkerView(requireContext()), highlightMuscleDataSet)

        observeWeightRecords()
        setupTabLayoutListeners()

        binding.fabAddWeight.setOnClickListener {
            if (isAdded) RecordAddWeightFragment().show(parentFragmentManager, "AddWeight")
        }
    }

    private fun setupChart(chart: LineChart) {
        chart.apply {
            description.isEnabled = true
            description.textSize = 10f
            description.typeface = Typeface.DEFAULT_BOLD
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

    private fun createHighlightDataSet(): LineDataSet =
        LineDataSet(ArrayList(), "").apply {
            setDrawCircles(true)
            circleRadius = 6f
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.blue))
            setDrawCircleHole(false)
            setDrawValues(false)
            lineWidth = 0f
        }

    private fun updateChart(
        chart: LineChart,
        records: List<WeightRecord>,
        unit: String,
        dateRangeView: TextView,
        avgView: TextView,
        highlightSet: LineDataSet
    ) {
        if (records.isEmpty()) {
            chart.clear()
            chart.invalidate()
            dateRangeView.text = ""
            avgView.text = ""
            return
        }

        val sorted = records.sortedBy { it.date }
        val entries = sorted.mapIndexed { i, r ->
            val y = when (chart) {
                weightChart -> r.weight.toFloat()
                fatChart -> r.bodyFatPercentage?.toFloat() ?: Float.NaN
                else -> r.skeletalMuscleMass?.toFloat() ?: Float.NaN
            }
            Entry(i.toFloat(), y)
        }.filter { !it.y.isNaN() }

        // 메인 DataSet (hollow)
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

        // 데이터 적용
        chart.data = LineData(mainSet, highlightSet)

        // X축 라벨 설정
        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String =
                sorted.getOrNull(value.toInt())?.date?.format(dateFormatter) ?: ""
        }
        chart.xAxis.setLabelCount(entries.size, true)

        // Y축 범위 및 포맷
        val ys = entries.map { it.y }
        chart.axisLeft.axisMinimum = (ys.minOrNull() ?: 0f) * 0.9f
        chart.axisLeft.axisMaximum = (ys.maxOrNull() ?: 100f) * 1.1f
        chart.axisLeft.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(v: Float): String = String.format(Locale.getDefault(), "%.0f", v)
        }

        // 날짜 범위 및 평균
        dateRangeView.text =
            "${sorted.first().date.format(fullDateFormatter)} ~ ${sorted.last().date.format(fullDateFormatter)}"
        if (ys.isNotEmpty()) {
            val avg = ys.average()
            avgView.text = String.format(Locale.getDefault(), "평균 %.1f %s", avg, unit)
        }

        chart.invalidate()
    }

    private fun setupTabLayoutListeners() {
        binding.periodWeightTabLayout.getTabAt(1)?.select()
        binding.periodFatTabLayout.getTabAt(1)?.select()
        binding.periodMuscleTabLayout.getTabAt(1)?.select()

        binding.periodWeightTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let {
                    updateChart(
                        weightChart,
                        filterRecords(it),
                        "kg",
                        binding.tvWeightDateRange,
                        binding.tvWeightAverage,
                        highlightWeightDataSet
                    )
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) { onTabSelected(tab) }
        })

        binding.periodFatTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let {
                    updateChart(
                        fatChart,
                        filterRecords(it),
                        "%",
                        binding.tvFatDateRange,
                        binding.tvFatAverage,
                        highlightFatDataSet
                    )
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) { onTabSelected(tab) }
        })

        binding.periodMuscleTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let {
                    updateChart(
                        skeletalMuscleChart,
                        filterRecords(it),
                        "kg",
                        binding.tvMuscleDateRange,
                        binding.tvMuscleAverage,
                        highlightMuscleDataSet
                    )
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) { onTabSelected(tab) }
        })
    }

    private fun filterRecords(tabPosition: Int): List<WeightRecord> {
        val now = LocalDate.now()
        val start = when (tabPosition) {
            0 -> now.minusDays(7)
            1 -> now.minusMonths(1)
            2 -> now.minusYears(1)
            else -> return weightRecords
        }
        return weightRecords.filter { !it.date.isBefore(start) && !it.date.isAfter(now) }
    }

    private fun observeWeightRecords() {
        lifecycleScope.launch(Dispatchers.IO) {
            db.weightRecordDao().getAllRecords().collectLatest { recs ->
                withContext(Dispatchers.Main) {
                    weightRecords.clear()
                    weightRecords.addAll(recs)
                    // 초기 탭 적용
                    binding.periodWeightTabLayout.getTabAt(1)?.select()
                    binding.periodFatTabLayout.getTabAt(1)?.select()
                    binding.periodMuscleTabLayout.getTabAt(1)?.select()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
