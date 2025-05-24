package com.cookandroid.challengers


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cookandroid.challengers.data.ExercisePlanDao
import com.cookandroid.challengers.data.PlanDetailDao
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!  // 안전하게 접근

    private lateinit var dateAdapter: DateAdapter
    private lateinit var workoutAdapter: WorkoutAdapter

    private lateinit var planDao: ExercisePlanDao
    private lateinit var planDetailDao: PlanDetailDao

    private val weekDates = mutableListOf<WeekDate>()
    private var selectedPosition = 0
    private var planId: Long = -1L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext(), lifecycleScope)
        planDao = db.exercisePlanDao()
        planDetailDao = db.planDetailDao()

        setupButtons()
        setupDateRecyclerView()
        setupWorkoutRecyclerView()
        loadWeekDates()
        selectedPosition = weekDates.indexOfFirst { it.isToday }.takeIf { it >= 0 } ?: 0
        selectDate(selectedPosition)
    }

    override fun onResume() {
        super.onResume()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            add(Calendar.DAY_OF_MONTH, selectedPosition)
        }
        loadWorkoutForDate(calendar)
    }

    private fun setupButtons() {
        binding.btnNoti.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_homeNotiFragment)
        }
        binding.btnMypage.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_homeMypageFragment)
        }
        binding.btnAiChat.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_homeAichatFragment)
        }
        binding.btnAddWorkout.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_exerciseAddFragment)
        }
        // 운동 시작 버튼: 수정 필요 - 홈으로 돌아가지 못하는 오류
        binding.btnStartWorkout.setOnClickListener {
            findNavController().navigate(
                R.id.action_home_to_exerciseFragment,
                null,
                androidx.navigation.navOptions {
                    launchSingleTop = true
                    popUpTo(R.id.homeFragment) {
                        inclusive = false
                    }
                }
            )
        }
    }

    private fun setupDateRecyclerView() {
        dateAdapter = DateAdapter(weekDates) { position -> selectDate(position) }
        binding.rvDate.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = dateAdapter
        }
    }

    private fun setupWorkoutRecyclerView() {
        workoutAdapter = WorkoutAdapter(
            requireContext(),
            this,
            planDetailDao,
            onAddClick = { findNavController().navigate(R.id.action_home_to_exerciseAddFragment) }
        )

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                workoutAdapter.moveItem(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun isLongPressDragEnabled(): Boolean = false
        })

        binding.rvWorkout.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = workoutAdapter
            itemTouchHelper.attachToRecyclerView(this)
        }

        workoutAdapter.setDragListener { vh -> itemTouchHelper.startDrag(vh) }
    }

    private fun loadWeekDates() {
        weekDates.clear()
        val today = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
        val base = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
        val sdf = SimpleDateFormat("d", Locale.KOREA)
        repeat(7) {
            val isToday = base.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && base.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            weekDates.add(WeekDate(sdf.format(base.time), isToday, false))
            base.add(Calendar.DAY_OF_MONTH, 1)
        }
        dateAdapter.notifyDataSetChanged()
    }

    private fun selectDate(position: Int) {
        selectedPosition = position
        dateAdapter.setSelectedPosition(position)
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            add(Calendar.DAY_OF_MONTH, position)
        }
        binding.tvDay.text = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA).format(calendar.time)
        loadWorkoutForDate(calendar)
    }

    private fun loadWorkoutForDate(calendar: Calendar) {
        lifecycleScope.launch {
            val todayStart = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1

            val plans = planDao.getExercisePlansByDateRange(todayStart, todayEnd)
            if (plans.isNotEmpty()) {
                planId = plans.first().id
                val list = planDetailDao.getPlanDetailsWithExerciseOnce(planId).sortedBy { it.planDetail.exOrder }

                workoutAdapter.submitList(list)

                // 게이지
                val completed = list.count { it.planDetail.isCompleted }
                val total = list.size
                val percent = if (total > 0) (completed * 100 / total) else 0
                binding.uiGauge.progress = percent

            } else {
                workoutAdapter.submitList(emptyList())
                binding.uiGauge.progress = 0
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지!
    }

}