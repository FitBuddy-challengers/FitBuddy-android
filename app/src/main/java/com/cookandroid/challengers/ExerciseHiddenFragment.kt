package com.cookandroid.challengers

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Parcelable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.*
import com.cookandroid.challengers.data.Exercise
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentExerciseHiddenBinding
import com.cookandroid.challengers.databinding.ItemAddExerciseBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.drawable.toDrawable

class ExerciseHiddenFragment : Fragment() {

    private var _binding: FragmentExerciseHiddenBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HiddenExerciseListAdapter
    private lateinit var db: AppDatabase
    private var recyclerViewState: Parcelable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseHiddenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)

        setupAdapter()
        binding.exerciseListRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ExerciseHiddenFragment.adapter
            itemAnimator = null
        }

        adapter.setContext(requireContext())
        setupSwipeToUnhide()
        observeHiddenExercises()
        initChips()

        binding.backButton.setOnClickListener { findNavController().popBackStack() }
    }

    private fun setupAdapter() {
        adapter = HiddenExerciseListAdapter(
            onItemClicked = { exercise ->
                // 필요하다면 상세 화면 이동 로직 추가
            },
            onUnhideClicked = { exercise ->
                unhideExercise(exercise)
            }
        )
    }

    private fun unhideExercise(exercise: Exercise) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.exerciseDao().update(exercise.copy(isHidden = false))
        }
    }

    private fun observeHiddenExercises() {
        viewLifecycleOwner.lifecycleScope.launch {
            db.exerciseDao().getHiddenExercises().collectLatest { hiddenExercises -> // 이름 변경
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        adapter.submitList(hiddenExercises)
                        recyclerViewState?.let {
                            binding.exerciseListRecyclerView.layoutManager?.onRestoreInstanceState(it)
                            recyclerViewState = null
                        }
                    }
                }
            }
        }
    }

    private fun setupSwipeToUnhide() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val exercise = adapter.currentList[position]
                unhideExercise(exercise) // 어댑터의 unhide 콜백 대신 Fragment 함수 호출
            }

            override fun onChildDraw(
                c: Canvas,
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = vh.itemView
                val color = ContextCompat.getColor(requireContext(), R.color.light_gray2)
                val background = color.toDrawable()
                background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                background.draw(c)

                val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_unhide)
                icon?.let {
                    val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                    val iconTop = itemView.top + iconMargin
                    val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    val iconBottom = iconTop + it.intrinsicHeight
                    it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    it.draw(c)
                }

                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.exerciseListRecyclerView)
    }

    override fun onPause() {
        super.onPause()
        recyclerViewState = binding.exerciseListRecyclerView.layoutManager?.onSaveInstanceState()
    }

    private fun initChips() {
        setupChips(binding.myChipGroup, listOf("즐겨찾기", "최근 한 운동"))
        setupChips(binding.partChipGroup, listOf("가슴", "등", "하체", "어깨", "복근", "유산소"))
        setupChips(binding.equipmentChipGroup, listOf("맨몸", "덤벨", "케틀벨", "세라밴드", "스텝박스"))
    }

    private fun setupChips(group: ChipGroup, texts: List<String>) {
        val spacing = resources.getDimensionPixelSize(R.dimen.chip_spacing)
        group.chipSpacingHorizontal = spacing
        val defaultCornerRadius = resources.getDimension(R.dimen.default_chip_corner_radius)
        val selectedCornerRadius = resources.getDimension(R.dimen.selected_chip_corner_radius)
        val selectedPaddingHorizontal = resources.getDimensionPixelSize(R.dimen.selected_chip_padding_horizontal)
        val selectedPaddingVertical = resources.getDimensionPixelSize(R.dimen.selected_chip_padding_vertical)
        val selectedCloseIconStartPadding = resources.getDimensionPixelSize(R.dimen.selected_close_icon_start_padding).toFloat()

        texts.forEach { txt ->
            Chip(ContextThemeWrapper(context, R.style.TextChip)).apply {
                text = txt
                isChipIconVisible = false
                isCheckedIconVisible = false
                isCheckable = true
                isClickable = true
                shapeAppearanceModel = ShapeAppearanceModel.builder()
                    .setAllCorners(CornerFamily.ROUNDED, defaultCornerRadius)
                    .build()
                setPadding(0, 0, 0, 0)
                textStartPadding = 0f
                textEndPadding = 0f
            }.also { chip ->
                chip.setTextAppearance(R.style.TextChip)
                chip.setChipBackgroundColorResource(android.R.color.transparent)

                chip.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        chip.setTextAppearance(R.style.SelectedChip)
                        chip.setChipBackgroundColorResource(R.color.blue)
                        chip.shapeAppearanceModel = ShapeAppearanceModel.builder()
                            .setAllCorners(CornerFamily.ROUNDED, selectedCornerRadius)
                            .build()
                        chip.setCloseIconResource(R.drawable.ic_close)
                        chip.closeIconTint = ColorStateList.valueOf(Color.WHITE)
                        chip.isChipIconVisible = false
                        chip.isCloseIconVisible = true
                        chip.setPadding(selectedPaddingHorizontal, 0, selectedPaddingHorizontal, 0)
                        chip.closeIconStartPadding = selectedCloseIconStartPadding
                        chip.closeIconEndPadding = 0f
                        chip.textEndPadding = if (chip.isCloseIconVisible) selectedCloseIconStartPadding else 0f
                    } else {
                        chip.setTextAppearance(R.style.TextChip)
                        chip.setChipBackgroundColorResource(android.R.color.transparent)
                        chip.shapeAppearanceModel = ShapeAppearanceModel.builder()
                            .setAllCorners(CornerFamily.ROUNDED, defaultCornerRadius)
                            .build()
                        chip.isCloseIconVisible = false
                        chip.isChipIconVisible = false
                        chip.setPadding(0, 0, 0, 0)
                        chip.textEndPadding = 0f
                    }
                    applyFilters()
                }
                chip.setOnCloseIconClickListener {
                    chip.isChecked = false
                }
                group.addView(chip)
            }
        }
    }

    private fun applyFilters() {
        viewLifecycleOwner.lifecycleScope.launch {
            db.exerciseDao().getHiddenExercises().collectLatest { hiddenExercises ->
                val favFilter = binding.myChipGroup.checkedChipIds.any {
                    binding.myChipGroup.findViewById<Chip>(it).text == "즐겨찾기"
                }
                val parts = binding.partChipGroup.checkedChipIds.map {
                    binding.partChipGroup.findViewById<Chip>(it).text.toString()
                }
                val equips = binding.equipmentChipGroup.checkedChipIds.map {
                    binding.equipmentChipGroup.findViewById<Chip>(it).text.toString()
                }

                val filtered = hiddenExercises.filter { e ->
                    (!favFilter || e.isFavorite) &&
                            (parts.isEmpty() || parts.any { e.part.contains(it) }) &&
                            (equips.isEmpty() || equips.any { e.equip.contains(it) })
                }

                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        adapter.submitList(filtered)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private class HiddenExerciseListAdapter(
    private val onItemClicked: (Exercise) -> Unit,
    private val onUnhideClicked: (Exercise) -> Unit // 숨김 해제 콜백
) : ListAdapter<Exercise, HiddenExerciseListAdapter.HiddenExerciseViewHolder>(ExerciseDiffCallback()) {

    private lateinit var context: Context

    fun setContext(context: Context) {
        this.context = context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HiddenExerciseViewHolder {
        context = parent.context
        val binding = ItemAddExerciseBinding.inflate(LayoutInflater.from(context), parent, false)
        return HiddenExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HiddenExerciseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HiddenExerciseViewHolder(private val binding: ItemAddExerciseBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { onItemClicked(it) }
                }
            }
            // 즐겨찾기 버튼 필요하다면 추가
        }

        fun bind(exercise: Exercise) {
            binding.exerciseNameTextView.text = exercise.name
            // 즐겨찾기 상태 표시 필요하다면 추가
            val resId = context.resources.getIdentifier(
                exercise.imagePath ?: "", "drawable", context.packageName
            )
            binding.exerciseImageView.setImageResource(
                if (resId != 0) resId else R.drawable.ic_launcher_background
            )
        }
    }
}

private class ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
    override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean = oldItem == newItem
}