package com.cookandroid.challengers

import android.R.attr.name
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable // TextWatcher 사용을 위해 추가
import android.text.TextWatcher // TextWatcher 사용을 위해 추가
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.MotionEvent
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
import com.cookandroid.challengers.data.PlanDetail
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentExerciseAddBinding
import com.cookandroid.challengers.databinding.ItemAddExerciseBinding
import com.cookandroid.challengers.network.dto.ExerciseDto
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private lateinit var dbForEnrich: AppDatabase // 운동 이름/이미지 보완용

    private var planId: Long = -1L
    private val maxSlots = 20
    private var countOfExistingExercisesInPlan: Int = 0
    private var idsOfExistingExercisesInPlan = listOf<Long>()

    private val selectedExercises = mutableListOf<Exercise>()
    private var isMaxSelectionReached = false

    private var allExercisesFromServer = listOf<Exercise>()
    private var searchQuery: String = "" // ★ 검색어 저장을 위한 변수 추가

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            planId = it.getLong(ARG_PLAN_ID, -1L)
        }
        Log.d("ExerciseAddFragment", "onCreate - planId: $planId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExerciseAddBinding.inflate(inflater, container, false)
        dbForEnrich = AppDatabase.getDatabase(requireContext(), lifecycleScope)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        setupRecyclerView()
        loadInitialDataFromServer()
        setupChipFilters()
        setupSearchListener() // ★ 검색 리스너 설정

        binding.addCompleteButton.setOnClickListener {
            addSelectedExercisesToServer()
        }

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupSearchListener() { // ★ 검색 리스너 함수
        // FragmentExerciseAddBinding에 searchEditText가 있다고 가정
        binding.searchText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().trim()
                applyFilters(idsOfExistingExercisesInPlan) // 텍스트 변경 시 필터 다시 적용
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // ... (나머지 함수 생략)

    private fun setupAdapter() {
        adapter = AddExerciseAdapter(
            onExerciseItemClicked = { exercise ->
                val currentMaxSelectable = maxSlots - countOfExistingExercisesInPlan
                val isCurrentlySelected = selectedExercises.any { it.id == exercise.id }

                if (!isCurrentlySelected) {
                    if (selectedExercises.size < currentMaxSelectable || currentMaxSelectable < 0) {
                        selectedExercises.add(exercise)
                    } else {
                        Toast.makeText(requireContext(), "최대 ${if (currentMaxSelectable < 0) 0 else currentMaxSelectable}개까지 선택 가능합니다.", Toast.LENGTH_SHORT).show()
                        adapter.updateSelectionState(selectedExercises.map { it.id })
                        return@AddExerciseAdapter
                    }
                } else {
                    selectedExercises.removeAll { it.id == exercise.id }
                }
                adapter.updateSelectionState(selectedExercises.map { it.id })
                updateSelectedText()
                updateSelectedChips()
                checkMaxSelectionAndUpdateAdapterState()
            },
            onFavoriteClicked = { exerciseToToggle ->
                val newFavoriteState = !(exerciseToToggle.isFavorite ?: false)
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val response = RetrofitClient.exerciseApi.toggleExerciseFavorite(
                            exerciseToToggle.id,
                            RetrofitClient.ToggleFavoriteRequest(newFavoriteState)
                        )
                        if (response.isSuccessful && response.body() != null) {
                            val serverResponse = response.body()!!
                            withContext(Dispatchers.Main) {
                                if(_binding == null) return@withContext
                                Toast.makeText(
                                    requireContext(),
                                    if (serverResponse.isFavorite) "'${exerciseToToggle.name}' 즐겨찾기 추가" else "'${exerciseToToggle.name}' 즐겨찾기 해제",
                                    Toast.LENGTH_SHORT
                                ).show()
                                updateExerciseStateInAllExercisesList(
                                    exerciseToToggle.id,
                                    newIsFavorite = serverResponse.isFavorite,
                                    newIsHidden = serverResponse.isHidden
                                )
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                if(_binding == null) return@withContext
                                Log.e("ExerciseAddFragment", "즐겨찾기 변경 실패: ${response.code()} ${response.message()}")
                                Toast.makeText(requireContext(), "즐겨찾기 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            if(_binding == null) return@withContext
                            Log.e("ExerciseAddFragment", "즐겨찾기 업데이트 오류", e)
                            Toast.makeText(requireContext(), "즐겨찾기 변경 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    private fun setupRecyclerView() {
        binding.exerciseListRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ExerciseAddFragment.adapter
            addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    val childView = rv.findChildViewUnder(e.x, e.y)
                    val position = if (childView != null) rv.getChildAdapterPosition(childView) else RecyclerView.NO_POSITION
                    if (position != RecyclerView.NO_POSITION && position < this@ExerciseAddFragment.adapter.currentList.size) {
                        val exercise = this@ExerciseAddFragment.adapter.currentList.getOrNull(position)
                        if (exercise != null && !selectedExercises.any{ it.id == exercise.id} && isMaxSelectionReached) {
                            return true
                        }
                    }
                    return false
                }
                override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            })
        }
    }

    private fun loadInitialDataFromServer() {
        lifecycleScope.launch(Dispatchers.IO) {
            var initialRemainingSlots = maxSlots
            var tempExistingExerciseIds = emptyList<Long>()

            try {
                Log.d("ExerciseAddFragment", "loadInitialDataFromServer - 시작, planId: $planId")
                if (planId == -1L) {
                    Log.w("ExerciseAddFragment", "유효하지 않은 planId: $planId. 기존 운동 없이 진행.")
                    countOfExistingExercisesInPlan = 0
                    idsOfExistingExercisesInPlan = emptyList()
                } else {
                    // ★ 1. 서버에서 현재 planId에 이미 추가된 운동 목록(SimpleScheduleItemDto 리스트) 가져오기
                    val existingSchedulesResponse = RetrofitClient.scheduleApi.getSchedulesForPlan(planId) // API 호출
                    if (existingSchedulesResponse.isSuccessful) {
                        val existingItems = existingSchedulesResponse.body() ?: emptyList()
                        countOfExistingExercisesInPlan = existingItems.size
                        idsOfExistingExercisesInPlan = existingItems.map { it.exerciseId } // SimpleScheduleItemDto에 exerciseId가 있다고 가정
                        Log.d("ExerciseAddFragment", "서버에서 가져온 existingExercises 개수: $countOfExistingExercisesInPlan for planId: $planId")
                    } else {
                        Log.e("ExerciseAddFragment", "기존 스케줄 로드 실패: ${existingSchedulesResponse.code()} - ${existingSchedulesResponse.message()}")
                        countOfExistingExercisesInPlan = 0 // 오류 시 0으로 처리
                        idsOfExistingExercisesInPlan = emptyList()
                        withContext(Dispatchers.Main) {
                            if(_binding == null) return@withContext
                            Toast.makeText(requireContext(), "기존 운동 목록을 가져오지 못했습니다. (${existingSchedulesResponse.code()})", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                tempExistingExerciseIds = idsOfExistingExercisesInPlan
                initialRemainingSlots = maxSlots - countOfExistingExercisesInPlan

                withContext(Dispatchers.Main) {
                    if(_binding == null) return@withContext
                    updateSelectedText()
                    adapter.setMaxSelectableCountAndCurrentSelection(
                        if(initialRemainingSlots < 0) 0 else initialRemainingSlots,
                        selectedExercises.map { it.id }.toSet()
                    )
                }

                // 2. 서버에서 모든 운동 목록 가져오기
                val allExercisesResponse = RetrofitClient.exerciseApi.getAllExercises()
                if (allExercisesResponse.isSuccessful) {
                    val serverDtoList = allExercisesResponse.body() ?: emptyList()
                    Log.d("ExerciseAddFragment", "서버에서 전체 운동 DTO ${serverDtoList.size}개 로드")
                    // val localDbForEnrich = AppDatabase.getDatabase(requireContext(), lifecycleScope) // 이미 멤버 변수로 dbForEnrich 있음
                    allExercisesFromServer = serverDtoList.map { dto ->
                        val localExercise = dbForEnrich.exerciseDao().getExerciseById(dto.id)
                        Exercise(
                            id = dto.id,

                            name = dto.name ?: "이름 없음",
                            part = dto.part ?: "부위 정보 없음",
                            equip = dto.equip ?: "장비 정보 없음",
                            imagePath = dto.image_path ?: "",

                            startPosition = dto.start_position,
                            exerciseMotion = dto.exercise_motion,
                            breathing = dto.breathing,
                            caution = dto.caution,
                            mets = dto.mets ?: 0.0,

                            isFavorite = dto.isFavorite ?: false,
                            isTimeType = dto.isTimeType ?: false,
                            isHidden = dto.isHidden ?: false,
                            isNoise = dto.is_noise ?: false
                        )
                    }
                    Log.d("ExerciseAddFragment", "서버 DTO 매핑 후 allExercisesFromServer 개수: ${allExercisesFromServer.size}")
                    withContext(Dispatchers.Main) {
                        if(_binding == null) return@withContext
                        applyFilters(tempExistingExerciseIds)
                        updateSelectedChips()
                        checkMaxSelectionAndUpdateAdapterState()
                    }
                } else {
                    Log.e("ExerciseAddFragment", "❌ 모든 운동 목록 가져오기 실패: ${allExercisesResponse.code()}")
                    withContext(Dispatchers.Main) {
                        if(_binding == null) return@withContext
                        Toast.makeText(requireContext(), "운동 목록을 불러오지 못했습니다. (${allExercisesResponse.code()})", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ExerciseAddFragment", "❗ 데이터 로드 중 예외 발생: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if(_binding == null) return@withContext
                    Toast.makeText(requireContext(), "데이터 로드 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupChipFilters() {
        setupChips(binding.myChipGroup, listOf("즐겨찾기", "최근 한 운동"))
        setupChips(binding.partChipGroup, listOf("가슴", "등", "하체", "어깨", "팔", "복근", "유산소"))
        setupChips(binding.equipmentChipGroup, listOf("맨몸", "덤벨", "케틀벨", "세라밴드", "스텝박스", "짐볼"))
    }

    private fun addSelectedExercisesToServer() {
        val currentMaxSelectable = maxSlots - countOfExistingExercisesInPlan
        if (selectedExercises.isEmpty()){
            Toast.makeText(requireContext(), "운동을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedExercises.size > currentMaxSelectable && currentMaxSelectable >= 0) {
            Toast.makeText(requireContext(), "최대 ${currentMaxSelectable}개까지만 선택 가능합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            var successCount = 0
            val totalSelected = selectedExercises.size
            selectedExercises.forEachIndexed { idx, ex ->
                val exerciseOrder = countOfExistingExercisesInPlan + successCount + 1
                val dateString = LocalDate.now().toString()

                val request = RetrofitClient.CreateScheduleRequest(
                    planId = planId.toInt(),
                    date = dateString,
                    exerciseOrder = exerciseOrder,
                    exerciseId = ex.id.toInt()
                )
                try {
                    val response = RetrofitClient.scheduleApi.createSchedule(request)
                    if (response.isSuccessful && response.body() != null) {
                        successCount++
                    } else {
                        Log.e("ExerciseAddFragment", "❌ 서버 schedule 생성 실패: ${ex.name} - ${response.code()} ${response.message()}")
                        withContext(Dispatchers.Main) {
                            if(_binding == null) return@withContext
                            Toast.makeText(requireContext(), "'${ex.name}' 추가 실패: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ExerciseAddFragment", "'${ex.name}' 추가 중 오류", e)
                    withContext(Dispatchers.Main) {
                        if(_binding == null) return@withContext
                        Toast.makeText(requireContext(), "'${ex.name}' 추가 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            withContext(Dispatchers.Main) {
                if(_binding == null) return@withContext
                if (successCount > 0) {
                    Toast.makeText(requireContext(), "${successCount}개의 운동이 추가되었습니다.", Toast.LENGTH_SHORT).show()
                }
                if (successCount == totalSelected || (totalSelected == 0 && successCount == 0) ) {
                    parentFragmentManager.setFragmentResult("plan_updated_from_add", Bundle())
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun setupChips(group: ChipGroup, texts: List<String>) {
        val context = requireContext()
        val spacing = resources.getDimensionPixelSize(R.dimen.chip_spacing)
        group.chipSpacingHorizontal = spacing
        val defaultCornerRadius = resources.getDimension(R.dimen.default_chip_corner_radius)
        val selectedCornerRadius = resources.getDimension(R.dimen.selected_chip_corner_radius)
        val selectedPaddingHorizontal = resources.getDimensionPixelSize(R.dimen.selected_chip_padding_horizontal)
        val selectedCloseIconStartPadding = resources.getDimensionPixelSize(R.dimen.selected_close_icon_start_padding).toFloat()

        group.removeAllViews()
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
                setTextAppearance(R.style.TextChip)
                setChipBackgroundColorResource(android.R.color.transparent)
            }.also { chip ->
                chip.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        chip.setTextAppearance(R.style.SelectedChip)
                        chip.setChipBackgroundColorResource(R.color.blue)
                        chip.shapeAppearanceModel = ShapeAppearanceModel.builder()
                            .setAllCorners(CornerFamily.ROUNDED, selectedCornerRadius)
                            .build()
                        chip.setCloseIconResource(R.drawable.ic_close)
                        chip.closeIconTint = ColorStateList.valueOf(Color.WHITE)
                        chip.isCloseIconVisible = true
                        chip.setPadding(selectedPaddingHorizontal, chip.paddingTop, selectedPaddingHorizontal, chip.paddingBottom)
                        chip.closeIconStartPadding = selectedCloseIconStartPadding
                    } else {
                        chip.setTextAppearance(R.style.TextChip)
                        chip.setChipBackgroundColorResource(android.R.color.transparent)
                        chip.shapeAppearanceModel = ShapeAppearanceModel.builder()
                            .setAllCorners(CornerFamily.ROUNDED, defaultCornerRadius)
                            .build()
                        chip.isCloseIconVisible = false
                        chip.setPadding(0, chip.paddingTop, 0, chip.paddingBottom)
                    }
                    applyFilters(idsOfExistingExercisesInPlan)
                }
                chip.setOnCloseIconClickListener {
                    chip.isChecked = false
                }
                group.addView(chip)
            }
        }
    }

    private fun applyFilters(currentExistingExerciseIds: List<Long>) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            if (_binding == null) return@launch

            val currentSearchQuery = searchQuery.lowercase() // ★ 검색어 소문자 변환

            val favFilter = binding.myChipGroup.checkedChipIds.any {
                binding.myChipGroup.findViewById<Chip>(it)?.text == "즐겨찾기"
            }
            val parts = binding.partChipGroup.checkedChipIds.mapNotNull {
                binding.partChipGroup.findViewById<Chip>(it)?.text?.toString()
            }
            val equips = binding.equipmentChipGroup.checkedChipIds.mapNotNull {
                binding.equipmentChipGroup.findViewById<Chip>(it)?.text?.toString()
            }

            val filtered = allExercisesFromServer
                .filter { exercise -> !(exercise.isHidden ?: false) }
                .filter { exercise -> currentExistingExerciseIds.none { it == exercise.id } }
                .filter { e ->
                    // 1. 검색어 필터링
                    (currentSearchQuery.isBlank() || e.name.lowercase().contains(currentSearchQuery)) && // ★ 검색 필터 적용
                            // 2. 기타 필터
                            (!favFilter || (e.isFavorite ?: false)) &&
                            (parts.isEmpty() || parts.any { e.part.contains(it, ignoreCase = true) }) &&
                            (equips.isEmpty() || equips.any { e.equip.contains(it, ignoreCase = true) })
                }
            adapter.updateSelectionState(selectedExercises.map { it.id })
            adapter.submitList(filtered)
            updateCount(filtered.size)
        }
    }

    // ... (나머지 함수 생략)
    private fun updateSelectedText() {
        val currentMaxSelectable = maxSlots - countOfExistingExercisesInPlan // ★ countOfExistingExercisesInPlan 사용
        Log.d("ExerciseAddFragment", "updateSelectedText - selected: ${selectedExercises.size}, maxSelectable (m): $currentMaxSelectable (maxSlots: $maxSlots, existing: $countOfExistingExercisesInPlan)")
        binding.selectedExercisesTextView.text =
            String.format("선택한 운동: %02d / %02d", selectedExercises.size, if (currentMaxSelectable < 0) 0 else currentMaxSelectable)
    }

    private fun updateCount(count: Int) {
        binding.totalExercisesTextView.text = "전체 ${count}개"
    }

    private fun updateSelectedChips() {
        binding.selectedChipGroup.removeAllViews()
        selectedExercises.forEach { exercise ->
            val chip = Chip(requireContext(), null, R.style.SelectedExerciseChip).apply {
                text = exercise.name
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    val isCurrentlySelected = selectedExercises.any { it.id == exercise.id }
                    if (isCurrentlySelected) {
                        selectedExercises.removeAll { it.id == exercise.id }
                        adapter.updateSelectionState(selectedExercises.map { it.id })
                        updateSelectedText()
                        updateSelectedChips()
                        checkMaxSelectionAndUpdateAdapterState()
                    }
                }
            }
            binding.selectedChipGroup.addView(chip)
        }
    }

    private fun checkMaxSelectionAndUpdateAdapterState() { // ★ countOfExistingExercisesInPlan 사용
        val currentMaxSelectable = maxSlots - countOfExistingExercisesInPlan
        isMaxSelectionReached = selectedExercises.size >= currentMaxSelectable && currentMaxSelectable >= 0
        Log.d("ExerciseAddFragment", "checkMaxSelection - isMaxReached: $isMaxSelectionReached, selected: ${selectedExercises.size}, maxSelectable: $currentMaxSelectable")

        adapter.setMaxSelectableCountAndCurrentSelection(
            if (currentMaxSelectable < 0) 0 else currentMaxSelectable,
            selectedExercises.map { it.id }.toSet()
        )
    }

    private fun updateExerciseStateInAllExercisesList(exerciseId: Long, newIsFavorite: Boolean?, newIsHidden: Boolean?) { // ★ idsOfExistingExercisesInPlan 사용
        val globalIndex = allExercisesFromServer.indexOfFirst { it.id == exerciseId }
        if (globalIndex != -1) {
            val currentItem = allExercisesFromServer[globalIndex]
            val updatedItem = currentItem.copy(
                isFavorite = newIsFavorite ?: currentItem.isFavorite,
                isHidden = newIsHidden ?: currentItem.isHidden
            )
            val tempList = allExercisesFromServer.toMutableList()
            if (globalIndex < tempList.size) {
                tempList[globalIndex] = updatedItem
                allExercisesFromServer = tempList.toList()
            }
        }
        applyFilters(idsOfExistingExercisesInPlan) // ★ idsOfExistingExercisesInPlan 사용
    }


    private class AddExerciseAdapter(
        private val onExerciseItemClicked: (Exercise) -> Unit,
        private val onFavoriteClicked: (Exercise) -> Unit
    ) : ListAdapter<Exercise, AddExerciseAdapter.ExerciseViewHolder>(ExerciseDiffCallback()) {

        private var currentSelectedIds = setOf<Long>()
        private var currentMaxSelectableCount: Int = Int.MAX_VALUE

        fun setMaxSelectableCountAndCurrentSelection(maxCount: Int, selectedIds: Set<Long>) {
            currentMaxSelectableCount = maxCount
            currentSelectedIds = selectedIds
            notifyDataSetChanged()
        }

        fun updateSelectionState(selectedIds: List<Long>) {
            val newSelectedIds = selectedIds.toSet()
            if (currentSelectedIds != newSelectedIds) {
                currentSelectedIds = newSelectedIds
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
            val binding = ItemAddExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ExerciseViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class ExerciseViewHolder(private val binding: ItemAddExerciseBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {
                binding.itemContentLayout.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val exercise = getItem(position)
                        if (exercise.isHidden == true) {
                            Toast.makeText(itemView.context, "숨김 처리된 운동은 선택할 수 없습니다.", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        onExerciseItemClicked(exercise)
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
                binding.favoriteButton.isSelected = exercise.isFavorite ?: false

                val imagePath = exercise.imagePath
                val context = itemView.context
                val resId = if (!imagePath.isNullOrBlank()) {
                    context.resources.getIdentifier(
                        imagePath, "drawable", context.packageName
                    ).takeIf { it != 0 }
                } else { null }

                Glide.with(context)
                    .asBitmap()
                    .load(resId ?: R.drawable.ic_fitbuddy_logo)
                    .placeholder(R.drawable.ic_fitbuddy_logo)
                    .error(R.drawable.ic_fitbuddy_logo)
                    .into(binding.exerciseImageView)

                updateViewSelectionState(currentSelectedIds.contains(exercise.id))

                val isCurrentlySelected = currentSelectedIds.contains(exercise.id)
                val canBeNewlySelected = currentSelectedIds.size < currentMaxSelectableCount
                val isActuallyHidden = exercise.isHidden ?: false

                if (isActuallyHidden) {
                    binding.itemRootLayout.alpha = 0.4f
                    binding.exerciseNameTextView.text = "${exercise.name} (숨김)"
                    itemView.isEnabled = false
                } else {
                    binding.itemRootLayout.alpha = 1.0f
                    binding.exerciseNameTextView.text = exercise.name
                    val isClickable = isCurrentlySelected || canBeNewlySelected
                    itemView.isEnabled = isClickable
                    itemView.alpha = if (isClickable) 1.0f else 0.5f
                }
            }

            private fun updateViewSelectionState(isSelected: Boolean) {
                val colorString = if (isSelected) "#F0F0F0" else "#FFFFFF"
                try {
                    binding.itemRootLayout.setBackgroundColor(Color.parseColor(colorString))
                } catch (e: IllegalArgumentException) {
                    Log.e("AddExerciseAdapter", "Color parsing error: $colorString", e)
                    binding.itemRootLayout.setBackgroundColor(Color.TRANSPARENT)
                }
            }
        }
    }

    private class ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean = oldItem == newItem
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.exerciseListRecyclerView.adapter = null
        _binding = null
    }
}