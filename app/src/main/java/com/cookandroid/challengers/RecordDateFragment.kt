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
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class WorkoutTimeByPart(val part: String, val totalTimeMillis: Long)
data class MostFrequentExercise(val name: String, val exerciseCount: Int)
data class MostFrequentWorkoutPart(val part: String, val exerciseCount: Int)











// 아래에 주석 처리된 코드로 사용하기 !!












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

        binding.periodExerciseTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> loadRadarChartData("week")
                    1 -> loadRadarChartData("month")
                    2 -> loadRadarChartData("year")
                    3 -> loadRadarChartData("all")
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
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

            val dataSet = RadarDataSet(entries, "").apply {
                color = ContextCompat.getColor(requireContext(), R.color.blue)
                fillColor = ContextCompat.getColor(requireContext(), R.color.blue)
                setDrawFilled(true)
                setDrawValues(false) // ✅ 수치 텍스트 제거
                lineWidth = 2f
            }

            radarChart.data = RadarData(dataSet)

            radarChart.description.isEnabled = false
            radarChart.legend.isEnabled = false
            radarChart.rotationAngle = 0f              // ✅ 회전 고정
            radarChart.isRotationEnabled = false       // ✅ 사용자 회전 방지

            // X축: "부위\n퍼센트%"로 표시
            radarChart.xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(
                    bodyParts.map { part ->
                        val percent = ((partTimeMap[part] ?: 0f) * 100).toInt()
                        "$part\n$percent%"
                    }
                )
                textSize = 13f
                yOffset = 0f
                xOffset = 0f
            }

            // Y축 스타일 제거
            radarChart.yAxis.apply {
                axisMinimum = 0f
                setDrawLabels(false)       // 숫자 제거
                setDrawAxisLine(false)     // 중심선 제거
                setDrawGridLines(false)    // 내부 원 제거
            }

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



