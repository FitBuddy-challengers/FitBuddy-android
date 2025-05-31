package com.cookandroid.challengers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cookandroid.challengers.data.ExerciseDao
import com.cookandroid.challengers.data.ExercisePlanDao
import com.cookandroid.challengers.data.ExerciseSetDao
import com.cookandroid.challengers.data.PlanDetailDao
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import android.util.Log

data class ExerciseRecordItem(
    val exerciseName: String,
    val sets: Int,
    val reps: Int,
    val isCompleted: Boolean
)

data class CalendarDecorateItem(val date: LocalDate?, val completionRate: Int)

class RecordCalendarViewModel(
    private val exercisePlanDao: ExercisePlanDao,
    private val planDetailDao: PlanDetailDao,
    private val exerciseDao: ExerciseDao,
    private val exerciseSetDao: ExerciseSetDao
) : ViewModel() {

    private val _exerciseRecords = MutableLiveData<List<ExerciseRecordItem>>()
    val exerciseRecords: LiveData<List<ExerciseRecordItem>> = _exerciseRecords

    private val _decorateDates = MutableLiveData<List<CalendarDecorateItem>>()
    val decorateDates: LiveData<List<CalendarDecorateItem>> = _decorateDates

    suspend fun loadExerciseRecords(date: LocalDate) {
        Log.d("RecordCalendarViewModel", "loadExerciseRecords started for: $date")

        // 서울 시간 기준 오늘 날짜의 시작과 끝
        val seoulTimeZone = ZoneId.of("Asia/Seoul")
        val startOfDaySeoul = date.atStartOfDay(seoulTimeZone).toInstant().toEpochMilli()
        val endOfDaySeoul = date.plusDays(1).atStartOfDay(seoulTimeZone).minusNanos(1).toInstant().toEpochMilli()

        Log.d("RecordCalendarViewModel", "Start of day Seoul: $startOfDaySeoul")
        Log.d("RecordCalendarViewModel", "End of day Seoul: $endOfDaySeoul")

        val plans = exercisePlanDao.getExercisePlansByDateRange(startOfDaySeoul, endOfDaySeoul)
        Log.d("RecordCalendarViewModel", "Retrieved plans: $plans")

        val records = mutableListOf<ExerciseRecordItem>()

        plans.forEach { plan ->
            val planDetails = planDetailDao.getPlanDetailsByPlanId(plan.id)
            Log.d("RecordCalendarViewModel", "Retrieved planDetails for plan ${plan.id}: $planDetails")
            for (detail in planDetails) {
                val exerciseName = exerciseDao.getExerciseNameById(detail.exerciseId)
                val setList = exerciseSetDao.getExerciseSets(plan.id, detail.exerciseId)
                val setCounts = setList.size // 세트 수

                // 첫 번째 세트가 있는 경우에만 reps 값을 가져옴
                val firstSetReps = if (setList.isNotEmpty()) {
                    setList.first().reps
                } else {
                    0 // 또는 다른 기본값
                }

                records.add(
                    ExerciseRecordItem(
                        exerciseName = exerciseName,
                        sets = setCounts,
                        reps = firstSetReps, // 첫 번째 세트의 reps 값 사용
                        isCompleted = detail.isCompleted
                    )
                )
            }
        }
        Log.d("RecordCalendarViewModel", "Loaded records: $records")
        _exerciseRecords.value = records
    }

    suspend fun loadAndCalculateCompletionRates(year: Int, month: Int) {
        val seoulTimeZone = ZoneId.of("Asia/Seoul") // org.threeten.bp.ZoneId
        val firstDayOfMonth = LocalDate.of(year, month, 1)
        val lastDayOfMonth = firstDayOfMonth.plusMonths(1).minusDays(1)

        val startDate = firstDayOfMonth.atStartOfDay(seoulTimeZone).toInstant().toEpochMilli()
        val endDate = lastDayOfMonth.plusDays(1).atStartOfDay(seoulTimeZone).minusNanos(1).toInstant().toEpochMilli()

        val plans = exercisePlanDao.getExercisePlansByDateRange(startDate, endDate)
        val decorateList = mutableListOf<CalendarDecorateItem>()

        for (plan in plans) {
            val planDetails = planDetailDao.getPlanDetailsByPlanId(plan.id)
            if (planDetails.isNotEmpty()) {
                val completedCount = planDetails.count { it.isCompleted }
                val completionRate = (completedCount.toDouble() / planDetails.size * 100).toInt()

                // ✅ ThreeTenBP 기반으로 변환
                val planDateThreeTen = org.threeten.bp.Instant.ofEpochMilli(plan.plannedDate)
                    .atZone(seoulTimeZone)
                    .toLocalDate()

                decorateList.add(CalendarDecorateItem(planDateThreeTen, completionRate))
            }
        }
        _decorateDates.value = decorateList
    }


    fun java.time.LocalDate.toThreeTenLocalDate(): org.threeten.bp.LocalDate {
        return org.threeten.bp.LocalDate.of(this.year, this.monthValue, this.dayOfMonth)
    }
}
class RecordCalendarViewModelFactory(
    private val exercisePlanDao: ExercisePlanDao,
    private val planDetailDao: PlanDetailDao,
    private val exerciseDao: ExerciseDao,
    private val exerciseSetDao: ExerciseSetDao
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecordCalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecordCalendarViewModel(
                exercisePlanDao,
                planDetailDao,
                exerciseDao,
                exerciseSetDao
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}