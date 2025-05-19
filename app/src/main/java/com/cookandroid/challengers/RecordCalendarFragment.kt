package com.cookandroid.challengers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.CalendarMode
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.format.TitleFormatter
import kotlinx.coroutines.launch
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale
import android.R.color.transparent
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.text.style.ForegroundColorSpan
import android.util.Log
import androidx.compose.runtime.getValue
import com.cookandroid.challengers.data.ExerciseDao
import com.cookandroid.challengers.data.ExercisePlanDao
import com.cookandroid.challengers.data.ExerciseSetDao
import com.cookandroid.challengers.data.PlanDetailDao
import com.cookandroid.challengers.data.db.AppDatabase
import com.google.android.material.dialog.MaterialDialogs.insetDrawable
import com.prolificinteractive.materialcalendarview.MaterialCalendarView

class RecordCalendarFragment : Fragment() {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var selectedDateTextView: TextView
    private lateinit var selectedDecorator: SelectedDateDecorator
    private lateinit var recordRecyclerView: RecyclerView
    private lateinit var recordAdapter: ExerciseRecordAdapter
    private lateinit var viewModel: RecordCalendarViewModel

    private val titleFormatter = object : TitleFormatter {
        val formatter = DateTimeFormatter.ofPattern("yy년 M월", Locale.getDefault())
        override fun format(day: CalendarDay?): CharSequence {
            return day?.date?.format(formatter) ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_record_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        calendarView = view.findViewById(R.id.calendarView)
        selectedDateTextView = view.findViewById(R.id.selectedDateTextView)
        recordRecyclerView = view.findViewById(R.id.personalChallengeRecyclerView)
        recordRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        recordAdapter = ExerciseRecordAdapter(emptyList())
        recordRecyclerView.adapter = recordAdapter

        // AppDatabase 인스턴스를 가져올 때 scope 전달
        val database = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
        val exercisePlanDao = database.exercisePlanDao()
        val planDetailDao = database.planDetailDao()
        val exerciseDao = database.exerciseDao()
        val exerciseSetDao = database.exerciseSetDao()

        // ViewModel 팩토리를 사용하여 ViewModel 생성
        val viewModelFactory = RecordCalendarViewModelFactory(
            exercisePlanDao,
            planDetailDao,
            exerciseDao,
            exerciseSetDao
        )
        viewModel = ViewModelProvider(this, viewModelFactory).get(RecordCalendarViewModel::class.java)
        Log.d("RecordCalendarFragment", "ViewModel created: $viewModel")

        // 초기 텍스트 설정 (오늘 날짜)
        val today = CalendarDay.today().date
        val formattedToday =
            today.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.getDefault()))
        selectedDateTextView.text = formattedToday

        // 캘린더 설정
        calendarView.apply {
            // 월 단위로 표시
            state().edit()
                .setFirstDayOfWeek(DayOfWeek.MONDAY)
                .setCalendarDisplayMode(CalendarMode.MONTHS)
                .commit()

            showOtherDates = MaterialCalendarView.SHOW_NONE
            isDynamicHeightEnabled = true
            selectionMode = MaterialCalendarView.SELECTION_MODE_SINGLE

            // 헤더 형식 설정
            setTitleFormatter(object : TitleFormatter {
                override fun format(calendarDay: CalendarDay?): CharSequence {
                    val year = calendarDay?.year ?: LocalDate.now().year
                    val month = calendarDay?.month ?: LocalDate.now().monthValue
                    return String.format("%04d년 %d월", year, month)
                }
            })

            // 선택 날짜 데코
            setSelectionColor(Color.TRANSPARENT)
            selectedDecorator = SelectedDateDecorator(requireContext())
            addDecorator(selectedDecorator)

            // 오늘 날짜 데코
            addDecorator(TodayDecorator(requireContext()))

            // 날짜 선택 리스너
            setOnDateChangedListener { _, date, _ ->
                selectedDateTextView.text =
                    date.date.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.getDefault()))

