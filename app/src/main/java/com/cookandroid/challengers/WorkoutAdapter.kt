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
    private val onCheckChanged: (WorkoutUiModel, Boolean) -> Unit
) : ListAdapter<WorkoutUiModel, WorkoutAdapter.WorkoutViewHolder>(DiffCallback()) {

    inner class WorkoutViewHolder(val binding: ItemWorkoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val binding = ItemWorkoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WorkoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        val item = getItem(position)

        // 디버그 로그 추가
        android.util.Log.d(
            "WorkoutAdapter",
            " 바인딩 → id=${item.scheduleId}, name=${item.name}, reps=${item.reps}, seconds=${item.seconds}, sets=${item.sets}, done=${item.isCompleted}"
        )


        holder.binding.apply {
            tvWorkoutName.text = item.name

            // 시간 운동인지 / 반복 운동인지 구분해서 표시
            if (item.seconds != null) {
                // 시간 운동 → "00:01:00"
                tvWorkoutDetail1.text = formatSeconds(item.seconds)
                tvWorkoutDetail2.text = "${item.sets}세트"
            } else {
                // 반복 운동 → "15회"
                tvWorkoutDetail1.text = "${item.reps}회"
                tvWorkoutDetail2.text = "${item.sets}세트"
            }

            // 체크박스 임시
            checkWorkout.setOnCheckedChangeListener(null)
            checkWorkout.isChecked = item.isCompleted

            checkWorkout.setOnCheckedChangeListener { _, isChecked ->
                // 상태 변경은 외부에서 처리 후 submitList로 갱신할 것!
                onCheckChanged(item.copy(isCompleted = isChecked), isChecked)
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

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}