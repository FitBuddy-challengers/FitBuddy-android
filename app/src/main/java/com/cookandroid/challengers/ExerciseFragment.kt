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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cookandroid.challengers.data.ExercisePlanDao
import com.cookandroid.challengers.data.PlanDetail
import com.cookandroid.challengers.data.PlanDetailDao
import com.cookandroid.challengers.data.PlanDetailWithExercise
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentExerciseBinding
import com.cookandroid.challengers.databinding.ItemAddExerciseButtonBinding
import com.cookandroid.challengers.databinding.ItemExerciseBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Collections

// 운동탭 메인화면
class ExerciseFragment : Fragment() {

    private var _binding: FragmentExerciseBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ExerciseAdapter
    private lateinit var planDao: ExercisePlanDao
    private lateinit var planDetailDao: PlanDetailDao
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

        db = AppDatabase.getDatabase(requireContext(), lifecycleScope)
        planDao = db.exercisePlanDao()
        planDetailDao = db.planDetailDao()

        parentFragmentManager.setFragmentResultListener("sets_updated", viewLifecycleOwner) { _, _ ->
            loadTodayPlan()
        }

        adapter = ExerciseAdapter(requireContext(), this, db) {
            findNavController().navigate(
                R.id.action_exercise_to_exerciseAdd,
                Bundle().apply { putLong("planId", planId) }
            )
        }
        binding.exerciseListRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ExerciseFragment.adapter
        }
        setupDragAndDrop()
        loadTodayPlan()

        binding.startExerciseButton.setOnClickListener {
            if (planId != -1L) {
                findNavController().navigate(
                    R.id.action_exercise_to_exerciseDoing,
                    Bundle().apply {
                        putLong("planId", planId)
                        putInt("initialExerciseIndex", 0)
                    }
                )
            }
        }
        binding.menuBtn.setOnClickListener {
            findNavController().navigate(R.id.action_exercise_to_exerciseList)
        }
    }

    override fun onResume() {
        super.onResume()
        loadTodayPlan()
    }

    private fun loadTodayPlan() {
        lifecycleScope.launch(Dispatchers.IO) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStart = cal.timeInMillis
            val todayEnd = Calendar.getInstance().apply {
                timeInMillis = todayStart
                add(Calendar.DAY_OF_YEAR, 1)
                add(Calendar.MILLISECOND, -1)
            }.timeInMillis

            planDao.getExercisePlansByDate(todayStart, todayEnd)
                .collectLatest { plans ->
                    if (plans.isNotEmpty()) {
                        planId = plans.first().id
                        planDao.getPlanDetailsWithExerciseFlow(planId)
                            .collectLatest { exercises ->
                                val sorted = exercises.sortedBy { it.planDetail.exOrder }
                                withContext(Dispatchers.Main) {
                                    adapter.submitList(sorted.toList())
                                }
                            }

                    } else {
                        withContext(Dispatchers.Main) {
                            adapter.submitList(emptyList())
                            planId = -1L
                        }
                    }
                }
        }
    }

    private fun setupDragAndDrop() {
        val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (to >= adapter.currentList.size) return false
                val newList = adapter.currentList.toMutableList().apply {
                    Collections.swap(this, from, to)
                }
                adapter.submitList(newList)

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val planDetails = planDetailDao.getPlanDetailsForPlanId(planId)
                        newList.forEachIndexed { index, item ->
                            val planDetail =
                                planDetails.find { it.exerciseId == item.planDetail.exerciseId }
                            planDetail?.let {
                                val updatedPlanDetail = it.copy(exOrder = index)
                                planDetailDao.update(updatedPlanDetail)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                "운동 순서가 변경되었습니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Log.e("ExerciseFragment", "Error updating exOrder: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                "운동 순서 변경에 실패했습니다: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                return true
            }

            override fun onSwiped(holder: RecyclerView.ViewHolder, dir: Int) = Unit
        })
        helper.attachToRecyclerView(binding.exerciseListRecyclerView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ExerciseAdapter(
        private val context: Context,
        private val fragment: Fragment,
        private val db: AppDatabase,
        private val onAddClick: () -> Unit
    ) : ListAdapter<PlanDetailWithExercise, RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<PlanDetailWithExercise>() {
            override fun areItemsTheSame(
                oldItem: PlanDetailWithExercise,
                newItem: PlanDetailWithExercise
            ): Boolean {
                return oldItem.planDetail.exercisePlanId == newItem.planDetail.exercisePlanId &&
                        oldItem.planDetail.exerciseId == newItem.planDetail.exerciseId
            }

            override fun areContentsTheSame(
                oldItem: PlanDetailWithExercise,
                newItem: PlanDetailWithExercise
            ): Boolean {
                return oldItem == newItem &&
                        oldItem.planDetail.isCompleted == newItem.planDetail.isCompleted
            }
        }
    ) {
        private val TYPE_EXERCISE = 0
        private val TYPE_COOLDOWN = 1
        private val TYPE_ADD = 2

        override fun getItemCount() = currentList.size + 2
        override fun getItemViewType(position: Int) = when {
            position < currentList.size -> TYPE_EXERCISE
            position == currentList.size -> TYPE_COOLDOWN
            else -> TYPE_ADD
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                TYPE_EXERCISE -> {
                    val binding = ItemExerciseBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    ExerciseVH(binding)
                }
                TYPE_COOLDOWN -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_cool_down, parent, false)
                    object : RecyclerView.ViewHolder(view) {}
                }
                else -> {
                    val binding = ItemAddExerciseButtonBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    object : RecyclerView.ViewHolder(binding.root) {
                        init {
                            binding.addExerciseButton.setOnClickListener { onAddClick() }
                        }
                    }
                }
            }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is ExerciseVH && position < currentList.size) {
                val item = currentList[position]
                holder.binding.apply {
                    fragment.lifecycleScope.launch(Dispatchers.IO) {
                        val exerciseSets = db.exerciseSetDao().getSetsByExerciseId(item.exercise.id)
                        withContext(Dispatchers.Main) {
                            val reps = exerciseSets.firstOrNull()?.reps ?: 0
                            val setCount = exerciseSets.size
                            holder.binding.apply {
                                exerciseNameTextView.text = item.exercise.name
                                exerciseDetailTextView.text = "${reps}회 X ${setCount}세트"
                            }
                        }
                    }


                    val resId = context.resources.getIdentifier(
                        item.exercise.imagePath ?: "",
                        "drawable",
                        context.packageName
                    )
                    if (item.exercise.imagePath != null) {
                        Glide.with(context)
                            .asBitmap()
                            .load(resId)
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_launcher_background)
                            .into(exerciseImageView)
                    } else {
                        exerciseImageView.setImageResource(R.drawable.ic_launcher_background)
                    }

                    exerciseItem.setOnClickListener {
                        fragment.findNavController().navigate(
                            R.id.action_exercise_to_exerciseDoing,
                            Bundle().apply {
                                putLong("planId", item.planDetail.exercisePlanId)
                                putInt("initialExerciseIndex", position)
                            }
                        )
                    }
                    btnMore.setOnClickListener {
                        val dialog = ExerciseEditFragment(
                            item.planDetail,
                            item.exercise
                        ) {
                            (fragment as? ExerciseFragment)?.loadTodayPlan()
                        }
                        dialog.show(fragment.childFragmentManager, ExerciseEditFragment.TAG)
                    }
                }
            } else if (position == currentList.size) {
                val coolDownListView =
                    holder.itemView.findViewById<LinearLayout>(R.id.coolDownListLayoutContainer)
                val btnExpand = holder.itemView.findViewById<ImageButton>(R.id.btnExpand)

                btnExpand.setOnClickListener {
                    coolDownListView.visibility =
                        if (coolDownListView.visibility == View.GONE) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                }
            }
        }

        private class ExerciseVH(val binding: ItemExerciseBinding) :
            RecyclerView.ViewHolder(binding.root)
    }
}
