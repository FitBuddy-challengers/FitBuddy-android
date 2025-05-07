package com.cookandroid.challengers

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cookandroid.challengers.data.ExercisePlan
import com.cookandroid.challengers.data.ExercisePlanDao
import com.cookandroid.challengers.data.PlanDetailWithExercise
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentExerciseBinding
import com.cookandroid.challengers.databinding.ItemAddExerciseButtonBinding
import com.cookandroid.challengers.databinding.ItemExerciseBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExerciseFragment : Fragment() {

    private var _binding: FragmentExerciseBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ExerciseListAdapter
    private lateinit var planDao: ExercisePlanDao
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

        val db = AppDatabase.getDatabase(requireContext(), lifecycleScope)
        planDao = db.exercisePlanDao()

        binding.exerciseListRecyclerView.visibility = View.VISIBLE
        binding.startExerciseButton.visibility = View.VISIBLE

        adapter = ExerciseListAdapter(requireContext()) {
//            findNavController().navigate(
//                R.id.action_ExerciseFragment_to_exerciseAddFragment,
//                Bundle().apply { putLong("planId", planId) }
//            )
        }

        binding.exerciseListRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ExerciseFragment.adapter
        }

        lifecycleScope.launch(Dispatchers.IO) {
            if (planId < 0) {
                val allPlans: List<ExercisePlan> = planDao.getAllExercisePlans().first()
                if (allPlans.isNotEmpty()) {
                    planId = allPlans.first().id
                }
            }

            planDao.getPlanDetailsWithExerciseFlow(planId)
                .collectLatest { list ->
                    val sortedList = list.sortedBy { it.planDetail.exOrder }
                    withContext(Dispatchers.Main) {
                        adapter.submitList(sortedList)
                    }
                }
        }

        binding.startExerciseButton.setOnClickListener {
//            findNavController().navigate(
//                R.id.action_ExerciseFragment_to_exerciseDoingFragment,
//                Bundle().apply { putLong("planId", planId) }
//            )
        }

        binding.menuBtn.setOnClickListener {
            findNavController().navigate(R.id.action_exerciseFragment_to_exerciseListFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ExerciseListAdapter(
        private val context: Context,
        private val onAddButtonClick: () -> Unit
    ) : ListAdapter<PlanDetailWithExercise, RecyclerView.ViewHolder>(PlanExerciseDiffCallback()) {

        private val VIEW_TYPE_EXERCISE = 0
        private val VIEW_TYPE_ADD_BUTTON = 1

        override fun getItemViewType(position: Int): Int =
            if (position < currentList.size) VIEW_TYPE_EXERCISE else VIEW_TYPE_ADD_BUTTON

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            if (viewType == VIEW_TYPE_EXERCISE) {
                val binding = ItemExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ExerciseViewHolder(binding)
            } else {
                val binding = ItemAddExerciseButtonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                AddButtonViewHolder(binding)
            }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is ExerciseViewHolder && position < currentList.size) {
                holder.bind(currentList[position])
            }
        }

        override fun getItemCount(): Int = currentList.size + 1

        inner class ExerciseViewHolder(private val binding: ItemExerciseBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(item: PlanDetailWithExercise) {
                binding.exerciseNameTextView.text = item.exercise.name
                binding.exerciseDetailTextView.text = "${item.planDetail.reps}회 X ${item.planDetail.sets}세트"

                val resId = context.resources.getIdentifier(
                    item.exercise.imagePath ?: "",
                    "drawable",
                    context.packageName
                )
                binding.exerciseImageView.setImageResource(
                    if (resId != 0) resId else R.drawable.ic_launcher_background
                )

                binding.exerciseItem.setOnClickListener {
                    // 상세 보기 화면으로 이동
                    // findNavController().navigate(R.id.action_exerciseFragment_to_exerciseDetailFragment,
                    //     Bundle().apply { putLong("exerciseId", item.exercise.id) })
                }
            }
        }

        inner class AddButtonViewHolder(binding: ItemAddExerciseButtonBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {
                binding.addExerciseButton.setOnClickListener {
                    onAddButtonClick()
                }
            }
        }
    }

    private class PlanExerciseDiffCallback : DiffUtil.ItemCallback<PlanDetailWithExercise>() {
        override fun areItemsTheSame(oldItem: PlanDetailWithExercise, newItem: PlanDetailWithExercise): Boolean {
            return oldItem.planDetail.exercisePlanId == newItem.planDetail.exercisePlanId &&
                    oldItem.planDetail.exerciseId == newItem.planDetail.exerciseId
        }

        override fun areContentsTheSame(oldItem: PlanDetailWithExercise, newItem: PlanDetailWithExercise): Boolean {
            return oldItem == newItem
        }
    }
}