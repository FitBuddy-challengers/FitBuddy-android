package com.cookandroid.challengers.viewmodel

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton // CoolDownViewHolder에서 사용
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cookandroid.challengers.R
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.data.ExerciseDao
import com.cookandroid.challengers.databinding.ItemAddExerciseButtonBinding
import com.cookandroid.challengers.databinding.ItemCoolDownBinding // 쿨다운 바인딩
import com.cookandroid.challengers.databinding.ItemExerciseBinding
import com.cookandroid.challengers.network.dto.ScheduleDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServerExerciseAdapter(
    private val exerciseDao: ExerciseDao, // 로컬 DB 접근 (이름/이미지 보완 등에 사용 가능)
    // ★★★ 콜백 세분화 ★★★
    private val onExerciseItemClicked: (schedule: ScheduleDto) -> Unit, // 아이템 전체 클릭 시
    private val onMoreButtonClicked: (schedule: ScheduleDto) -> Unit,   // '더보기' 버튼 클릭 시
    private val onAddButtonClicked: () -> Unit                          // '운동 추가' 버튼 클릭 시
) : ListAdapter<ScheduleDto, RecyclerView.ViewHolder>(ScheduleDiffCallback()) {

    companion object {
        const val TYPE_EXERCISE = 0
        const val TYPE_COOLDOWN = 1
        const val TYPE_ADD = 2
    }

    override fun getItemCount(): Int {
        // 쿨다운 아이템을 항상 표시한다고 가정하고 +2 (쿨다운 + 추가 버튼)
        // 만약 쿨다운이 없다면 currentList.size + 1
        return currentList.size + 2
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position < currentList.size -> TYPE_EXERCISE
            position == currentList.size -> TYPE_COOLDOWN // 운동 목록 바로 다음에 쿨다운
            else -> TYPE_ADD // 마지막은 항상 추가 버튼
        }
    }

    inner class ExerciseViewHolder(val binding: ItemExerciseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(schedule: ScheduleDto) {
            binding.exerciseNameTextView.text = schedule.exercise_name
            val detailText = if (schedule.is_time_type && schedule.display_detail.isNullOrEmpty()) {
                val totalSeconds = schedule.seconds ?: 0
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                "${minutes}분 ${seconds}초 × ${schedule.set_count ?: 1}세트"
            } else {
                schedule.display_detail ?: "${schedule.set_count ?: 0}세트"
            }
            binding.exerciseDetailTextView.text = detailText

            val imagePath = schedule.image_path
            val context = binding.root.context
            val resId = if (!imagePath.isNullOrBlank()) {
                try { context.resources.getIdentifier(imagePath, "drawable", context.packageName).takeIf { it != 0 } }
                catch (e: Exception) { R.drawable.ic_fitbuddy_logo }
            } else { R.drawable.ic_fitbuddy_logo }

            Glide.with(context)
                .asBitmap()
                .load(resId)
                .placeholder(R.drawable.ic_fitbuddy_logo) // 기본 이미지 리소스 필요
                .error(R.drawable.ic_fitbuddy_logo)     // 에러 시 이미지 리소스 필요
                .into(binding.exerciseImageView)

            // ★★★ 클릭 리스너 분리 ★★★
            binding.btnMore.setOnClickListener {
                onMoreButtonClicked(schedule) // '더보기' 버튼 클릭 콜백 호출
            }
            binding.exerciseItem.setOnClickListener {
                onExerciseItemClicked(schedule) // 아이템 전체 클릭 콜백 호출
            }
            binding.contentLayout.alpha = if (schedule.is_completed) 0.5f else 1.0f
        }
    }

    inner class CoolDownViewHolder(val binding: ItemCoolDownBinding) : RecyclerView.ViewHolder(binding.root) {
        private var isExpanded = false
        fun bind() {
            binding.coolDownListLayoutContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
            updateExpandButtonIcon(binding.btnExpand) // XML ID에 맞게 수정 (예: btnExpandCoolDown)

            binding.btnExpand.setOnClickListener { // XML ID에 맞게 수정
                isExpanded = !isExpanded
                binding.coolDownListLayoutContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE
                updateExpandButtonIcon(binding.btnExpand) // XML ID에 맞게 수정
            }
            // binding.coolDownTitle.text = "쿨다운 스트레칭" // 필요시
        }
        private fun updateExpandButtonIcon(button: ImageButton) {
            button.setImageResource(if (isExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down)
        }
    }

    inner class AddButtonViewHolder(val binding: ItemAddExerciseButtonBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.addExerciseButton.setOnClickListener {
                onAddButtonClicked() // '운동 추가' 버튼 클릭 콜백 호출
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_EXERCISE -> ExerciseViewHolder(ItemExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            TYPE_COOLDOWN -> CoolDownViewHolder(ItemCoolDownBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            TYPE_ADD -> AddButtonViewHolder(ItemAddExerciseButtonBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ExerciseViewHolder -> holder.bind(currentList[position])
            is CoolDownViewHolder -> holder.bind()
        }
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition >= currentList.size || toPosition >= currentList.size) return

        val currentItems = currentList.toMutableList()
        val movedItem = currentItems.removeAt(fromPosition)
        currentItems.add(toPosition, movedItem)
        submitList(currentItems) // ListAdapter의 submitList 호출

        CoroutineScope(Dispatchers.IO).launch {
            try {
                currentItems.forEachIndexed { index, item ->
                    RetrofitClient.scheduleApi.updateExerciseOrder(
                        scheduleId = item.schedule_id.toLong(),
                        newOrder = index + 1
                    )
                }
                Log.d("ServerExerciseAdapter", "✅ 순서 업데이트 API 호출 완료")
            } catch (e: Exception) {
                Log.e("ServerExerciseAdapter", "❌ 순서 업데이트 API 호출 실패: ${e.message}")
            }
        }
    }
}

class ScheduleDiffCallback : DiffUtil.ItemCallback<ScheduleDto>() {
    override fun areItemsTheSame(oldItem: ScheduleDto, newItem: ScheduleDto): Boolean {
        return oldItem.schedule_id == newItem.schedule_id
    }
    override fun areContentsTheSame(oldItem: ScheduleDto, newItem: ScheduleDto): Boolean {
        return oldItem == newItem
    }
}