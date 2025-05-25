package com.cookandroid.challengers


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.cookandroid.challengers.R
import com.cookandroid.challengers.api.RetrofitClient.RepsSetUiModel

class RepsSetAdapter(private val sets: MutableList<RepsSetUiModel>) :
    RecyclerView.Adapter<RepsSetAdapter.ViewHolder>() {

    fun addSet() {
        sets.add(RepsSetUiModel(sets.size + 1, 12, 0f))
        notifyItemInserted(sets.size - 1)
    }

    fun getSetList(): List<RepsSetUiModel> = sets

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val repsEdit: EditText = view.findViewById(R.id.repsEditText)
        private val weightEdit: EditText = view.findViewById(R.id.weightEditText)

        fun bind(set: RepsSetUiModel) {
            repsEdit.setText(set.reps.toString())
            weightEdit.setText(set.weight.toString())

            repsEdit.doAfterTextChanged {
                set.reps = it.toString().toIntOrNull() ?: 0
            }
            weightEdit.doAfterTextChanged {
                set.weight = it.toString().toFloatOrNull() ?: 0f
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_edit_set, parent, false)
    )

    override fun getItemCount(): Int = sets.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sets[position])
    }
}