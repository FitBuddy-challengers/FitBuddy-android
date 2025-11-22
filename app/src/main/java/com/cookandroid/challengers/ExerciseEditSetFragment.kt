package com.cookandroid.challengers

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.databinding.FragmentExerciseEditSetBinding
import com.cookandroid.challengers.databinding.ItemEditSetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
class ExerciseEditSetFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentExerciseEditSetBinding? = null
    private val binding get() = _binding!!

    private lateinit var setAdapter: EditSetAdapter
    private var scheduleId: Long = -1L

    data class RepsSetUiModel(
        val id: Long,
        val setNumber: Int,
        val reps: Int,
        val weight: Float,
        val isCompleted: Boolean
    )

    companion object {
        const val TAG = "RepsSetEditDebug" // 로그 확인을 위해 태그 변경
        private const val ARG_SCHEDULE_ID = "scheduleId"
        private val idCounter = AtomicLong(System.currentTimeMillis())

        fun newInstance(scheduleId: Long): ExerciseEditSetFragment {
            return ExerciseEditSetFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_SCHEDULE_ID, scheduleId)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheet = (dialogInterface as BottomSheetDialog)
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            bottomSheet?.let {
                it.background = ContextCompat.getDrawable(requireContext(), R.drawable.bottom_sheet_background)

                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            scheduleId = it.getLong(ARG_SCHEDULE_ID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExerciseEditSetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        loadSetsFromServer()
    }

    private fun setupRecyclerView() {
        setAdapter = EditSetAdapter(
            onItemChanged = { updatedItem ->
                val newList = setAdapter.currentList.map { item ->
                    if (item.id == updatedItem.id) updatedItem else item
                }
                setAdapter.submitList(newList)
            },
            onDeleteItem = { itemToDelete ->
                val oldList = setAdapter.currentList
                Log.d(TAG, "Deleting item: $itemToDelete")
                val newList = oldList
                    .filter { it.id != itemToDelete.id }
                    .mapIndexed { index, item -> item.copy(setNumber = index + 1) }
                Log.d(TAG, "New list after deletion: $newList")
                setAdapter.submitList(newList)
            }
        )
        binding.recyclerViewSetList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = setAdapter
        }
    }

    private fun setupClickListeners() {
        binding.buttonAddSet.setOnClickListener {
            val oldList = setAdapter.currentList
            val lastSet = oldList.lastOrNull()
            val newSet = RepsSetUiModel(
                id = idCounter.incrementAndGet(),
                setNumber = oldList.size + 1,
                reps = lastSet?.reps ?: 12,
                weight = lastSet?.weight ?: 0f,
                isCompleted = false
            )
            setAdapter.submitList(oldList + newSet)
            binding.recyclerViewSetList.smoothScrollToPosition(setAdapter.currentList.size - 1)
        }

        binding.buttonSaveSet.setOnClickListener {
            saveChangesToServer()
        }
    }

    private fun loadSetsFromServer() {
        if (scheduleId == -1L) return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.scheduleApi.getRepsSets(scheduleId).execute()
                if (response.isSuccessful) {
                    val dtoList = response.body() ?: emptyList()
                    // ⭐️ STEP 1: 서버에서 받은 원본 데이터 확인
                    Log.d(TAG, "1. DTO from Server: $dtoList")

                    val uiModelList = dtoList.map { dto ->
                        RepsSetUiModel(
                            id = idCounter.incrementAndGet(),
                            setNumber = dto.setNumber,
                            reps = dto.reps,
                            weight = dto.weight,
                            isCompleted = dto.isCompleted
                        )
                    }
                    // ⭐️ STEP 2: 어댑터에 전달할 UI 모델 데이터 확인
                    Log.d(TAG, "2. Mapped UI Model List: $uiModelList")

                    withContext(Dispatchers.Main) {
                        setAdapter.submitList(uiModelList)
                    }
                } else {
                    Log.e(TAG, "Server error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network exception", e)
            }
        }
    }

    private fun saveChangesToServer() {
        if (scheduleId == -1L) return
        val dtoList = setAdapter.currentList.map { uiModel ->
            RetrofitClient.RepsSetDto(
                setNumber = uiModel.setNumber,
                reps = uiModel.reps,
                weight = uiModel.weight,
                isCompleted = uiModel.isCompleted
            )
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.scheduleApi.updateRepsSets(scheduleId, dtoList).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "세트가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        setFragmentResult("sets_updated", Bundle())
                        dismiss()
                    } else {
                        Toast.makeText(context, "저장 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "네트워크 오류로 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class EditSetAdapter(
        private val onItemChanged: (RepsSetUiModel) -> Unit,
        private val onDeleteItem: (RepsSetUiModel) -> Unit
    ) : ListAdapter<RepsSetUiModel, EditSetAdapter.ViewHolder>(RepsSetDiffCallback()) {

        inner class ViewHolder(val binding: ItemEditSetBinding) : RecyclerView.ViewHolder(binding.root) {
            private val textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (bindingAdapterPosition == RecyclerView.NO_POSITION) return
                    val currentItem = getItem(bindingAdapterPosition)
                    val updatedItem = currentItem.copy(
                        weight = binding.weightEditText.text.toString().toFloatOrNull() ?: 0f,
                        reps = binding.repsEditText.text.toString().toIntOrNull() ?: 0
                    )
                    onItemChanged(updatedItem)
                }
                override fun afterTextChanged(s: Editable?) {}
            }

            fun bind(set: RepsSetUiModel) {
                // ⭐️ STEP 3: 각 아이템이 UI에 바인딩될 때의 데이터 확인
                Log.d(TAG, "3. Binding item at position $bindingAdapterPosition with data: $set")

                binding.weightEditText.removeTextChangedListener(textWatcher)
                binding.repsEditText.removeTextChangedListener(textWatcher)

                binding.setNumberTextView.text = "${set.setNumber}세트"
                binding.weightEditText.setText(if (set.weight == 0f) "" else set.weight.toString().removeSuffix(".0"))
                binding.repsEditText.setText(if (set.reps == 0) "" else set.reps.toString())

                binding.deleteButton.setOnClickListener {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        onDeleteItem(getItem(bindingAdapterPosition))
                    }
                }

                binding.weightEditText.addTextChangedListener(textWatcher)
                binding.repsEditText.addTextChangedListener(textWatcher)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemEditSetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private class RepsSetDiffCallback : DiffUtil.ItemCallback<RepsSetUiModel>() {
        override fun areItemsTheSame(oldItem: RepsSetUiModel, newItem: RepsSetUiModel): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: RepsSetUiModel, newItem: RepsSetUiModel): Boolean {
            return oldItem == newItem
        }
    }
}