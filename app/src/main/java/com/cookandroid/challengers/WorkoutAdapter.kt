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
    private val onMoreClick: (WorkoutUiModel) -> Unit
) : ListAdapter<WorkoutUiModel, WorkoutAdapter.WorkoutViewHolder>(DiffCallback()) {

    inner class WorkoutViewHolder(val binding: ItemWorkoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val binding = ItemWorkoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.apply {
            tvWorkoutName.text = item.name
            tvWorkoutDetail1.text = "${item.reps}회"
            tvWorkoutDetail2.text = "${item.sets}세트"

            // 체크박스는 아직 기능 없음 (디자인용)
            checkWorkout.isChecked = false

            btnMore.setOnClickListener {
                onMoreClick(item)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<WorkoutUiModel>() {
        override fun areItemsTheSame(oldItem: WorkoutUiModel, newItem: WorkoutUiModel): Boolean {
            return oldItem.scheduleId == newItem.scheduleId
        }

        override fun areContentsTheSame(oldItem: WorkoutUiModel, newItem: WorkoutUiModel): Boolean {
            return oldItem == newItem
        }
    }
}