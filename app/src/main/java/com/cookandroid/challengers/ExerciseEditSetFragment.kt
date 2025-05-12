package com.cookandroid.challengers

import android.app.Dialog
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.cookandroid.challengers.data.ExerciseSet
import com.cookandroid.challengers.data.ExerciseSetDao
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.ItemEditSetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList

class ExerciseEditSetFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "ExerciseEditSetFragment"
        private const val ARG_EXERCISE_ID = "exerciseId"
        private const val ARG_INITIAL_SET_LIST = "initialSetList"
        private const val ARG_HIGHLIGHT_INDEX = "highlightIndex"
        private const val ARG_EQUIP = "equip"

        fun newInstance(
            exerciseId: Long,
            initialSetList: List<ExerciseSet>,
            highlightIndex: Int,
            equip: String?
        ): ExerciseEditSetFragment {
            return ExerciseEditSetFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_EXERCISE_ID, exerciseId)
                    putParcelableArrayList(ARG_INITIAL_SET_LIST, ArrayList(initialSetList))
                    putInt(ARG_HIGHLIGHT_INDEX, highlightIndex)
                    putString(ARG_EQUIP, equip)
                }
            }
        }

        fun newInstance(
            exerciseId: Long,
            initialSetList: List<ExerciseSet>,
            highlightIndex: Int
        ): ExerciseEditSetFragment {
            return newInstance(exerciseId, initialSetList, highlightIndex, null)
        }
    }

    private lateinit var exerciseSetDao: ExerciseSetDao
    private lateinit var adapter: EditSetAdapter
    private var exerciseId: Long = -1L
    private val initialSetList: MutableList<ExerciseSet> = mutableListOf()
    private var onSetsUpdatedCallback: ((List<ExerciseSet>) -> Unit)? = null
    private var currentEquip: String? = null

    fun setOnSetsUpdatedListener(listener: (List<ExerciseSet>) -> Unit) {
        onSetsUpdatedCallback = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exerciseId = arguments?.getLong(ARG_EXERCISE_ID) ?: -1L
        currentEquip = arguments?.getString(ARG_EQUIP)
        arguments
            ?.getParcelableArrayList<ExerciseSet>(ARG_INITIAL_SET_LIST)
            ?.let { initialSetList.addAll(it) }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        dialog.setOnShowListener {
            val bottomSheet =
                (it as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.background = ContextCompat.getDrawable(
                dialog.context,
                R.drawable.bottom_sheet_background
            )
            BottomSheetBehavior.from(bottomSheet!!).apply {
                val h = resources.getDimensionPixelSize(R.dimen.rest_timer_peek_height)
                peekHeight = h
                maxHeight = h
                state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_exercise_edit_set, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
        exerciseSetDao = db.exerciseSetDao()
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewSetList)
        val addButton = view.findViewById<Button>(R.id.buttonAddSet)
        val highlightIndex = arguments?.getInt(ARG_HIGHLIGHT_INDEX, -1) ?: -1

        adapter = EditSetAdapter(currentEquip)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val initial = initialSetList.sortedBy { it.setNumber }
        val withHighlight = initial.mapIndexed { idx, set ->
            if (idx == highlightIndex) set.copy(isHighlighted = true, setNumber = idx + 1)
            else set.copy(setNumber = idx + 1)
        }
        adapter.submitList(withHighlight)

        addButton.setOnClickListener {
            val currentList = adapter.currentList.toMutableList()
            val newSet = ExerciseSet(
                id = 0,
                exerciseId = exerciseId,
                setNumber = currentList.size + 1,
                weight = currentList.lastOrNull()?.weight,
                reps = currentList.lastOrNull()?.reps ?: 0
            )
            currentList.add(newSet)
            adapter.updateSetNumbers(currentList)
            adapter.submitList(currentList)
            recyclerView.scrollToPosition(currentList.size - 1)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        view?.findFocus()?.clearFocus()
        saveSets()
    }

    private fun saveSets() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val currentList = adapter.currentList.toList()
            val initialSnapshot = initialSetList.toList()
            val diffCallback = object : DiffUtil.Callback() {
                override fun getOldListSize() = initialSnapshot.size
                override fun getNewListSize() = currentList.size
                override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                    initialSnapshot.getOrNull(oldPos)?.id == currentList.getOrNull(newPos)?.id
                override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                    initialSnapshot.getOrNull(oldPos) == currentList.getOrNull(newPos)
            }
            val diff = DiffUtil.calculateDiff(diffCallback)
            val toInsert = mutableListOf<ExerciseSet>()
            val toUpdate = mutableListOf<ExerciseSet>()
            val toDelete = mutableListOf<ExerciseSet>()

            diff.dispatchUpdatesTo(object : ListUpdateCallback {
                override fun onInserted(pos: Int, cnt: Int) =
                    (pos until pos + cnt).forEach { currentList.getOrNull(it)?.let(toInsert::add) }
                override fun onRemoved(pos: Int, cnt: Int) =
                    (pos until pos + cnt).forEach { initialSnapshot.getOrNull(it)?.let(toDelete::add) }
                override fun onMoved(from: Int, to: Int) {}
                override fun onChanged(pos: Int, cnt: Int, payload: Any?) =
                    (pos until pos + cnt).forEach { currentList.getOrNull(it)?.let(toUpdate::add) }
            })

            toInsert.forEach { exerciseSetDao.insert(it) }
            toDelete.forEach { exerciseSetDao.delete(it) }
            toUpdate.forEach { exerciseSetDao.update(it) }

            withContext(Dispatchers.Main) {
                onSetsUpdatedCallback?.invoke(currentList)
                initialSetList.clear()
                initialSetList.addAll(currentList)
            }
        }
    }

    inner class EditSetAdapter(private val equip: String?) :
        ListAdapter<ExerciseSet, EditSetAdapter.ViewHolder>(object :
            DiffUtil.ItemCallback<ExerciseSet>() {
            override fun areItemsTheSame(old: ExerciseSet, new: ExerciseSet) =
                old.id == new.id && old.exerciseId == new.exerciseId
            override fun areContentsTheSame(old: ExerciseSet, new: ExerciseSet) = old == new
        }) {

        fun updateSetNumbers(list: MutableList<ExerciseSet>) {
            list.forEachIndexed { i, s -> s.setNumber = i + 1 }
        }

        inner class ViewHolder(val binding: ItemEditSetBinding) :
            RecyclerView.ViewHolder(binding.root) {

            private val weightWatcher = object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    if (!binding.weightEditText.hasFocus()) updateWeightAndReps()
                }
            }
            private val repsWatcher = object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    if (!binding.repsEditText.hasFocus()) updateWeightAndReps()
                }
            }

            init {
                binding.deleteButton.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        val list = currentList.toMutableList()
                        val removed = list.removeAt(pos)
                        updateSetNumbers(list)
                        submitList(list)
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            removed.id?.let { id ->
                                exerciseSetDao.deleteById(id)
                            }
                        }
                    }
                }

                binding.weightEditText.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) binding.weightEditText.addTextChangedListener(weightWatcher)
                    else {
                        binding.weightEditText.removeTextChangedListener(weightWatcher)
                        updateWeightAndReps()
                    }
                }
                binding.weightEditText.setOnKeyListener { _, _, _ ->
                    binding.repsEditText.requestFocus()
                    false
                }
                binding.repsEditText.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) binding.repsEditText.addTextChangedListener(repsWatcher)
                    else {
                        binding.repsEditText.removeTextChangedListener(repsWatcher)
                        updateWeightAndReps()
                    }
                }
            }

            private fun updateWeightAndReps() {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return
                val current = getItem(pos)
                val w = binding.weightEditText.text.toString().toIntOrNull()
                val r = binding.repsEditText.text.toString().toIntOrNull() ?: 0
                if ((w != null && w != current.weight) || r != current.reps) {
                    val upd = current.copy(weight = w, reps = r, setNumber = pos + 1)
                    updateItem(upd, pos)
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        exerciseSetDao.update(upd)
                    }
                }
            }

            fun bind(set: ExerciseSet) {
                binding.setNumberTextView.text = "${bindingAdapterPosition + 1}세트"
                binding.repsEditText.setText(set.reps.toString())
                binding.repsEditText.inputType = InputType.TYPE_CLASS_NUMBER

                // 장비에 따라 무게 필드 숨기기
                if (equip in listOf("맨몸", "스텝박스", "세라밴드")) {
                    binding.weightEditText.visibility = View.GONE
                    binding.weightText.visibility = View.GONE
                } else {
                    binding.weightEditText.visibility = View.VISIBLE
                    binding.weightText.visibility = View.VISIBLE
                    binding.weightEditText.setText(set.weight?.toString() ?: "")
                    binding.weightEditText.inputType = InputType.TYPE_CLASS_NUMBER
                }

                binding.weightEditText.isEnabled = !set.isCompleted
                binding.repsEditText.isEnabled = !set.isCompleted

                when {
                    set.isCompleted   -> binding.itemRoot.setBackgroundResource(R.drawable.set_item_background_completed2)
                    set.isHighlighted -> binding.itemRoot.setBackgroundResource(R.drawable.set_item_background_emphasized)
                    else              -> binding.itemRoot.setBackgroundResource(R.drawable.set_item_background)
                }

                val hColor = ContextCompat.getColor(binding.root.context, R.color.light_gray2)
                val dColor = ContextCompat.getColor(binding.root.context, R.color.white)
                binding.weightEditText.backgroundTintList = ColorStateList.valueOf(if (set.isHighlighted) hColor else dColor)
                binding.repsEditText.backgroundTintList  = ColorStateList.valueOf(if (set.isHighlighted) hColor else dColor)
            }
        }

        private fun updateItem(newItem: ExerciseSet, position: Int) {
            val list = currentList.toMutableList()
            if (position < list.size) {
                list[position] = newItem
                submitList(list)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(ItemEditSetBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(getItem(position))
    }
}
