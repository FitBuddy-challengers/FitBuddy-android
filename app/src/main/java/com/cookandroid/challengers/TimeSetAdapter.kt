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
        // 기본값: 0시 10분 0초
        sets.add(TimeSetUiModel(sets.size + 1, 0, 10, 0, 0f))
        notifyItemInserted(sets.size - 1)
    }

    fun getSetList(): List<TimeSetUiModel> = sets

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val setNumberText: TextView = view.findViewById(R.id.setNumberTextView)
        private val hourEdit: EditText = view.findViewById(R.id.hourEditText)
        private val minEdit: EditText = view.findViewById(R.id.minEditText)
        private val secEdit: EditText = view.findViewById(R.id.secEditText)
        // private val weightEdit: EditText = view.findViewById(R.id.someWeightEditTextId)
        private val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)


        fun bind(set: TimeSetUiModel) {
            setNumberText.text = "${bindingAdapterPosition + 1}세트" // RecyclerView의 최신 위치 사용

            // 3. 시간, 분, 초 값을 EditText에 설정
            if (hourEdit.text.toString() != set.hours.toString()) {
                hourEdit.setText(set.hours.toString())
            }
            if (minEdit.text.toString() != set.minutes.toString()) {
                minEdit.setText(set.minutes.toString())
            }
            if (secEdit.text.toString() != set.seconds.toString()) {
                secEdit.setText(set.seconds.toString())
            }

            // 값 변경 시 데이터 모델에 반영
            hourEdit.doAfterTextChanged { text ->
                sets.getOrNull(bindingAdapterPosition)?.hours = text.toString().toIntOrNull() ?: 0
            }
            minEdit.doAfterTextChanged { text ->
                sets.getOrNull(bindingAdapterPosition)?.minutes = text.toString().toIntOrNull() ?: 0
            }
            secEdit.doAfterTextChanged { text ->
                sets.getOrNull(bindingAdapterPosition)?.seconds = text.toString().toIntOrNull() ?: 0
            }

            deleteButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    sets.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, sets.size - position)
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