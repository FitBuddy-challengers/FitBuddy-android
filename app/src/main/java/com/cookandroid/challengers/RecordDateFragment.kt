package com.cookandroid.challengers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.data.ExerciseDao
import com.cookandroid.challengers.data.ExerciseSetDao
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentRecordDateBinding
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class WorkoutTimeByPart(val part: String, val totalTimeMillis: Long)
data class MostFrequentExercise(val name: String, val exerciseCount: Int)
data class MostFrequentWorkoutPart(val part: String, val exerciseCount: Int)

class RecordDateFragment : Fragment() {

    private lateinit var radarChart: RadarChart
    private val bodyParts = listOf("가슴", "등", "하체", "어깨", "복근", "유산소")
    private var _binding: FragmentRecordDateBinding? = null
    private val binding get() = _binding!!
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var setDao: ExerciseSetDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRecordDateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        radarChart = binding.radarChart
        val db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
        exerciseDao = db.exerciseDao()
        setDao = db.exerciseSetDao()

        loadMostFrequentWorkoutInfo()
        loadRadarChartData("month") // 초기 로드: 이번 달 데이터

        binding.recordCalendarButton.setOnClickListener {
            findNavController().navigate(R.id.action_record_to_recordCalendar)
        }

        binding.periodToggleWeight.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                val period = when (checkedId) {
                    R.id.btnWeekDate -> "week"
                    R.id.btnMonthDate -> "month"
                    R.id.btnYearDate -> "year"
                    R.id.btnAllDate -> "all"
                    else -> "month"
                }
                loadRadarChartData(period)
            }
        }
    }

    private fun loadMostFrequentWorkoutInfo() {
        lifecycleScope.launch {
            val mostFrequentPart = withContext(Dispatchers.IO) {
                setDao.getMostFrequentWorkoutPartThisMonth()
            }
            val mostFrequentExercise = withContext(Dispatchers.IO) {
                setDao.getMostFrequentExerciseThisMonth()
            }

            val userName = "김슈니" // 실제 사용자 이름으로 변경해야 합니다.

            binding.topWorkoutTextView.text = if (mostFrequentPart != null && mostFrequentExercise != null) {
                "${userName}님은 ${mostFrequentPart.part} 왕!\n이번 달 가장 많이 한 운동은 \n ${mostFrequentExercise.name}입니다"
            }else {
                "${userName}님, 이번 달 운동 기록이 없습니다."
            }
        }
    }

    private fun loadRadarChartData(period: String) {
        lifecycleScope.launch {
            val partTimeMap = withContext(Dispatchers.IO) {
                getWorkoutDataForPeriod(period)
            }

            val entries = bodyParts.map { part -> RadarEntry(partTimeMap[part] ?: 0f) }
            val dataSet = RadarDataSet(entries, "운동 점수")
            dataSet.color = ContextCompat.getColor(requireContext(), R.color.blue)
            dataSet.fillColor = ContextCompat.getColor(requireContext(), R.color.blue)
            dataSet.setDrawFilled(true)
            dataSet.valueTextSize = 14f

            val data = RadarData(dataSet)
            radarChart.data = data
            radarChart.xAxis.valueFormatter = IndexAxisValueFormatter(bodyParts)
            radarChart.description.isEnabled = false
            radarChart.invalidate()
        }
    }

    private suspend fun getWorkoutDataForPeriod(period: String): MutableMap<String, Float> =
        withContext(Dispatchers.IO) {
            val startDate: Long
            val endDate: Long
            val calendar = Calendar.getInstance()

            when (period) {
                "week" -> {
                    calendar.add(Calendar.DAY_OF_YEAR, -7)
                    startDate = calendar.timeInMillis
                    endDate = System.currentTimeMillis()
                }

                "month" -> {
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    startDate = calendar.timeInMillis
                    endDate = System.currentTimeMillis()
                }

                "year" -> {
                    calendar.set(Calendar.DAY_OF_YEAR, 1)
                    startDate = calendar.timeInMillis
                    endDate = System.currentTimeMillis()
                }

                "all" -> {
                    startDate = 0L // 모든 기록
                    endDate = System.currentTimeMillis()
                }

                else -> { // default is month
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    startDate = calendar.timeInMillis
                    endDate = System.currentTimeMillis()
                }
            }

            val partTimeMap = mutableMapOf<String, Float>()
            for (part in bodyParts) partTimeMap[part] = 0f

            val workoutTimes = setDao.getWorkoutTimeByPart(startDate, endDate)

            workoutTimes.forEach { workoutTime ->
                partTimeMap[workoutTime.part] = (partTimeMap[workoutTime.part]
                    ?: 0f) + (workoutTime.totalTimeMillis / 1000f / 60f ) * exerciseDao.getExerciseMets(
                    workoutTime.part
                )
            }
            return@withContext partTimeMap
        }
}
