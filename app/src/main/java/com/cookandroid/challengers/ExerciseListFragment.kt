package com.cookandroid.challengers

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.*
import com.cookandroid.challengers.data.Exercise
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentExerciseListBinding
import com.cookandroid.challengers.databinding.ItemExerciseListBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit
import android.view.GestureDetector
import android.view.MotionEvent

class ItemClickListener(
    context: Context,
    recyclerView: RecyclerView,
    private val listener: (View, Int) -> Unit
) : RecyclerView.OnItemTouchListener {

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val child = recyclerView.findChildViewUnder(e.x, e.y)
            if (child != null) {
                val position = recyclerView.getChildAdapterPosition(child)
                if (position != RecyclerView.NO_POSITION) {
                    listener(child, position)
                    return true
                }
            }
            return false
        }
    })

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(e)
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        // No-op
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // No-op
    }
}


class ExerciseListFragment : Fragment() {

    private var _binding: FragmentExerciseListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ExerciseListAdapter
    private lateinit var db: AppDatabase
    private var recyclerViewState: Parcelable? = null
    private var recentlyHiddenExercise: Exercise? = null
    private val todayPlannedExerciseIds = mutableSetOf<Long>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExerciseListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)

        setupAdapter()
        binding.exerciseListRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ExerciseListFragment.adapter
            itemAnimator = null
        }

        adapter.setContext(requireContext())
        setupSwipeToHide()
        observeExercises()
        setupChipGroups()
        setupButtonClickListeners()

        loadTodayPlannedExercises()