                lifecycleScope.launch {
                    viewModel.loadExerciseRecords(date.date)
                    viewModel.loadAndCalculateCompletionRates(date.year, date.month)
                }
                selectedDecorator.selectedDay = date
                invalidateDecorators()
            }

            // 초기 선택: 오늘
            selectedDecorator.selectedDay = CalendarDay.today()
            invalidateDecorators()
        }

        // 초기 데이터 로드 (오늘 날짜)
        lifecycleScope.launch {
            Log.d("RecordCalendarFragment", "loadExerciseRecords called for: ${CalendarDay.today().date}")
            viewModel.loadExerciseRecords(CalendarDay.today().date)
            viewModel.loadAndCalculateCompletionRates(LocalDate.now().year, LocalDate.now().monthValue)
        }

        // ViewModel의 LiveData를 관찰하여 UI 업데이트
        viewModel.exerciseRecords.observe(viewLifecycleOwner) { records ->
            Log.d("RecordCalendarFragment", "Observed records: $records")
            recordAdapter.updateList(records)
        }

        // 이행률 데이터 관찰 및 데코레이터 적용
        viewModel.decorateDates.observe(viewLifecycleOwner) { decorateItems ->
            calendarView.removeDecorators() // 기존ㅠ 데코레이터 제거
            calendarView.addDecorator(selectedDecorator) // 선택 데코레이터 다시 추가

            val items100 = decorateItems.filter { it.completionRate >= 100 }
            val items70 = decorateItems.filter { it.completionRate in 70..99 }
            val items30 = decorateItems.filter { it.completionRate in 30..69 }

            if (items100.isNotEmpty()) {
                calendarView.addDecorator(CompletionRateGroupDecorator(requireContext(), items100, R.drawable.ic_circle_blue_100))
            }
            if (items70.isNotEmpty()) {
                calendarView.addDecorator(CompletionRateGroupDecorator(requireContext(), items70, R.drawable.ic_circle_blue_70))
            }
            if (items30.isNotEmpty()) {
                calendarView.addDecorator(CompletionRateGroupDecorator(requireContext(), items30, R.drawable.ic_circle_blue_30))
            }
            calendarView.addDecorator(TodayDecorator(requireContext())) // 오늘 데코레이터 다시 추가
        }

        // 캘린더 월 변경 리스너
        calendarView.setOnMonthChangedListener { _, date ->
            lifecycleScope.launch {
                viewModel.loadAndCalculateCompletionRates(date.year, date.month ?: LocalDate.now().monthValue)
            }
        }
    }

    fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }

}

class TodayDecorator(context: Context) : DayViewDecorator {
    private val today = CalendarDay.today()
    private val bgDrawable = ContextCompat.getDrawable(context, R.drawable.ic_circle_black)!!
    private val textColor = ContextCompat.getColor(context, R.color.white)
    private val transparent = ColorDrawable(Color.TRANSPARENT)


    override fun shouldDecorate(day: CalendarDay): Boolean {
        return day == today
    }

    override fun decorate(view: DayViewFacade) {
        view.setSelectionDrawable(transparent)
        val inset = 4
        val insetDrawable = InsetDrawable(bgDrawable, inset, inset, inset, inset)
        view.setBackgroundDrawable(insetDrawable)
        view.addSpan(ForegroundColorSpan(textColor))
    }
}

class SelectedDateDecorator(context: Context) : DayViewDecorator {
    private val transparent = ColorDrawable(Color.TRANSPARENT)
    var selectedDay: CalendarDay? = null


    override fun shouldDecorate(day: CalendarDay): Boolean {
        return day == selectedDay && day != CalendarDay.today()
    }

    override fun decorate(view: DayViewFacade) {
        view.setSelectionDrawable(transparent)
    }
}

class CompletionRateGroupDecorator(
    private val context: Context,
    private val items: List<CalendarDecorateItem>,
    private val backgroundResId: Int
) : DayViewDecorator {

    private val dates: Set<CalendarDay> = items.mapNotNull { item ->
        item.date?.let { CalendarDay.from(it.year, it.monthValue, it.dayOfMonth) }
    }.toSet()

    private val drawable: Drawable = ContextCompat.getDrawable(context, backgroundResId)!!
    private val whiteColor = ContextCompat.getColor(context, R.color.white)

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        val inset = 4 // dp 단위로 여백 설정
        val insetDrawable = InsetDrawable(drawable, inset, inset, inset, inset)

        view.setBackgroundDrawable(insetDrawable)
        view.addSpan(ForegroundColorSpan(whiteColor))
    }
}




// 어댑터 클래스
class ExerciseRecordAdapter(private var recordList: List<ExerciseRecordItem>) :
    RecyclerView.Adapter<ExerciseRecordAdapter.RecordViewHolder>() {

    inner class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val completedIcon: ImageView = itemView.findViewById(R.id.completedIcon)
        val planTextView: TextView = itemView.findViewById(R.id.planTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_completed_plan, parent, false) // 아이템 레이아웃
        return RecordViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val currentItem = recordList[position]
        Log.d("ExerciseRecordAdapter", "Binding item at position $position: $currentItem")

        if (currentItem.isCompleted) {
            holder.completedIcon.setImageResource(R.drawable.btn_completed) // 완료 아이콘
        } else {
            holder.completedIcon.setImageResource(R.drawable.btn_uncompleted) // 미완료 아이콘
        }

        holder.planTextView.text =
            "${currentItem.exerciseName} ${currentItem.reps}회 X ${currentItem.sets}세트"
        Log.d("ExerciseRecordAdapter", "Text set to: ${holder.planTextView.text}")
    }

    override fun getItemCount(): Int {
        return recordList.size
    }

    fun updateList(newList: List<ExerciseRecordItem>) {
        recordList = newList
        notifyDataSetChanged()
    }
}