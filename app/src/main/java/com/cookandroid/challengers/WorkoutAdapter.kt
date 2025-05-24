package com.cookandroid.challengers

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cookandroid.challengers.data.PlanDetailDao
import com.cookandroid.challengers.data.PlanDetailWithExercise
import com.cookandroid.challengers.databinding.ItemWorkoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections

class WorkoutAdapter(
    private val context: Context,
    private val fragment: Fragment,
    private val planDetailDao: PlanDetailDao,
    private val onAddClick: () -> Unit
) : ListAdapter<PlanDetailWithExercise, RecyclerView.ViewHolder>(DiffCallback()) {

    private val TYPE_EXERCISE = 0
    private val TYPE_COOLDOWN = 1
    private val TYPE_ADD = 2

    private var dragListener: ((RecyclerView.ViewHolder) -> Unit)? = null
    fun setDragListener(listener: (RecyclerView.ViewHolder) -> Unit) {
        dragListener = listener
    }

    override fun getItemCount(): Int = currentList.size

    override fun getItemViewType(position: Int): Int = TYPE_EXERCISE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemWorkoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ExerciseViewHolder && position < currentList.size) {
            val item = currentList[position]
            holder.binding.apply {
                tvWorkoutName.text = item.exercise.name
                tvWorkoutDetail1.text = "${item.planDetail.reps}회"
                tvWorkoutDetail2.text = "${item.planDetail.sets}세트"

                btnMore.setOnClickListener {
                    val dialog = ExerciseEditFragment(item.planDetail, item.exercise) {
                        if (fragment is HomeFragment) {
                            fragment.onResume()
                        } else if (fragment is ExerciseFragment) {
                            fragment.loadTodayPlan()
                        }
                    }
                    dialog.show(fragment.childFragmentManager, ExerciseEditFragment.TAG)
                }

                workout.setOnClickListener {
                    fragment.findNavController().navigate(
                        R.id.action_exercise_to_exerciseDoing,
                        Bundle().apply {
                            putLong("planId", item.planDetail.exercisePlanId)
                            putInt("initialExerciseIndex", position)
                        }
                    )
                }
            }
        }
    }

    fun moveItem(from: Int, to: Int) {
        if (from >= currentList.size || to >= currentList.size) return

        val newList = currentList.toMutableList().apply {
            Collections.swap(this, from, to)
        }

        submitList(newList)

        fragment.lifecycleScope.launch(Dispatchers.IO) {
            newList.forEachIndexed { index, item ->
                planDetailDao.updateExOrder(
                    planId = item.planDetail.exercisePlanId,
                    exerciseId = item.planDetail.exerciseId,
                    newOrder = index
                )
            }
        }
    }

    private class ExerciseViewHolder(val binding: ItemWorkoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class DiffCallback : DiffUtil.ItemCallback<PlanDetailWithExercise>() {
        override fun areItemsTheSame(
            oldItem: PlanDetailWithExercise,
            newItem: PlanDetailWithExercise
        ): Boolean = oldItem.planDetail.exercisePlanId == newItem.planDetail.exercisePlanId &&
                oldItem.planDetail.exerciseId == newItem.planDetail.exerciseId

        override fun areContentsTheSame(
            oldItem: PlanDetailWithExercise,
            newItem: PlanDetailWithExercise
        ): Boolean = oldItem == newItem &&
                oldItem.planDetail.isCompleted == newItem.planDetail.isCompleted
    }
}