//
//        binding.exerciseListRecyclerView.addOnItemTouchListener(
//            ItemClickListener(requireContext(), binding.exerciseListRecyclerView) { _, position ->
//                val exercise = adapter.currentList[position]
//                recyclerViewState = binding.exerciseListRecyclerView.layoutManager?.onSaveInstanceState()
//                navigateToDetail(exercise.id)
//            }
//        )
    }

    private fun loadTodayPlannedExercises() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val todayStartMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val todayEndMillis = todayStartMillis + TimeUnit.DAYS.toMillis(1) - 1

            Log.d("ExerciseListFragment", "Today Start Millis: $todayStartMillis, End Millis: $todayEndMillis")

            val todayExercisePlans = db.exercisePlanDao().getExercisePlansByDateRange(todayStartMillis, todayEndMillis)
            Log.d("ExerciseListFragment", "Today Exercise Plans: ${todayExercisePlans.size}")

            val plannedIds = mutableSetOf<Long>()
            todayExercisePlans.forEach { plan ->
                val details = db.planDetailDao().getPlanDetailsByExercisePlanIdOnce(plan.id)
                Log.d("ExerciseListFragment", "Plan ID: ${plan.id}, Details Count: ${details.size}")
                details.forEach { detail ->
                    plannedIds.add(detail.exerciseId)
                    Log.d("ExerciseListFragment", "Planned Exercise ID: ${detail.exerciseId}")
                }
            }
            withContext(Dispatchers.Main) {
                todayPlannedExerciseIds.addAll(plannedIds)
                Log.d("ExerciseListFragment", "Today Planned Exercise IDs: $todayPlannedExerciseIds")
            }
        }
    }

    private fun setupAdapter() {
        adapter = ExerciseListAdapter(
            onItemClicked = { exercise ->
                recyclerViewState = binding.exerciseListRecyclerView.layoutManager?.onSaveInstanceState()
                navigateToDetail(exercise.id)
            },
            onFavoriteClicked = { ex ->
                updateFavorite(ex)
            }
        )
    }

    private fun navigateToDetail(exerciseId: Long) {
        val bundle = Bundle().apply {
            putLong("exerciseId", exerciseId)
        }
        findNavController().navigate(R.id.action_global_exerciseDetailFragment, bundle)
    }

    private fun updateFavorite(exercise: Exercise) {
        Log.d("ExerciseList", "updateFavorite called for exercise ID: ${exercise.id}, new isFavorite: ${!exercise.isFavorite}")
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.exerciseDao().update(exercise.copy(isFavorite = !exercise.isFavorite))
            val updatedExercise = db.exerciseDao().getExerciseById(exercise.id) // 업데이트 후 데이터 확인
            withContext(Dispatchers.Main) {
                Log.d("ExerciseList", "Database updated - ID: ${updatedExercise?.id}, isFavorite: ${updatedExercise?.isFavorite}")
            }
        }
    }

    private fun hideExercise(exercise: Exercise) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            recentlyHiddenExercise = exercise
            db.exerciseDao().update(exercise.copy(isHidden = true))
            withContext(Dispatchers.Main) {
                showUndoSnackbar(exercise.name)
            }
        }
    }

    private fun undoHideExercise() {
        recentlyHiddenExercise?.let { exercise ->
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                db.exerciseDao().update(exercise.copy(isHidden = false))
                recentlyHiddenExercise = null
            }
        }
    }

    private fun showUndoSnackbar(exerciseName: String) {
        binding.root.let { view ->
            Snackbar.make(view, "${exerciseName} 숨김", Snackbar.LENGTH_LONG)
                .setActionTextColor(ContextCompat.getColor(requireContext(), R.color.blue))
                .setAction("취소") { undoHideExercise() }
                .show()
        }
    }

    private fun observeExercises() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.exerciseDao().getAllExercises().collectLatest { all ->
                val visibleExercises = all.filter { !it.isHidden }
                withContext(Dispatchers.Main) {
                    if (_binding != null) {
                        adapter.submitList(visibleExercises)
                        recyclerViewState?.let {
                            binding.exerciseListRecyclerView.layoutManager?.onRestoreInstanceState(it)
                            recyclerViewState = null
                        }
                    }
                }
            }
        }
    }

    private fun setupSwipeToHide() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private var isToastShown = false

            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val exercise = adapter.currentList[position]
                    if (todayPlannedExerciseIds.contains(exercise.id)) {
                        if (!isToastShown) {
                            Toast.makeText(requireContext(), "오늘 계획에 있는 운동은 숨길 수 없습니다.", Toast.LENGTH_SHORT).show()
                            isToastShown = true
                        }
                        return 0
                    } else {
                        isToastShown = false
                        return ItemTouchHelper.LEFT
                    }
                }
                return 0
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val exercise = adapter.currentList[position]
                hideExercise(exercise)
            }

            override fun onChildDraw(
                c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = vh.itemView
                val color = ContextCompat.getColor(requireContext(), R.color.light_gray2)
                val background = ColorDrawable(color)
                background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                background.draw(c)

                val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_hide)
                icon?.let {
                    val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                    val iconTop = itemView.top + iconMargin
                    val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    val iconBottom = iconTop + it.intrinsicHeight
                    it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    it.draw(c)
                }
                itemView.translationX = dX
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.exerciseListRecyclerView)
    }

    private fun setupChipGroups() {
        setupChips(binding.myChipGroup, listOf("즐겨찾기", "최근 한 운동"))
        setupChips(binding.partChipGroup, listOf("가슴", "등", "하체", "어깨", "복근", "유산소"))
        setupChips(binding.equipmentChipGroup, listOf("맨몸", "덤벨", "케틀벨", "세라밴드", "스텝박스"))
    }

    private fun setupButtonClickListeners() {
        binding.backButton.setOnClickListener { findNavController().popBackStack() }
        binding.hiddenExerciseButton.setOnClickListener {
            findNavController().navigate(R.id.action_exerciseList_to_exerciseHidden)
        }
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
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.exerciseDao().getAllExercises().collectLatest { all ->
                val visibleExercises = all.filter { !it.isHidden }  // 숨김 제외

                val favFilter = binding.myChipGroup.checkedChipIds.any {
                    binding.myChipGroup.findViewById<Chip>(it).text == "즐겨찾기"
                }
                val parts = binding.partChipGroup.checkedChipIds.map {
                    binding.partChipGroup.findViewById<Chip>(it).text.toString()
                }
                val equips = binding.equipmentChipGroup.checkedChipIds.map {
                    binding.equipmentChipGroup.findViewById<Chip>(it).text.toString()
                }

                val filtered = visibleExercises.filter { e ->
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


    private class ExerciseListAdapter(
        private val onItemClicked: (Exercise) -> Unit,
        private val onFavoriteClicked: (Exercise) -> Unit
    ) : ListAdapter<Exercise, ExerciseListAdapter.ExerciseViewHolder>(ExerciseDiffCallback()) {

        private lateinit var context: Context

        fun setContext(context: Context) {
            this.context = context
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
            context = parent.context
            val binding = ItemExerciseListBinding.inflate(LayoutInflater.from(context), parent, false)
            return ExerciseViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class ExerciseViewHolder(val binding: ItemExerciseListBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {
                // ★루트 대신 itemContentLayout 에만 클릭
                binding.itemContentLayout.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onItemClicked(getItem(pos))
                    }
                }
                binding.favoriteButtonContainer.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onFavoriteClicked(getItem(pos))
                    }
                }
            }

            fun bind(exercise: Exercise) {
                binding.exerciseNameTextView.text = exercise.name
                binding.favoriteButton.isSelected = exercise.isFavorite
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}