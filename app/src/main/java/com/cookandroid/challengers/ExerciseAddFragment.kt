package com.cookandroid.challengers

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.data.Exercise
import com.cookandroid.challengers.data.ExerciseSet
import com.cookandroid.challengers.data.PlanDetail
import com.cookandroid.challengers.data.PlanDetailDao
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentExerciseAddBinding
import com.cookandroid.challengers.databinding.ItemAddExerciseBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.any
import kotlin.collections.filter
import kotlin.collections.find
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.collections.map
import kotlin.collections.none
import java.time.LocalDate

class ExerciseAddFragment : Fragment() {

    companion object {
        private const val ARG_PLAN_ID = "planId"
        fun newInstance(planId: Long) = ExerciseAddFragment().apply {
            arguments = Bundle().apply { putLong(ARG_PLAN_ID, planId) }
        }
    }

    private var _binding: FragmentExerciseAddBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AddExerciseAdapter
    private lateinit var planDetailDao: PlanDetailDao
    private lateinit var db: AppDatabase

    private var planId: Long = -1L
    private val maxSlots = 20
    private var existingExercises = listOf<PlanDetail>()
    private val selectedExercises = mutableListOf<Exercise>()
    private var isMaxSelectionReached = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        planId = arguments?.getLong(ARG_PLAN_ID) ?: -1L
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExerciseAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
        planDetailDao = db.planDetailDao()

