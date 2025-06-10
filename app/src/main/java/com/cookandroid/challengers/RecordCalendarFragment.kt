package com.cookandroid.challengers

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cookandroid.challengers.api.RetrofitClient
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.CalendarMode
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.format.TitleFormatter
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

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

        // ViewModel 생성
        viewModel = ViewModelProvider(this).get(RecordCalendarViewModel::class.java)

        setupRecyclerView()
        setupCalendarView()
        observeViewModel()

        // 초기 데이터 로드 (오늘 날짜 기준)
        val today = LocalDate.now()
        viewModel.loadExerciseRecords(today)
        viewModel.loadAndCalculateCompletionRates(today.year, today.monthValue)
    }

    private fun setupRecyclerView() {
        recordRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        recordAdapter = ExerciseRecordAdapter(emptyList())
        recordRecyclerView.adapter = recordAdapter
    }

    private fun observeViewModel() {
        // ViewModel의 LiveData를 관찰하여 UI 업데이트
        viewModel.exerciseRecords.observe(viewLifecycleOwner) { records ->
            Log.d("RecordCalendarFragment", "서버로부터 받은 기록으로 UI 업데이트: ${records.size}개")
            recordAdapter.updateList(records)
        }

        // 이행률 데이터 관찰 및 데코레이터 적용
        viewModel.decorateDates.observe(viewLifecycleOwner) { decorateItems ->
            calendarView.removeDecorators() // 기존 데코레이터 제거
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
        }
    }

    private fun setupCalendarView() {
        // 초기 텍스트 설정 (오늘 날짜)
        val today = CalendarDay.today().date
        val formattedToday = today.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.getDefault()))
        selectedDateTextView.text = formattedToday

        calendarView.apply {
            state().edit()
                .setFirstDayOfWeek(DayOfWeek.MONDAY)
                .setCalendarDisplayMode(CalendarMode.MONTHS)
                .commit()

            showOtherDates = MaterialCalendarView.SHOW_NONE
            isDynamicHeightEnabled = true
            selectionMode = MaterialCalendarView.SELECTION_MODE_SINGLE
            setTitleFormatter(titleFormatter)
            setSelectionColor(Color.TRANSPARENT)
            selectedDecorator = SelectedDateDecorator(requireContext())
            addDecorator(selectedDecorator)

            // 날짜 선택 리스너
            setOnDateChangedListener { _, date, _ ->
                selectedDateTextView.text = date.date.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.getDefault()))
                viewModel.loadExerciseRecords(date.date) // 선택된 날짜의 기록 로드
                selectedDecorator.selectedDay = date
                invalidateDecorators()
            }

            // 캘린더 월 변경 리스너
            setOnMonthChangedListener { _, date ->
                viewModel.loadAndCalculateCompletionRates(date.year, date.month)
            }

            // 초기 선택: 오늘
            selectedDecorator.selectedDay = CalendarDay.today()
            invalidateDecorators()
        }
    }
}

// Decorator 클래스
class SelectedDateDecorator(context: Context) : DayViewDecorator {
    private val transparent = ColorDrawable(Color.TRANSPARENT)
    var selectedDay: CalendarDay? = null

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return day == selectedDay
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
        val inset = 4 // dp
        val insetDrawable = InsetDrawable(drawable, inset, inset, inset, inset)
        view.setBackgroundDrawable(insetDrawable)
        view.addSpan(ForegroundColorSpan(whiteColor))
    }
}


// ★★★ 어댑터 클래스를 프래그먼트 파일 하단에 함께 배치 ★★★
class ExerciseRecordAdapter(private var recordList: List<RetrofitClient.ExerciseRecordItem>) :
    RecyclerView.Adapter<ExerciseRecordAdapter.RecordViewHolder>() {

    inner class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val completedIcon: ImageView = itemView.findViewById(R.id.completedIcon)
        val planTextView: TextView = itemView.findViewById(R.id.planTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_completed_plan, parent, false)
        return RecordViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val currentItem = recordList[position]

        if (currentItem.isCompleted) {
            holder.completedIcon.setImageResource(R.drawable.btn_completed)
        } else {
            holder.completedIcon.setImageResource(R.drawable.btn_uncompleted)
        }

        // isTimeType에 따라 표시할 텍스트 포맷팅
        val detailText = if (currentItem.isTimeType) {
            val totalSeconds = currentItem.seconds ?: 0
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            "${currentItem.exerciseName} ${String.format("%02d:%02d", minutes, seconds)} X ${currentItem.sets ?: 1}세트"
        } else {
            "${currentItem.exerciseName} ${currentItem.reps ?: 0}회 X ${currentItem.sets ?: 1}세트"
        }
        holder.planTextView.text = detailText
    }

    override fun getItemCount(): Int {
        return recordList.size
    }

    fun updateList(newList: List<RetrofitClient.ExerciseRecordItem>) {
        recordList = newList
        notifyDataSetChanged()
    }
}