//
//package com.cookandroid.challengers
//
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.view.ViewTreeObserver
//import androidx.core.content.ContextCompat
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.lifecycleScope
//import androidx.navigation.fragment.findNavController
//import com.cookandroid.challengers.data.ExerciseDao
//import com.cookandroid.challengers.data.ExerciseSetDao
//import com.cookandroid.challengers.data.db.AppDatabase
//import com.cookandroid.challengers.databinding.FragmentRecordDateBinding
//import com.github.mikephil.charting.charts.RadarChart
//import com.github.mikephil.charting.data.RadarData
//import com.github.mikephil.charting.data.RadarDataSet
//import com.github.mikephil.charting.data.RadarEntry
//import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
//import com.google.android.material.tabs.TabLayout // 이 임포트가 필요합니다.
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.text.SimpleDateFormat
//import java.util.*
//
//data class WorkoutTimeByPart(val part: String, val totalTimeMillis: Long)
//data class MostFrequentExercise(val name: String, val exerciseCount: Int)
//data class MostFrequentWorkoutPart(val part: String, val exerciseCount: Int)
//data class WorkoutTimeByExercise(val exerciseId: Long, val totalTimeMillis: Long)
//
//class RecordDateFragment : Fragment() {
//
//    private lateinit var radarChart: RadarChart
//    private val bodyParts = listOf("가슴", "등", "하체", "어깨", "복근", "유산소")
//    private var _binding: FragmentRecordDateBinding? = null
//    private val binding get() = _binding!!
//    private lateinit var exerciseDao: ExerciseDao
//    private lateinit var setDao: ExerciseSetDao
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
//    ): View? {
//        _binding = FragmentRecordDateBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        radarChart = binding.radarChart
//        val db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
//        exerciseDao = db.exerciseDao()
//        setDao = db.exerciseSetDao()
//
//        loadMostFrequentWorkoutInfo()
//        // ViewTreeObserver를 사용하여 레이아웃 완료 시점 감지
//        radarChart.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
//            override fun onGlobalLayout() {
//                // 레이아웃이 완료되고 뷰의 크기가 확정된 후 차트 데이터 로드
//                loadRadarChartData("week") // 초기 로드
//                // 리스너는 한 번만 필요하므로 제거
//                radarChart.viewTreeObserver.removeOnGlobalLayoutListener(this)
//            }
//        })
//
//        binding.recordCalendarButton.setOnClickListener {
//            findNavController().navigate(R.id.action_record_to_recordCalendar)
//        }
//
//        // TabLayout 리스너 설정
//        binding.periodExerciseTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
//            override fun onTabSelected(tab: TabLayout.Tab) {
//                when (tab.position) {
//                    0 -> loadRadarChartData("week")
//                    1 -> loadRadarChartData("month")
//                    2 -> loadRadarChartData("year")
//                    3 -> loadRadarChartData("all")
//                }
//            }
//
//            override fun onTabUnselected(tab: TabLayout.Tab?) {}
//            override fun onTabReselected(tab: TabLayout.Tab?) {}
//        })
//
//    }
//
//    private fun loadMostFrequentWorkoutInfo() {
//        lifecycleScope.launch {
//            val mostFrequentPart = withContext(Dispatchers.IO) {
//                setDao.getMostFrequentWorkoutPartThisMonth()
//            }
//            val mostFrequentExercise = withContext(Dispatchers.IO) {
//                setDao.getMostFrequentExerciseThisMonth()
//            }
//
//            val userName = "김슈니" // 실제 사용자 이름으로 변경해야 합니다.
//
//            binding.topWorkoutTextView.text = if (mostFrequentPart != null && mostFrequentExercise != null) {
//                val exerciseNameWithParticle = appendObjectParticle(mostFrequentExercise.name)
//                "${userName}님은 ${mostFrequentPart.part} 왕!\n이번 달, ${exerciseNameWithParticle} 가장 많이 하셨어요"
//            } else {
//                "${userName}님, 이번 달 운동 기록이 없습니다."
//            }
//
//        }
//    }
//
//    // 을/를 처리 함수
//    private fun appendObjectParticle(word: String): String {
//        if (word.isEmpty()) return word
//        val lastChar = word.last()
//        val hasFinalConsonant = (lastChar.code - 0xAC00) % 28 != 0
//        return word + if (hasFinalConsonant) "을" else "를"
//    }
//
//
//    private fun loadRadarChartData(period: String) {
//        lifecycleScope.launch {
//            val partTimeMap = withContext(Dispatchers.IO) {
//                getWorkoutDataForPeriod(period)
//            }
//
//            Log.d("RadarChart", "최종 계산된 partTimeMap: $partTimeMap")
//
//            val entries = bodyParts.map { part -> RadarEntry(partTimeMap[part] ?: 0f) }
//
//            val dataSet = RadarDataSet(entries, "").apply {
//                color = ContextCompat.getColor(requireContext(), R.color.blue) // 데이터셋의 선 색상 (파란색 채워진 영역의 외곽선)
//                fillColor = ContextCompat.getColor(requireContext(), R.color.blue) // 채워질 영역의 색상
//                setDrawFilled(true) // 영역 채우기 활성화
//                setDrawValues(false) // 수치 텍스트 제거
//                lineWidth = 2f // 데이터셋 라인 두께 (파란색 채워진 영역의 외곽선)
//            }
//
//            radarChart.data = RadarData(dataSet)
//
//            radarChart.description.isEnabled = false
//            radarChart.legend.isEnabled = false
//            radarChart.rotationAngle = 0f              // 회전 고정
//            radarChart.isRotationEnabled = false       // 사용자 회전 방지
//            radarChart.isHighlightPerTapEnabled = false // 노란 십자선 제거 (탭 시 하이라이트 비활성화)
//
//            // *** 그리드 및 웹라인 설정 ***
//            // radarChart.webLineWidth와 webColor는 모든 웹 라인의 공통 스타일을 정의합니다.
//            // Y축 그리드를 숨기려면 Y축에서 setDrawGridLines(false)를 명시적으로 설정해야 합니다.
//            radarChart.webLineWidth = 1f // 그리드 선 두께 (X축 방사형 그리드 및 가장 바깥쪽 육각형 선에 적용)
//            radarChart.webColor = ContextCompat.getColor(requireContext(), R.color.gray) // 그리드 선 색상
//
//
//            // X축 설정 (방사형 축 라벨과 그 주변)
//            radarChart.xAxis.apply {
//                valueFormatter = IndexAxisValueFormatter(
//                    bodyParts.map { part ->
//                        val totalValue = partTimeMap.values.sum()
//                        val percent = if (totalValue > 0) ((partTimeMap[part] ?: 0f) / totalValue * 100).toInt() else 0
//                        "$part\n$percent%" // 한 줄로 표시
//                    }
//                )
//                textSize = 13f
//                textColor = ContextCompat.getColor(requireContext(), android.R.color.black)
//                yOffset = 0f
//                xOffset = 0f
//                setDrawGridLines(false)    // **X축의 방사형 그리드 선을 보이게 합니다.**
//                setDrawAxisLine(true)     // **육각형 가장 바깥 선 (X축 라벨을 연결하는 선)을 보이게 합니다.**
//            }
//
//            // Y축 설정 (내부 원형 그리드 선 및 라벨)
//            radarChart.yAxis.apply {
//                axisMinimum = 0f
//                setDrawLabels(false)       // 숫자 라벨 제거
//                setDrawAxisLine(false)     // Y축 중심선 (원형 축 라인) 제거
//                setDrawGridLines(false)    // **Y축의 원형 그리드 선을 안 보이게 합니다.**
//            }
//
//            // 레이아웃이 완료된 후 invalidate() 호출하여 초기 로드 시 크기 문제 해결
//            radarChart.post {
//                radarChart.notifyDataSetChanged() // 데이터 변경 알림
//                radarChart.invalidate()
//            }
//        }
//    }
//
//
//    private suspend fun getWorkoutDataForPeriod(period: String): MutableMap<String, Float> =
//        withContext(Dispatchers.IO) {
//            val startDate: Long
//            val endDate: Long
//            val calendar = Calendar.getInstance()
//
//            when (period) {
//                "week" -> {
//                    calendar.add(Calendar.DAY_OF_YEAR, -7)
//                    startDate = calendar.timeInMillis
//                    endDate = System.currentTimeMillis()
//                }
//
//                "month" -> {
//                    calendar.set(Calendar.DAY_OF_MONTH, 1)
//                    calendar.set(Calendar.HOUR_OF_DAY, 0)
//                    calendar.set(Calendar.MINUTE, 0)
//                    calendar.set(Calendar.SECOND, 0)
//                    calendar.set(Calendar.MILLISECOND, 0)
//                    startDate = calendar.timeInMillis
//                    endDate = System.currentTimeMillis()
//                }
//
//                "year" -> {
//                    calendar.set(Calendar.DAY_OF_YEAR, 1)
//                    calendar.set(Calendar.HOUR_OF_DAY, 0)
//                    calendar.set(Calendar.MINUTE, 0)
//                    calendar.set(Calendar.SECOND, 0)
//                    calendar.set(Calendar.MILLISECOND, 0)
//                    startDate = calendar.timeInMillis
//                    endDate = System.currentTimeMillis()
//                }
//
//                "all" -> {
//                    startDate = 0L // 모든 기록
//                    endDate = System.currentTimeMillis()
//                }
//
//                else -> { // default is month
//                    calendar.set(Calendar.DAY_OF_MONTH, 1)
//                    calendar.set(Calendar.HOUR_OF_DAY, 0)
//                    calendar.set(Calendar.MINUTE, 0)
//                    calendar.set(Calendar.SECOND, 0)
//                    calendar.set(Calendar.MILLISECOND, 0)
//                    startDate = calendar.timeInMillis
//                    endDate = System.currentTimeMillis()
//                }
//            }
//
//            Log.d("RadarChart", "기간: $period, 시작일: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(startDate))}, 종료일: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(endDate))}")
//
//            val partTimeMap = mutableMapOf<String, Float>()
//            for (part in bodyParts) partTimeMap[part] = 0f // 모든 부위 초기화
//
//            val workoutTimesByExercise = setDao.getWorkoutTimesByExerciseId(startDate, endDate)
//            Log.d("RadarChart", "DB에서 가져온 운동 시간 (운동 ID별): $workoutTimesByExercise")
//
//            workoutTimesByExercise.forEach { entry ->
//                val exercise = exerciseDao.getExerciseById(entry.exerciseId)
//                if (exercise != null) {
//                    val exerciseParts = exercise.part?.split(",")?.map { it.trim() } ?: emptyList()
//                    val mets = exercise.mets.toFloat()
//
//                    Log.d("RadarChart", "처리 중인 운동: ID=${exercise.id}, 이름=${exercise.name}, 부위=${exerciseParts}, METs=${mets}, 총 시간(밀리초)=${entry.totalTimeMillis}")
//
//                    if (mets > 0 && entry.totalTimeMillis > 0 && exerciseParts.isNotEmpty()) {
//                        val baseValuePerPart = (entry.totalTimeMillis / 1000f / 60f) * mets / exerciseParts.size
//
//                        exerciseParts.forEach { part ->
//                            if (bodyParts.contains(part)) {
//                                partTimeMap[part] = (partTimeMap[part] ?: 0f) + baseValuePerPart
//                                Log.d("RadarChart", "  -> '$part' 부위에 $baseValuePerPart 값 추가. 현재 '$part' 총계: ${partTimeMap[part]}")
//                            } else {
//                                Log.w("RadarChart", "  -> 경고: 운동 부위 '$part'가 레이더 차트의 bodyParts 목록에 없습니다.")
//                            }
//                        }
//                    } else {
//                        Log.d("RadarChart", "  -> 운동 계산 건너뜀 (${exercise.name}): METs 또는 시간 값이 0이거나 부위가 정의되지 않았습니다.")
//                    }
//                } else {
//                    Log.w("RadarChart", "  -> 경고: DB에서 ID ${entry.exerciseId}에 해당하는 운동을 찾을 수 없습니다.")
//                }
//            }
//            Log.d("RadarChart", "레이더 차트 최종 partTimeMap: $partTimeMap")
//            return@withContext partTimeMap
//        }
//}