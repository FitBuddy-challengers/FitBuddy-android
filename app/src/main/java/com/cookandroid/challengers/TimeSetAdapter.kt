package com.cookandroid.challengers

import com.cookandroid.challengers.api.RetrofitClient

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import androidx.core.widget.doAfterTextChanged
import com.cookandroid.challengers.R
import com.cookandroid.challengers.api.RetrofitClient.TimeSetUiModel

class TimeSetAdapter(private val sets: MutableList<RetrofitClient.TimeSetUiModel>) :
    RecyclerView.Adapter<TimeSetAdapter.ViewHolder>() {

    fun addSet() {
        sets.add(TimeSetUiModel(sets.size + 1, 10, 0f))
        notifyItemInserted(sets.size - 1)
    }

    fun getSetList(): List<TimeSetUiModel> = sets

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val minEdit: EditText = view.findViewById(R.id.weightEditText)
        private val weightEdit: EditText = view.findViewById(R.id.repsEditText)

        fun bind(set: TimeSetUiModel) {
            minEdit.setText(set.minutes.toString())
            weightEdit.setText(set.weight.toString())

            minEdit.doAfterTextChanged {
                set.minutes = it.toString().toIntOrNull() ?: 0
            }
            weightEdit.doAfterTextChanged {
                set.weight = it.toString().toFloatOrNull() ?: 0f
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_edit_time, parent, false)
    )

    override fun getItemCount(): Int = sets.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sets[position])
    }
}