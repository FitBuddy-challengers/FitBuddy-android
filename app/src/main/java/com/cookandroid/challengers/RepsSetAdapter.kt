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

    // 서버 응답으로 받은 세트로 전체 갱신할 때 호출
    fun updateSets(newSets: List<RepsSetUiModel>) {
        sets.clear()
        sets.addAll(newSets)
        notifyDataSetChanged()
    }

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_edit_set, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sets[position])
    }

    override fun getItemCount(): Int = sets.size
}