        adapter = AddExerciseAdapter(
            emptyList(),
            onExerciseSelected = { ex, isSelected ->
                if (isSelected) selectedExercises.add(ex) else selectedExercises.remove(ex)
                updateSelectedText()
                updateSelectedChips()
                checkMaxSelection() // 선택 상태 변경 시 최대 선택 여부 확인
            },
            onFavoriteClicked = { ex ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.exerciseDao().update(ex.copy(isFavorite = !ex.isFavorite))
                }
            },
            onMaxSelectionReached = { reached ->
                val maxSelect = maxSlots - existingExercises.size
                isMaxSelectionReached = reached
                if (reached) {
                    Toast.makeText(requireContext(), "최대 ${maxSelect}개까지 선택 가능합니다.", Toast.LENGTH_SHORT).show()
                }
                // 필요에 따라 UI 업데이트 (예: 더 이상 선택 못하도록 시각적으로 변경)
            }
        )
        adapter.setContext(requireContext())

        binding.exerciseListRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ExerciseAddFragment.adapter
            addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: android.view.MotionEvent): Boolean {
                    return isMaxSelectionReached // 최대 선택 도달 시 true를 반환하여 터치 이벤트 가로챔
                }

                override fun onTouchEvent(rv: RecyclerView, e: android.view.MotionEvent) {}
                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            })
        }

        lifecycleScope.launch(Dispatchers.IO) {
            existingExercises = planDetailDao.getPlanDetailsForPlanId(planId)
            val initialRemainingSlots = maxSlots - existingExercises.size
            withContext(Dispatchers.Main) {
                adapter.setMaxSelectableCount(initialRemainingSlots)
                db.exerciseDao().getAllExercises().collectLatest { all ->
                    val filtered = all.filter { e -> existingExercises.none { it.exerciseId == e.id.toLong() } }
                    adapter.submitList(filtered)
                    updateCount(filtered.size)
                    updateSelectedText()
                    updateSelectedChips()
                }
            }
        }

        setupChips(binding.myChipGroup, listOf("즐겨찾기", "최근 한 운동"))
        setupChips(binding.partChipGroup, listOf("가슴", "등", "하체", "어깨", "복근", "유산소"))
        setupChips(binding.equipmentChipGroup, listOf("맨몸", "덤벨", "케틀벨", "세라밴드", "스텝박스","짐볼"))

        binding.addCompleteButton.setOnClickListener {
            val maxSelect = maxSlots - existingExercises.size
            if (selectedExercises.size > maxSelect) {
                Toast.makeText(requireContext(), "최대 ${maxSelect}개까지 선택 가능합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val defaultSets = 3
                val defaultReps = 15

                selectedExercises.forEachIndexed { idx, ex ->
                    val isTimeType = ex.isTimeType
                    val exerciseOrder = existingExercises.size + idx + 1
                    val dateString = LocalDate.now().toString() // 예: "2025-05-23"

                    // ✅ 1. 먼저 서버에 schedule 생성 요청
                    Log.d("AddExercise", "📤 schedule 요청: planId=$planId, date=$dateString, order=$exerciseOrder")
                    val scheduleResponse = try {
                        RetrofitClient.scheduleApi.createSchedule(
                            RetrofitClient.CreateScheduleRequest(
                                planId = planId.toInt(),
                                date = dateString,
                                exerciseOrder = exerciseOrder
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("AddExercise", "❗ schedule 생성 실패: ${e.localizedMessage}")
                        null
                    }
                    // ✅ 2. schedule 응답 상세 로그 출력
                    if (scheduleResponse != null) {
                        val errorBody = scheduleResponse.errorBody()?.string()
                        Log.d("AddExercise", "📥 응답 code=${scheduleResponse.code()}, error=$errorBody")
                    } else {
                        Log.e("AddExercise", "❌ schedule 응답이 null임")
                    }
                    Log.e("AddExercise", "❌ 서버 응답 코드: ${scheduleResponse?.code()}")

                    val scheduleId = scheduleResponse?.body()?.scheduleId

                    if (scheduleResponse?.isSuccessful == true && scheduleId != null) {
                        // ✅ 2. scheduleId 기반으로 운동 추가
                        val setList = (1..defaultSets).map { i ->
                            if (isTimeType) {
                                RetrofitClient.SetData(setNumber = i, seconds = 30)
                            } else {
                                RetrofitClient.SetData(setNumber = i, reps = defaultReps, weight = 0)
                            }
                        }

                        val request = RetrofitClient.AddExerciseRequest(
                            exerciseId = ex.id.toInt(),
                            setList = setList
                        )

                        val response = try {
                            RetrofitClient.scheduleApi.addExerciseToSchedule(scheduleId = scheduleId.toLong(), request = request)
                        } catch (e: Exception) {
                            Log.e("AddExercise", "❗ 운동 추가 실패: ${e.localizedMessage}")
                            null
                        }

                        if (response?.isSuccessful == true) {
                            // ✅ 3. Room에 저장
                            val planDetail = PlanDetail(
                                exercisePlanId = planId,
                                exerciseId = ex.id,
                                exOrder = exerciseOrder
                            )
                            planDetailDao.insert(planDetail)

                            for (i in 1..defaultSets) {
                                val exerciseSet = ExerciseSet(
                                    exercisePlanId = planId,
                                    exerciseId = ex.id,
                                    setNumber = i,
                                    weight = 0,
                                    reps = defaultReps,
                                    isCompleted = false,
                                    isHighlighted = (i == 1 && idx == 0 && existingExercises.isEmpty())
                                )
                                db.exerciseSetDao().insert(exerciseSet)
                            }

                            Log.d("AddExercise", "✅ ${ex.name} → 서버 + Room 저장 성공")
                        } else {
                            Log.e("AddExercise", "❌ ${ex.name} → 서버 운동 저장 실패")
                        }
                    } else {
                        Log.e("AddExercise", "❌ scheduleId 생성 실패")
                    }
                }

                withContext(Dispatchers.Main) {
                    findNavController().popBackStack()
                }
            }
        }


        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
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
                    .filter { e -> existingExercises.none { it.exerciseId == e.id.toLong() } }
                    .filter { e ->
                        (!favFilter || e.isFavorite) &&
                                (parts.isEmpty() || parts.any { e.part.contains(it) }) &&
                                (equips.isEmpty() || equips.any { e.equip.contains(it) })
                    }

                withContext(Dispatchers.Main) {
                    adapter.submitList(filtered)
                    updateCount(filtered.size)
                }
            }
        }
    }

    private fun updateSelectedText() {
        binding.selectedExercisesTextView.text =
            String.format("선택한 운동: %02d / %02d", selectedExercises.size, maxSlots - existingExercises.size)
    }

    private fun updateCount(count: Int) {
        binding.totalExercisesTextView.text = "전체 ${count}개"
    }

    private fun updateSelectedChips() {
        binding.selectedChipGroup.removeAllViews()
        for (exercise in selectedExercises) {
            val chip = Chip(requireContext(), null, R.style.SelectedExerciseChip).apply {
                text = exercise.name
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    selectedExercises.remove(exercise)
                    updateSelectedText()
                    updateSelectedChips()
                    adapter.notifySelectionChanged(exercise.id.toLong(), false)
                }
            }
            binding.selectedChipGroup.addView(chip)
        }
    }

    private fun checkMaxSelection() {
        val maxSelect = maxSlots - existingExercises.size
        if (selectedExercises.size >= maxSelect) {
            if (!isMaxSelectionReached) {
                Toast.makeText(requireContext(), "최대 ${maxSelect}개까지 선택 가능합니다.", Toast.LENGTH_SHORT).show()
                isMaxSelectionReached = true
            }
        } else {
            isMaxSelectionReached = false
        }
        // 필요에 따라 UI 업데이트 (예: 더 이상 선택 못하도록 시각적으로 변경)
    }

    private class AddExerciseAdapter(
        initialList: List<Exercise>,
        private val onExerciseSelected: (Exercise, Boolean) -> Unit,
        private val onFavoriteClicked: (Exercise) -> Unit,
        private val onMaxSelectionReached: (Boolean) -> Unit // 최대 선택 도달 시 알림 콜백 추가
    ) : ListAdapter<Exercise, AddExerciseAdapter.ExerciseViewHolder>(ExerciseDiffCallback()) {

        private lateinit var context: Context
        private val selectedItemPositions = mutableSetOf<Int>()
        private var maxSelectableCount: Int = 20

        fun setContext(context: Context) {
            this.context = context
        }

        fun setMaxSelectableCount(count: Int) {
            maxSelectableCount = count
        }

        // 계획에 들어간 운동 아이템들
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
            context = parent.context
            val binding = ItemAddExerciseBinding.inflate(LayoutInflater.from(context), parent, false)
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
                    if (isSelected) selectedItemPositions.add(position)
                    else selectedItemPositions.remove(position)
                    notifyItemChanged(position)
                }
            }
            onMaxSelectionReached(selectedItemPositions.size >= maxSelectableCount)
        }

        inner class ExerciseViewHolder(private val binding: ItemAddExerciseBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {
                binding.itemContentLayout.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val exercise = getItem(position)
                        val isSelected = selectedItemPositions.contains(position)
                        if (isSelected) selectedItemPositions.remove(position)
                        else selectedItemPositions.add(position)
                        onExerciseSelected(exercise, !isSelected)
                        updateBackgroundColor(!isSelected) // 선택 상태의 반대로 색상 적용
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

                // 아이템이 바인딩될 때 현재 선택 상태에 따라 배경색 설정 (원하는 색상으로)
                updateBackgroundColor(selectedItemPositions.contains(adapterPosition))
            }

            private fun updateBackgroundColor(isSelected: Boolean) {
                // isSelected가 true (선택됨)일 때 회색, false (선택 안됨)일 때 흰색
                val color = if (isSelected) "#d6d6d6" else "#00000000"
                binding.itemRootLayout.setBackgroundColor(Color.parseColor(color))
            }
        }
    }

    private class ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean = oldItem == newItem
    }
}