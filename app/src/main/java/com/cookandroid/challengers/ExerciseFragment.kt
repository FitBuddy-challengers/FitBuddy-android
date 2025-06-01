package com.cookandroid.challengers

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.data.ExercisePlanDao
import com.cookandroid.challengers.data.PlanDetail
import com.cookandroid.challengers.data.PlanDetailDao
import com.cookandroid.challengers.data.PlanDetailWithExercise
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentExerciseBinding
import com.cookandroid.challengers.databinding.ItemAddExerciseButtonBinding
import com.cookandroid.challengers.databinding.ItemExerciseBinding
import com.cookandroid.challengers.viewmodel.ServerExerciseAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Collections

class ExerciseFragment : Fragment() {

    private var _binding: FragmentExerciseBinding? = null
    private val binding get() = _binding!!

    private lateinit var serverAdapter: ServerExerciseAdapter
    private lateinit var db: AppDatabase
    private var planId: Long = -1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)

        serverAdapter = ServerExerciseAdapter(emptyList(), db.exerciseDao()) { schedule ->
            if (schedule.schedule_id == -1) {
                val args = Bundle().apply { putLong("planId", planId) }
                findNavController().navigate(R.id.action_exercise_to_exerciseAdd, args)
            } else {
                val editFragment = ExerciseEditFragment(
                    planId = planId,
                    exerciseId = schedule.exercise_id.toLong(),
                    exerciseName = schedule.exercise_name
                ) {
                    loadTodayPlanFromServer()
                }
                editFragment.show(parentFragmentManager, "ExerciseEdit")
            }
        }

        binding.exerciseListRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = serverAdapter

            val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val from = viewHolder.adapterPosition
                    val to = target.adapterPosition
                    if (from >= serverAdapter.getItems().size || to >= serverAdapter.getItems().size) return false
                    serverAdapter.moveItem(from, to)
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            })
            itemTouchHelper.attachToRecyclerView(this)
        }

        binding.startExerciseButton.setOnClickListener {
            val scheduleList = serverAdapter.getItems()

            // ✅ 첫 번째 미완료 운동 찾기
            val firstIncompleteIndex = scheduleList.indexOfFirst { !it.is_completed }

            if (firstIncompleteIndex != -1) {
                val target = scheduleList[firstIncompleteIndex]

                val args = Bundle().apply {
                    putLong("planId", planId)
                    putInt("initialExerciseIndex", firstIncompleteIndex)
                    putLong("scheduleId", target.schedule_id.toLong())     // 서버 스케줄 ID
                    putLong("exerciseId", target.exercise_id.toLong())     // 운동 ID
                    putString("exerciseName", target.exercise_name)        // 이름 (Room 보완)
                    putString("imagePath", target.image_path)              // 이미지 (Room 보완)
                    putString("equip", target.equip)                       // 장비 정보 (Room 보완)
                }

                findNavController().navigate(R.id.action_exercise_to_exerciseDoing, args)
            } else {
                Toast.makeText(requireContext(), "진행할 운동이 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.menuBtn.setOnClickListener {
            findNavController().navigate(R.id.action_exercise_to_exerciseList)
        }

        parentFragmentManager.setFragmentResultListener("sets_updated", viewLifecycleOwner) { _, _ ->
            loadTodayPlanFromServer()
        }

        loadTodayPlanFromServer()


    }

    override fun onResume() {
        super.onResume()
        loadTodayPlanFromServer()
    }

    // 서버로 부터 연동! -> 되었으면 좋겠다...
    private fun loadTodayPlanFromServer() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.scheduleApi.getTodayPlan(userId = 21)


                if (response.isSuccessful) {
                    val data = response.body()
                    planId = data?.plan?.id?.toLong() ?: -1L

                    val validSchedules = data?.schedules?.filter { it.exercise_id != 0 } ?: emptyList()

                    // 🧠 Room에서 운동 정보 보완
                    val enriched = validSchedules.map { schedule ->
                        val ex = db.exerciseDao().getExerciseById(schedule.exercise_id.toLong())
                        Log.d("ExerciseFragment", "🧪 exercise_id: ${schedule.exercise_id} → Room에서 찾은 운동: $ex")
                        schedule.copy(
                            exercise_name = schedule.exercise_name.ifBlank { ex?.name ?: "운동 이름 없음" },
                            image_path = ex?.imagePath ?: "", // drawable 리소스 이름
                            equip = ex?.equip ?: "정보 없음",
                            part = ex?.part ?: "부위 없음",
                            start_position = ex?.startPosition,
                            exercise_motion = ex?.exerciseMotion,
                            breathing = ex?.breathing,
                            caution = ex?.caution,
                            mets = ex?.mets ?: 0.0
                        )
                    }

                    withContext(Dispatchers.Main) {
                        serverAdapter.submitList(enriched)
                    }
                } else {
                    Log.e("ExerciseFragment", "❌ 서버 오류: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("ExerciseFragment", "❗ 네트워크 오류: ${e.message}")
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


//class ExerciseFragment : Fragment() {
//
//    private var _binding: FragmentExerciseBinding? = null
//    private val binding get() = _binding!!
//
//    private lateinit var serverAdapter: ServerExerciseAdapter
//    private lateinit var db: AppDatabase
//    private var planId: Long = -1L
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentExerciseBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
//
//        serverAdapter = ServerExerciseAdapter(emptyList(), db.exerciseDao()) { schedule ->
//            if (schedule.schedule_id == -1) {
//                val args = Bundle().apply { putLong("planId", planId) }
//                findNavController().navigate(R.id.action_exercise_to_exerciseAdd, args)
//            } else {
//                val editFragment = ExerciseEditFragment(
//                    planId = planId,
//                    exerciseId = schedule.exercise_id.toLong(),
//                    exerciseName = schedule.exercise_name
//                ) {
//                    loadTodayPlanFromServer()
//                }
//                editFragment.show(parentFragmentManager, "ExerciseEdit")
//            }
//        }
//
//        binding.exerciseListRecyclerView.apply {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = serverAdapter
//
//            val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
//                ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
//            ) {
//                override fun onMove(
//                    recyclerView: RecyclerView,
//                    viewHolder: RecyclerView.ViewHolder,
//                    target: RecyclerView.ViewHolder
//                ): Boolean {
//                    val from = viewHolder.adapterPosition
//                    val to = target.adapterPosition
//                    if (from >= serverAdapter.getItems().size || to >= serverAdapter.getItems().size) return false
//                    serverAdapter.moveItem(from, to)
//                    return true
//                }
//
//                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
//            })
//            itemTouchHelper.attachToRecyclerView(this)
//        }
//
//        binding.startExerciseButton.setOnClickListener {
//            //Toast.makeText(requireContext(), "운동 시작 기능은 아직 구현되지 않았습니다.", Toast.LENGTH_SHORT).show()
//            val args = Bundle().apply {
//                putLong("planId", planId)
//                putInt("initialExerciseIndex", 0) // 0번 운동부터 시작
//            }
//            findNavController().navigate(R.id.action_exercise_to_exerciseDoing, args)
//        }
//
//        binding.menuBtn.setOnClickListener {
//            findNavController().navigate(R.id.action_exercise_to_exerciseList)
//        }
//
//        parentFragmentManager.setFragmentResultListener("sets_updated", viewLifecycleOwner) { _, _ ->
//            loadTodayPlanFromServer()
//        }
//
//        loadTodayPlanFromServer()
//    }
//
//    override fun onResume() {
//        super.onResume()
//        loadTodayPlanFromServer()
//    }
//
//    private fun loadTodayPlanFromServer() {
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val response = RetrofitClient.scheduleApi.getTodayPlan(userId = 21)
//                if (response.isSuccessful) {
//                    val data = response.body()
//                    planId = data?.plan?.id?.toLong() ?: -1L
//
//                    val validSchedules = data?.schedules?.filter { it.exercise_id != 0 } ?: emptyList()
//                    val enriched = validSchedules.map { schedule ->
//                        val ex = db.exerciseDao().getExerciseById(schedule.exercise_id.toLong())
//                        schedule.copy(
//                            exercise_name = schedule.exercise_name.ifBlank { ex?.name ?: "운동 이름 없음" },
//                            image_path = ex?.imagePath
//                        )
//                    }
//
//                    withContext(Dispatchers.Main) {
//                        serverAdapter.submitList(enriched)
//                    }
//                } else {
//                    Log.e("ExerciseFragment", "❌ 서버 오류: ${response.code()}")
//                }
//            } catch (e: Exception) {
//                Log.e("ExerciseFragment", "❗ 네트워크 오류: ${e.message}")
//            }
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//}


// 운동탭 메인화면
//class ExerciseFragment : Fragment() {
//
//    private var _binding: FragmentExerciseBinding? = null
//    private val binding get() = _binding!!
//
//    private lateinit var adapter: ExerciseAdapter
//    private lateinit var planDao: ExercisePlanDao
//    private lateinit var planDetailDao: PlanDetailDao
//    private lateinit var db: AppDatabase
//    private var planId: Long = -1L
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentExerciseBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        db = AppDatabase.getDatabase(requireContext(), lifecycleScope)
//        planDao = db.exercisePlanDao()
//        planDetailDao = db.planDetailDao()
//
//        parentFragmentManager.setFragmentResultListener(
//            "sets_updated",
//            viewLifecycleOwner
//        ) { _, _ ->
//            loadTodayPlan()
//        }
//
//        adapter = ExerciseAdapter(requireContext(), this, db) {
//            lifecycleScope.launch(Dispatchers.IO) {
//                try {
//                    val response = RetrofitClient.scheduleApi.getTodayPlan(userId = 21) // 유저 ID 실제 값으로!
//                    if (response.isSuccessful) {
//                        val schedules = response.body()?.schedules.orEmpty()
//
//                        val newScheduleId = (schedules.maxOfOrNull { it.schedule_id } ?: 0) + 1 // 예측된 다음 스케줄 ID
//
//                        Log.d("ExerciseFragment", "🔥 예측된 다음 scheduleId: $newScheduleId")
//
//                        withContext(Dispatchers.Main) {
//                            val args = Bundle().apply {
//                                putLong("planId", planId) // ✅ 정확한 키
//                            }
//                            Log.d("ExerciseFragment", "💡 Add로 넘기는 planId = $planId") // ✅ Log는 밖에서 찍어야 정확히 찍힘
//
//                            findNavController().navigate(
//                                R.id.action_exercise_to_exerciseAdd,
//                                args
//                            )
//                        }
//                    } else {
//                        Log.e("ExerciseFragment", "❌ 오늘 스케줄 불러오기 실패: ${response.code()}")
//                    }
//                } catch (e: Exception) {
//                    Log.e("ExerciseFragment", "❗ 네트워크 오류: ${e.localizedMessage}")
//                }
//            }
//        }
//        binding.exerciseListRecyclerView.apply {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = this@ExerciseFragment.adapter
//        }
//        setupDragAndDrop()
//        loadTodayPlan()
//
//        binding.startExerciseButton.setOnClickListener {
//            if (planId != -1L) {
//                val firstIncompleteExerciseIndex =
//                    adapter.currentList.indexOfFirst { !it.planDetail.isCompleted }
//
//                if (firstIncompleteExerciseIndex != -1) {
//                    val exerciseToStart = adapter.currentList[firstIncompleteExerciseIndex]
//                    Log.d("ExerciseFragment", "Starting exercise: ${exerciseToStart.exercise.name} at index: $firstIncompleteExerciseIndex")
//
//                    findNavController().navigate(
//                        R.id.action_exercise_to_exerciseDoing,
//                        Bundle().apply {
//                            putLong("planId", planId)
//                            putInt("initialExerciseIndex", firstIncompleteExerciseIndex)
//                        }
//                    )
//                } else {
//                    // 모든 운동이 완료되었거나 운동이 없는 경우
//                    Toast.makeText(
//                        requireContext(),
//                        "오늘 계획된 운동을 모두 완료했거나, 시작할 운동이 없습니다.",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            } else {
//                Toast.makeText(requireContext(), "오늘 계획된 운동이 없습니다. 운동을 추가해주세요.", Toast.LENGTH_SHORT)
//                    .show()
//            }
//        }
//        binding.menuBtn.setOnClickListener {
//            findNavController().navigate(R.id.action_exercise_to_exerciseList)
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        loadTodayPlan()
//    }
//
//    private fun loadTodayPlan() {
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                val response = RetrofitClient.scheduleApi.getTodayPlan(userId = 21)
//                if (response.isSuccessful) {
//                    val body = response.body()
//                    val todayPlanId = body?.plan?.id?.toLong() ?: -1L
//                    planId = todayPlanId
//
//                    val planDetails = planDetailDao.getPlanDetailsForPlanId(todayPlanId)
//
//                    val planDetailWithExerciseList = planDetails.mapNotNull { planDetail ->
//                        val exercise = db.exerciseDao().getExerciseById(planDetail.exerciseId)
//                        val sets = db.exerciseSetDao().getSetsByPlanAndExerciseId(
//                            planDetail.exercisePlanId,
//                            planDetail.exerciseId
//                        )
//
//                        if (exercise != null) {
//                            PlanDetailWithExercise(planDetail, exercise, sets)
//                        } else null
//                    }.sortedBy { it.planDetail.exOrder }
//
//                    withContext(Dispatchers.Main) {
//                        adapter.submitList(planDetailWithExerciseList)
//                        Log.d("ExerciseFragment", "✅ 오늘의 운동 불러오기 완료: ${planDetailWithExerciseList.size}개")
//                    }
//                } else {
//                    Log.e("ExerciseFragment", "❌ getTodayPlan 실패: ${response.code()}")
//                }
//            } catch (e: Exception) {
//                Log.e("ExerciseFragment", "❗ 오류 발생: ${e.localizedMessage}")
//            }
//        }
//    }
//
//    private fun setupDragAndDrop() {
//        val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
//            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
//        ) {
//            override fun onMove(
//                recyclerView: RecyclerView,
//                viewHolder: RecyclerView.ViewHolder,
//                target: RecyclerView.ViewHolder
//            ): Boolean {
//                val from = viewHolder.bindingAdapterPosition
//                val to = target.bindingAdapterPosition
//                // 'Add Exercise' 버튼과 'Cool Down' 섹션은 이동할 수 없도록 방지
//                if (to >= adapter.currentList.size) return false // 'Add' 버튼 또는 'Cool Down' 영역으로 이동 방지
//
//                val newList = adapter.currentList.toMutableList().apply {
//                    Collections.swap(this, from, to)
//                }
//                adapter.submitList(newList)
//
//                lifecycleScope.launch(Dispatchers.IO) {
//                    try {
//                        val planDetails = planDetailDao.getPlanDetailsForPlanId(planId)
//                        newList.forEachIndexed { index, item ->
//                            val planDetail =
//                                planDetails.find { it.exerciseId == item.planDetail.exerciseId }
//                            planDetail?.let {
//                                val updatedPlanDetail = it.copy(exOrder = index)
//                                planDetailDao.update(updatedPlanDetail)
//                            }
//                        }
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(
//                                requireContext(),
//                                "운동 순서가 변경되었습니다.",
//                                Toast.LENGTH_SHORT
//                            ).show()
//                        }
//                    } catch (e: Exception) {
//                        Log.e("ExerciseFragment", "Error updating exOrder: ${e.message}")
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(
//                                requireContext(),
//                                "운동 순서 변경에 실패했습니다: ${e.message}",
//                                Toast.LENGTH_SHORT
//                            ).show()
//                        }
//                    }
//                }
//                return true
//            }
//
//            override fun onSwiped(holder: RecyclerView.ViewHolder, dir: Int) = Unit
//        })
//        helper.attachToRecyclerView(binding.exerciseListRecyclerView)
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//
//
//
//
//    private class ExerciseAdapter(
//        private val context: Context,
//        private val fragment: Fragment,
//        private val db: AppDatabase,
//        private val onAddClick: () -> Unit
//    ) : ListAdapter<PlanDetailWithExercise, RecyclerView.ViewHolder>(
//        object : DiffUtil.ItemCallback<PlanDetailWithExercise>() {
//            override fun areItemsTheSame(
//                oldItem: PlanDetailWithExercise,
//                newItem: PlanDetailWithExercise
//            ): Boolean {
//                return oldItem.planDetail.exercisePlanId == newItem.planDetail.exercisePlanId &&
//                        oldItem.planDetail.exerciseId == newItem.planDetail.exerciseId
//            }
//
//            override fun areContentsTheSame(
//                oldItem: PlanDetailWithExercise,
//                newItem: PlanDetailWithExercise
//            ): Boolean {
//                // isCompleted 값도 비교하여 UI 업데이트가 필요할 때 DiffUtil이 감지하도록 합니다.
//                return oldItem == newItem &&
//                        oldItem.planDetail.isCompleted == newItem.planDetail.isCompleted
//            }
//        }
//    ) {
//        private val TYPE_EXERCISE = 0
//        private val TYPE_COOLDOWN = 1
//        private val TYPE_ADD = 2
//
//        // currentList.size는 운동 아이템만 포함하므로, 쿨다운(1)과 추가 버튼(1)을 더해야 합니다.
//        override fun getItemCount() = currentList.size + 2
//        override fun getItemViewType(position: Int) = when {
//            position < currentList.size -> TYPE_EXERCISE
//            position == currentList.size -> TYPE_COOLDOWN
//            else -> TYPE_ADD
//        }
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
//            when (viewType) {
//                TYPE_EXERCISE -> {
//                    val binding = ItemExerciseBinding.inflate(
//                        LayoutInflater.from(parent.context),
//                        parent,
//                        false
//                    )
//                    ExerciseVH(binding)
//                }
//
//                TYPE_COOLDOWN -> {
//                    val view = LayoutInflater.from(parent.context)
//                        .inflate(R.layout.item_cool_down, parent, false)
//                    object : RecyclerView.ViewHolder(view) {}
//                }
//
//                else -> { // TYPE_ADD
//                    val binding = ItemAddExerciseButtonBinding.inflate(
//                        LayoutInflater.from(parent.context),
//                        parent,
//                        false
//                    )
//                    object : RecyclerView.ViewHolder(binding.root) {
//                        init {
//                            binding.addExerciseButton.setOnClickListener { onAddClick() }
//                        }
//                    }
//                }
//            }
//
//        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//            if (holder is ExerciseVH && position < currentList.size) {
//                val item = currentList[position]
//                holder.binding.apply {
//                    fragment.lifecycleScope.launch(Dispatchers.IO) {
//                        val exerciseSets = db.exerciseSetDao().getSetsByPlanAndExerciseId(
//                            item.planDetail.exercisePlanId,
//                            item.exercise.id
//                        )
//                        withContext(Dispatchers.Main) {
//                            val reps = exerciseSets.firstOrNull()?.reps ?: 0
//                            val setCount = exerciseSets.size
//                            holder.binding.apply {
//                                exerciseNameTextView.text = item.exercise.name
//                                exerciseDetailTextView.text = "${reps}회 X ${setCount}세트"
//                            }
//                        }
//                    }
//
//                    val resId = context.resources.getIdentifier(
//                        item.exercise.imagePath ?: "",
//                        "drawable",
//                        context.packageName
//                    )
//                    if (item.exercise.imagePath != null) {
//                        Glide.with(context)
//                            .asBitmap()
//                            .load(resId)
//                            .placeholder(R.drawable.ic_launcher_background)
//                            .error(R.drawable.ic_launcher_background)
//                            .into(exerciseImageView)
//                    } else {
//                        exerciseImageView.setImageResource(R.drawable.ic_launcher_background)
//                    }
//
//                    // 투명도 (visibility) 설정 부분 추가
//                    if (item.planDetail.isCompleted) {
//                        contentLayout.alpha = 0.5f // 변경: contentLayout에 alpha 적용
//                    } else {
//                        contentLayout.alpha = 1.0f // 변경: contentLayout에 alpha 적용
//                    }
//
//                    exerciseItem.setOnClickListener {
//                        fragment.findNavController().navigate(
//                            R.id.action_exercise_to_exerciseDoing,
//                            Bundle().apply {
//                                putLong("planId", item.planDetail.exercisePlanId)
//                                putInt("initialExerciseIndex", position)
//                            }
//                        )
//                    }
//                    btnMore.setOnClickListener {
//                        val dialog = ExerciseEditFragment(
//                            item.planDetail,
//                            item.exercise
//                        ) {
//                            (fragment as? ExerciseFragment)?.loadTodayPlan()
//                        }
//                        dialog.show(fragment.childFragmentManager, ExerciseEditFragment.TAG)
//                    }
//                }
//            } else if (position == currentList.size) { // Cool Down Section
//                val coolDownListView =
//                    holder.itemView.findViewById<LinearLayout>(R.id.coolDownListLayoutContainer)
//                val btnExpand = holder.itemView.findViewById<ImageButton>(R.id.btnExpand)
//
//                btnExpand.setOnClickListener {
//                    coolDownListView.visibility =
//                        if (coolDownListView.visibility == View.GONE) {
//                            View.VISIBLE
//                        } else {
//                            View.GONE
//                        }
//                }
//            }
//            // TYPE_ADD (마지막 포지션)은 onBindViewHolder에서 특별히 할 일이 없으므로 비워둡니다.
//            // 이미 onCreateViewHolder에서 클릭 리스너가 설정되어 있습니다.
//        }
//
//        private class ExerciseVH(val binding: ItemExerciseBinding) :
//            RecyclerView.ViewHolder(binding.root)
//    }
//}