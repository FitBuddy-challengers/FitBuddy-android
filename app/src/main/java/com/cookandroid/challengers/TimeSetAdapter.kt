package com.cookandroid.challengers

import com.cookandroid.challengers.api.RetrofitClient
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.widget.doAfterTextChanged
import com.cookandroid.challengers.R
import com.cookandroid.challengers.api.RetrofitClient.TimeSetUiModel

class TimeSetAdapter(private val sets: MutableList<TimeSetUiModel>) :
    RecyclerView.Adapter<TimeSetAdapter.ViewHolder>() {

    fun addSet() {
        sets.add(TimeSetUiModel(sets.size + 1, 10, 0f)) // 기본 10분 0kg
        notifyItemInserted(sets.size - 1)
    }

    fun getSetList(): List<TimeSetUiModel> = sets

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val setNumberText: TextView = view.findViewById(R.id.setNumberTextView)
        private val minEdit: EditText = view.findViewById(R.id.weightEditText)    // 분 입력
        private val weightEdit: EditText = view.findViewById(R.id.repsEditText)   // 무게 입력
        private val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)

        fun bind(set: TimeSetUiModel) {
            setNumberText.text = "${adapterPosition + 1}세트"

            // 입력 필드에 값 설정
            if (minEdit.text.toString() != set.minutes.toString()) {
                minEdit.setText(set.minutes.toString())
            }

            if (weightEdit.text.toString() != set.weight.toString()) {
                weightEdit.setText(set.weight.toString())
            }

            // 값 변경 시 데이터 반영
            minEdit.doAfterTextChanged {
                set.minutes = it.toString().toIntOrNull() ?: 0
            }

            weightEdit.doAfterTextChanged {
                set.weight = it.toString().toFloatOrNull() ?: 0f
            }

            deleteButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    sets.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, sets.size)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_edit_time, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = sets.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sets[position])
    }
}