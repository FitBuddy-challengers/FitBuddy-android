package com.cookandroid.challengers.viewmodel


import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cookandroid.challengers.R
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.data.ExerciseDao
import com.cookandroid.challengers.databinding.ItemAddExerciseButtonBinding
import com.cookandroid.challengers.databinding.ItemExerciseBinding
import com.cookandroid.challengers.network.dto.ScheduleDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ServerExerciseAdapter(
    private var items: List<ScheduleDto>,
    private val exerciseDao: ExerciseDao,
    private val onMoreClicked: (ScheduleDto) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_EXERCISE = 0
        private const val TYPE_ADD = 1
    }

    override fun getItemCount(): Int = items.size + 1

    override fun getItemViewType(position: Int): Int {
        return if (position < items.size) TYPE_EXERCISE else TYPE_ADD
    }

    inner class ExerciseViewHolder(val binding: ItemExerciseBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ScheduleDto) {
            binding.exerciseNameTextView.text = item.exercise_name
            // ✅ 시간 기반이면 분으로 환산해서 출력
            val detailText = if (item.is_time_type && item.display_detail == null) {
                val millis = item.seconds ?: 0
                val minutes = millis / 1000 / 60
                "${minutes}분 × ${item.set_count ?: 1}세트"
            } else {
                item.display_detail ?: "${item.set_count ?: 0}세트"
            }
            binding.exerciseDetailTextView.text = detailText

            val resId = binding.root.context.resources.getIdentifier(
                item.image_path ?: "",
                "drawable",
                binding.root.context.packageName
            ).takeIf { it != 0 } ?: R.drawable.ic_launcher_background

            binding.exerciseImageView.setImageResource(resId)

            binding.btnMore.setOnClickListener {
                onMoreClicked(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_EXERCISE -> {
                val binding = ItemExerciseBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ExerciseViewHolder(binding)
            }

            TYPE_ADD -> {
                val binding = ItemAddExerciseButtonBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                object : RecyclerView.ViewHolder(binding.root) {
                    init {
                        binding.addExerciseButton.setOnClickListener {
                            onMoreClicked(
                                ScheduleDto(
                                    schedule_id = -1,
                                    exercise_id = -1,
                                    date = "",
                                    exercise_order = -1,
                                    is_completed = false,
                                    is_dummy = false,
                                    exercise_name = "",
                                    part = "",
                                    equip = "",
                                    image_path = null,
                                    start_position = null,
                                    exercise_motion = null,
                                    breathing = null,
                                    caution = null,
                                    mets = 0.0,
                                    is_time_type = false,
                                    is_noise = false,
                                    set_count = null,
                                    display_detail = null,
                                    seconds = null
                                )
                            )
                        }
                    }
                }
            }

            else -> throw IllegalArgumentException("알 수 없는 뷰타입: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ExerciseViewHolder && position < items.size) {
            holder.bind(items[position])
        }
    }

    fun submitList(newItems: List<ScheduleDto>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition >= items.size || toPosition >= items.size) return

        val mutableList = items.toMutableList()
        val movedItem = mutableList.removeAt(fromPosition)
        mutableList.add(toPosition, movedItem)
        items = mutableList
        notifyItemMoved(fromPosition, toPosition)

        CoroutineScope(Dispatchers.IO).launch {
            mutableList.forEachIndexed { index, item ->
                try {
                    RetrofitClient.scheduleApi.updateExerciseOrder(
                        scheduleId = item.schedule_id.toLong(),
                        newOrder = index + 1
                    )
                    Log.d("ServerExerciseAdapter", "✅ 순서 업데이트: ${item.exercise_name} → ${index + 1}")
                } catch (e: Exception) {
                    Log.e("ServerExerciseAdapter", "❌ 순서 업데이트 실패: ${e.message}")
                }
            }
        }
    }

    fun getItems(): List<ScheduleDto> = items
}