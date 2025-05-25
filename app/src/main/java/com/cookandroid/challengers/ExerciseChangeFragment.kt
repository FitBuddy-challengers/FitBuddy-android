package com.cookandroid.challengers

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.service.notification.Condition.newId
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.data.Exercise
import com.cookandroid.challengers.data.PlanDetail
import com.cookandroid.challengers.data.PlanDetailDao
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentExerciseChangeBinding
import com.cookandroid.challengers.databinding.ItemAddExerciseBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExerciseChangeFragment : BottomSheetDialogFragment() {

    //private var onExerciseChanged: ((Long) -> Unit)? = null

    companion object {
        private const val ARG_PLAN_ID = "planId"
        private const val ARG_EXERCISE_ID = "exerciseId"


        fun newInstance(planId: Long, exerciseId: Long) = ExerciseChangeFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_PLAN_ID, planId)
                putLong(ARG_EXERCISE_ID, exerciseId)
            }

        }
    }




    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    private var _binding: FragmentExerciseChangeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChangeExerciseAdapter
    private lateinit var planDetailDao: PlanDetailDao
    private lateinit var db: AppDatabase

    private var planId: Long = -1L
    private var exerciseId: Long = -1L
    private var existingExercises = listOf<PlanDetail>()
    private var selectedExercise: Exercise? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        planId = arguments?.getLong(ARG_PLAN_ID) ?: -1L
        exerciseId = arguments?.getLong(ARG_EXERCISE_ID) ?: -1L
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)

        dialog.behavior.apply {
            isDraggable = true
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
            peekHeight = resources.getDimensionPixelSize(R.dimen.rest_timer_peek_height)
        }

        dialog.setOnShowListener {
            val bottomSheet =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                sheet.layoutParams.height =
                    resources.getDimensionPixelSize(R.dimen.rest_timer_peek_height)
                sheet.requestLayout()
                sheet.background = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.bottom_sheet_background
                )
            }
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentExerciseChangeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
        planDetailDao = db.planDetailDao()

        adapter = ChangeExerciseAdapter(
            emptyList(),
            onExerciseSelected = { ex, isSelected ->
                if (isSelected) {
                    selectedExercise = ex
                } else {
                    selectedExercise = null
                }
            },
            onFavoriteClicked = { ex ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.exerciseDao().update(ex.copy(isFavorite = !ex.isFavorite))
                }
            }
        )
        adapter.setContext(requireContext())

        binding.exerciseListRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ExerciseChangeFragment.adapter
        }

        lifecycleScope.launch(Dispatchers.IO) {
            existingExercises = planDetailDao.getPlanDetailsForPlanId(planId) // planId 사용하여 PlanDetail 가져옴
            val initialRemainingSlots = 1
            withContext(Dispatchers.Main) {
                adapter.setMaxSelectableCount(initialRemainingSlots)
                db.exerciseDao().getAllExercises().collectLatest { all ->
                    val filtered = all.filter { e -> e.id != exerciseId }
                    adapter.submitList(filtered)

                    val currentExercise = all.find { it.id == exerciseId }
                    currentExercise?.let {
                        selectedExercise = it;
                        adapter.notifySelectionChanged(
                            it.id,
                            true
                        )
                    }
                }
            }
        }

        setupChips(binding.myChipGroup, listOf("즐겨찾기", "최근 한 운동"))
        setupChips(binding.partChipGroup, listOf("가슴", "등", "하체", "어깨", "복근", "유산소"))
        setupChips(binding.equipmentChipGroup, listOf("맨몸", "덤벨", "케틀벨", "세라밴드", "스텝박스", "짐볼"))

        binding.changeCompleteButton.setOnClickListener {
            if (selectedExercise == null) {
                Toast.makeText(requireContext(), "운동을 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val newId = selectedExercise!!.id
                try {
                    // 1️⃣ 서버 변경 요청
                    val response = RetrofitClient.scheduleApi.changeExercise(
                        RetrofitClient.ChangeExerciseRequest(
                            planId = planId,
                            oldExerciseId = exerciseId,
                            newExerciseId = newId
                        )
                    )

                    if (!response.isSuccessful) throw Exception("서버 변경 실패")

                    // 2️⃣ Room 동기화
                    db.planDetailDao().replaceExercise(planId, exerciseId, newId)
                    db.exerciseSetDao().updateExerciseId(exerciseId, newId)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "운동이 변경되었습니다.", Toast.LENGTH_SHORT).show()
                        setFragmentResult("exercise_changed", bundleOf("newId" to newId))
                        dismiss()
                    }
                } catch (e: Exception) {
                    Log.e("ExerciseChangeFragment", "운동 변경 오류: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "운동 변경 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


//        binding.changeCompleteButton.setOnClickListener {
//            if (selectedExercise == null) {
//
//                Toast.makeText(requireContext(), "운동을 선택해주세요.", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            lifecycleScope.launch(Dispatchers.IO) {
//                try {
//                    val newId = selectedExercise!!.id
//
//                    // PlanDetail 업데이트
//                    val updatedRows = planDetailDao.replaceExercise(planId, exerciseId, newId)
//                    if (updatedRows > 0) {
//                        // ExerciseSet 테이블도 업데이트
//                        db.exerciseSetDao().updateExerciseId(
//                            oldExerciseId = exerciseId,
//                            newExerciseId = newId
//                        )
//
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(requireContext(), "운동이 변경되었습니다.", Toast.LENGTH_SHORT).show()
//                            //setFragmentResult("exercise_changed", bundleOf("newId" to newId))
//                            onExerciseChanged?.invoke(newId)
//                            dismiss() // 프래그먼트 종료
//                        }
//                    } else {
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(requireContext(), "운동 변경 실패: PlanDetail 업데이트 안됨", Toast.LENGTH_SHORT).show()
//                            //setFragmentResult("exercise_changed", bundleOf("newId" to newId))
//                            onExerciseChanged?.invoke(newId)
//                            dismiss() // 프래그먼트 종료
//                        }
//                    }
//
//                } catch (e: Exception) {
//                    Log.e("ExerciseChangeFragment", "Error changing exercise: ${e.message}")
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(
//                            requireContext(),
//                            "운동 변경 중 오류 발생: ${e.message}",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                        dismiss()
//                    }
//                }
//            }
//        }

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }


    private fun setupChips(group: ChipGroup, texts: List<String>) {
        val spacing = resources.getDimensionPixelSize(R.dimen.chip_spacing)
        group.chipSpacingHorizontal = spacing
        val defaultCornerRadius = resources.getDimension(R.dimen.default_chip_corner_radius)
        val selectedCornerRadius = resources.getDimension(R.dimen.selected_chip_corner_radius)
        val selectedPaddingHorizontal =
            resources.getDimensionPixelSize(R.dimen.selected_chip_padding_horizontal)
        val selectedPaddingVertical =
            resources.getDimensionPixelSize(R.dimen.selected_chip_padding_vertical)
        val selectedCloseIconStartPadding =
            resources.getDimensionPixelSize(R.dimen.selected_close_icon_start_padding).toFloat()

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
                        chip.setPadding(
                            selectedPaddingHorizontal,
                            0,
                            selectedPaddingHorizontal,
                            0
                        )
                        chip.closeIconStartPadding = selectedCloseIconStartPadding
                        chip.closeIconEndPadding = 0f
                        chip.textEndPadding =
                            if (chip.isCloseIconVisible) selectedCloseIconStartPadding else 0f
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
        lifecycleScope.launch(Dispatchers.IO) {
            db.exerciseDao().getAllExercises().collectLatest { all ->
                val favFilter = binding.myChipGroup.checkedChipIds.any {
                    binding.myChipGroup.findViewById<Chip>(it).text == "즐겨찾기"
                }
                val parts = binding.partChipGroup.checkedChipIds.map {
                    binding.partChipGroup.findViewById<Chip>(it).text.toString()
                }
                val equips = binding.equipmentChipGroup.checkedChipIds.map {
                    binding.equipmentChipGroup.findViewById<Chip>(it).text.toString()
                }

                val filtered = all
                    .filter { e -> e.id != exerciseId }
                    .filter { e ->
                        (!favFilter || e.isFavorite) &&
                                (parts.isEmpty() || parts.any { e.part.contains(it) }) &&
                                (equips.isEmpty() || equips.any { e.equip.contains(it) })
                    }

                withContext(Dispatchers.Main) {
                    adapter.submitList(filtered)
                }
            }
        }
    }


    class ChangeExerciseAdapter(
        initialList: List<Exercise>,
        private val onExerciseSelected: (Exercise, Boolean) -> Unit,
        private val onFavoriteClicked: (Exercise) -> Unit
    ) : ListAdapter<Exercise, ChangeExerciseAdapter.ExerciseViewHolder>(ExerciseDiffCallback()) {

        private lateinit var context: Context
        private var selectedItemPosition: Int? = null
        private var selectedExerciseId: Long? = null

        fun setContext(context: Context) {
            this.context = context
        }

        fun setMaxSelectableCount(count: Int) {
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
            context = parent.context
            val binding =
                ItemAddExerciseBinding.inflate(LayoutInflater.from(context), parent, false)
            return ExerciseViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        fun notifySelectionChanged(exerciseId: Long, isSelected: Boolean) {
            val exercise = currentList.find { it.id == exerciseId }
            exercise?.let {
                val position = currentList.indexOf(exercise)
                if (position != -1) {
                    if (isSelected) {
                        selectedItemPosition = position
                        selectedExerciseId = exerciseId
                    } else {
                        selectedItemPosition = null
                        selectedExerciseId = null
                    }
                    notifyItemChanged(position)
                }
            }
        }

        override fun onCurrentListChanged(
            previousList: MutableList<Exercise>,
            currentList: MutableList<Exercise>
        ) {
            super.onCurrentListChanged(previousList, currentList)
            if (selectedExerciseId != null) {
                notifySelectionChanged(selectedExerciseId!!, true)
            }
        }

        inner class ExerciseViewHolder(private val binding: ItemAddExerciseBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {
                binding.itemContentLayout.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val exercise = getItem(position)
                        if (selectedExerciseId != exercise.id) {
                            val previousSelectedPosition = selectedItemPosition
                            selectedItemPosition = position
                            selectedExerciseId = exercise.id
                            onExerciseSelected(exercise, true)
                            if (previousSelectedPosition != null && previousSelectedPosition != position) {
                                notifyItemChanged(previousSelectedPosition)
                            }
                            notifyItemChanged(position)
                        } else {
                            selectedItemPosition = null
                            selectedExerciseId = null
                            onExerciseSelected(exercise, false)
                            notifyItemChanged(position)
                        }
                    }
                }

                binding.favoriteButtonContainer.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        getItem(position)?.let { onFavoriteClicked(it) }
                    }
                }
            }

            fun bind(exercise: Exercise) {
                binding.exerciseNameTextView.text = exercise.name
                binding.favoriteButton.isSelected = exercise.isFavorite
                updateBackgroundColor(selectedExerciseId == exercise.id)

                val resId = context.resources.getIdentifier(
                    exercise.imagePath ?: "",
                    "drawable",
                    context.packageName
                ).takeIf { it != 0 } ?: R.drawable.ic_launcher_background

                // Glide 로 이미지 로드
                Glide.with(binding.exerciseImageView)
                    .asBitmap() // GIF를 비트맵으로 로드하여 정지 상태로
                    .load(resId)
                    .placeholder(R.drawable.ic_launcher_background)   // 로딩 중 보여줄 이미지
                    .error(R.drawable.ic_launcher_background)  // 에러 시 보여줄 이미지
                    .into(binding.exerciseImageView)
            }

            private fun updateBackgroundColor(isSelected: Boolean) {
                val color = if (isSelected) "#d6d6d6" else "#00000000"
                binding.itemRootLayout.setBackgroundColor(Color.parseColor(color))
            }
        }
    }

    private class ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean =
            oldItem == newItem
    }
}
