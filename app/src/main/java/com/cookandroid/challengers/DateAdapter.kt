package com.cookandroid.challengers

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cookandroid.challengers.databinding.ItemDateBinding

class DateAdapter(
    private val items: List<WeekDate>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {

    private var selectedPosition = -1

    inner class DateViewHolder(val binding: ItemDateBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(weekDate: WeekDate, position: Int) {
            Log.d("DateAdapter", "📅 날짜 바인딩됨: ${weekDate.date}")

            binding.tvDate.text = weekDate.date
            binding.tvDate.visibility = View.VISIBLE

            binding.root.isSelected = position == selectedPosition

            binding.root.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousPosition)
                notifyItemChanged(position)
                onItemClick(position)
            }

            // 임시 마킹
            if (weekDate.isToday) {
                binding.tvDate.setTextColor(ContextCompat.getColor(binding.root.context, R.color.white))
                binding.tvDate.setBackgroundResource(R.drawable.bg_date_today)
            } else {
                binding.tvDate.setTextColor(ContextCompat.getColor(binding.root.context, android.R.color.black))
                binding.tvDate.setBackgroundResource(R.drawable.bg_date_default)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val binding = ItemDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    fun setSelectedPosition(position: Int) {
        val previousPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousPosition)
        notifyItemChanged(position)
    }

    // 주간 달력 사이즈 맞지 않음 수정 필요

}