package com.cookandroid.challengers

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable // TextWatcher 사용을 위해 추가
import android.text.TextWatcher // TextWatcher 사용을 위해 추가
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.data.Exercise
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExerciseChangeFragment : BottomSheetDialogFragment() {

    companion object {
        private const val TAG = "ExerciseChangeFragment"
        private const val ARG_PLAN_ID = "planId"
        private const val ARG_ORIGINAL_EXERCISE_ID = "originalExerciseId"
        private const val ARG_CURRENT_SCHEDULE_ID = "currentScheduleId"

        fun newInstance(planId: Long, originalExerciseId: Long, currentScheduleId: Long): ExerciseChangeFragment {
            val fragment = ExerciseChangeFragment()
            val args = Bundle().apply {
                putLong(ARG_PLAN_ID, planId)
                putLong(ARG_ORIGINAL_EXERCISE_ID, originalExerciseId)
                putLong(ARG_CURRENT_SCHEDULE_ID, currentScheduleId)
            }
            fragment.arguments = args
            Log.d(TAG, "newInstance called with planId: $planId, originalExerciseId: $originalExerciseId, currentScheduleId: $currentScheduleId")
            return fragment
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    private var _binding: FragmentExerciseChangeBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChangeExerciseAdapter
    private var allExercisesFromServer = listOf<Exercise>() // 서버에서 가져온 전체 운동 목록
    private var searchQuery: String = "" // ★ 검색어 저장을 위한 변수

    private var planId: Long = -1L
    private var originalExerciseId: Long = -1L
    private var currentScheduleId: Long = -1L
    private var selectedExercise: Exercise? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            planId = it.getLong(ARG_PLAN_ID, -1L)
            originalExerciseId = it.getLong(ARG_ORIGINAL_EXERCISE_ID, -1L)
            currentScheduleId = it.getLong(ARG_CURRENT_SCHEDULE_ID, -1L)
        }
        Log.d(TAG, "onCreate: planId=$planId, originalExerciseId=$originalExerciseId, currentScheduleId=$currentScheduleId")
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseChangeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Loaded with planId=$planId, originalExerciseId=$originalExerciseId, currentScheduleId=$currentScheduleId")

        setupAdapter()
        loadAllExercisesFromServer()
        setupChipFilters()
        setupSearchListener() // ★ 검색 리스너 설정

        binding.changeCompleteButton.setOnClickListener {
            handleExerciseChange()
        }

        binding.backButton.setOnClickListener {
            dismiss()
        }
    }

    private fun setupAdapter() {
        adapter = ChangeExerciseAdapter(emptyList()) { ex, isSelected ->
            selectedExercise = if (isSelected) ex else null
            Log.d(TAG, "Exercise selected: ${selectedExercise?.name}, isSelected: $isSelected")
        }
        adapter.setContext(requireContext())

        binding.exerciseListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.exerciseListRecyclerView.adapter = adapter
    }

    private fun setupChipFilters() {
        setupChips(binding.myChipGroup, listOf("즐겨찾기"))
        setupChips(binding.partChipGroup, listOf("가슴", "등", "하체", "어깨", "팔", "복근", "유산소"))
        setupChips(binding.equipmentChipGroup, listOf("맨몸", "덤벨", "케틀벨", "세라밴드", "스텝박스", "짐볼"))
    }

    private fun setupSearchListener() { // ★ 검색 리스너 구현
        // FragmentExerciseChangeBinding에 searchEditText가 있다고 가정하고 구현합니다.
        // 실제 바인딩 객체에 해당 뷰의 ID가 없으면 컴파일 오류가 발생합니다.
        // (ID가 searchEditText라고 가정)
        binding.searchText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().trim()
                applyFilters() // 텍스트 변경 시 필터 다시 적용
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * 서버에서 모든 운동 목록을 가져와 내부 리스트(allExercisesFromServer)에 저장합니다.
     */
    private fun loadAllExercisesFromServer() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val allExercisesResponse = RetrofitClient.exerciseApi.getAllExercises()

                if (allExercisesResponse.isSuccessful) {
                    val serverDtoList = allExercisesResponse.body() ?: emptyList()
                    Log.d(TAG, "서버에서 전체 운동 DTO ${serverDtoList.size}개 로드")

                    // DTO를 Exercise 데이터 클래스로 매핑합니다.
                    allExercisesFromServer = serverDtoList.map { dto ->
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

                    Log.d(TAG, "서버 데이터 매핑 후 allExercisesFromServer 개수: ${allExercisesFromServer.size}")
                    withContext(Dispatchers.Main) {
                        if (_binding == null) return@withContext
                        applyFilters() // 로드 후 필터 적용하여 목록 표시
                    }
                } else {
                    Log.e(TAG, "❌ 모든 운동 목록 가져오기 실패: ${allExercisesResponse.code()}")
                    withContext(Dispatchers.Main) {
                        if (_binding == null) return@withContext
                        Toast.makeText(requireContext(), "운동 목록을 불러오지 못했습니다. (${allExercisesResponse.code()})", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❗ 데이터 로드 중 예외 발생: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext
                    Toast.makeText(requireContext(), "데이터 로드 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    private fun handleExerciseChange() {
        val newSelectedExerciseId = selectedExercise?.id
        if (newSelectedExerciseId == null) {
            Toast.makeText(requireContext(), "운동을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentScheduleId <= 0) {
            Toast.makeText(requireContext(), "운동 변경을 위한 스케줄 정보가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "changeCompleteButton: Invalid currentScheduleId: $currentScheduleId")
            return
        }

        Log.d(TAG, "🚀 변경 요청할 새 운동 ID: $newSelectedExerciseId, 이름: ${selectedExercise?.name}. 기존 scheduleId: $currentScheduleId")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 서버에 운동 변경 요청
                Log.d(TAG, "📤 서버에 운동 변경 요청 - scheduleId: $currentScheduleId, newExerciseId: $newSelectedExerciseId")
                val changeResponse = RetrofitClient.scheduleApi.changeExerciseServer(
                    currentScheduleId,
                    RetrofitClient.ChangeExerciseServerRequest(newSelectedExerciseId)
                )

                if (!changeResponse.isSuccessful || changeResponse.body() == null) {
                    val errorBody = changeResponse.errorBody()?.string() ?: "알 수 없는 오류"
                    throw Exception("❌ 운동 변경 API 실패: ${changeResponse.code()} - $errorBody")
                }
                Log.d(TAG, "✅ 운동 변경 API 성공: ${changeResponse.body()?.message}")


                // 운동 변경 성공 후, 부모 프래그먼트에 결과 전달
                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext
                    Toast.makeText(requireContext(), "'${selectedExercise?.name}'(으)로 운동이 변경되었습니다.", Toast.LENGTH_SHORT).show()
                    setFragmentResult("exercise_changed", bundleOf(
                        "newId" to newSelectedExerciseId,
                        "changedScheduleId" to currentScheduleId
                    ))
                    dismiss()
                }

            } catch (e: Exception) {
                Log.e(TAG, "운동 변경 전체 과정 오류: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext
                    Toast.makeText(requireContext(), "운동 변경 실패: ${e.message}", Toast.LENGTH_SHORT).show()
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
        // val selectedPaddingVertical = resources.getDimensionPixelSize(R.dimen.selected_chip_padding_vertical) // 사용되지 않는 변수 제거
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

    /**
     * 필터링된 운동 목록을 어댑터에 제출합니다.
     * 서버 데이터 리스트(allExercisesFromServer)와 검색어를 사용합니다.
     */
    private fun applyFilters() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            if (_binding == null) return@launch

            val currentSearchQuery = searchQuery.lowercase() // ★ 검색어 소문자 변환

            val isFavoriteFilterEnabled = binding.myChipGroup.checkedChipIds.any {
                binding.myChipGroup.findViewById<Chip>(it).text == "즐겨찾기"
            }
            val selectedParts = binding.partChipGroup.checkedChipIds.mapNotNull {
                binding.partChipGroup.findViewById<Chip>(it)?.text?.toString()
            }
            val selectedEquips = binding.equipmentChipGroup.checkedChipIds.mapNotNull {
                binding.equipmentChipGroup.findViewById<Chip>(it)?.text?.toString()
            }

            Log.d(TAG, "Applying filters - Search: $searchQuery, Favorite: $isFavoriteFilterEnabled, Parts: $selectedParts, Equips: $selectedEquips")

            // 서버에서 가져온 전체 운동 목록을 필터링합니다.
            val filteredList = allExercisesFromServer.filter { exercise ->
                val matchesSearch = currentSearchQuery.isBlank() || exercise.name.lowercase().contains(currentSearchQuery) // ★ 검색 필터 추가

                (exercise.id != originalExerciseId) && // 현재 변경 대상 운동 제외
                        !(exercise.isHidden ?: false) && // 숨김 처리된 운동 제외
                        matchesSearch && // 검색 필터 적용
                        (!isFavoriteFilterEnabled || (exercise.isFavorite ?: false)) && // 즐겨찾기 필터
                        (selectedParts.isEmpty() || selectedParts.any { part -> exercise.part.contains(part, ignoreCase = true) }) && // 부위 필터
                        (selectedEquips.isEmpty() || selectedEquips.any { equip -> exercise.equip.contains(equip, ignoreCase = true) }) // 장비 필터
            }

            Log.d(TAG, "Filtered list size: ${filteredList.size}")
            adapter.submitList(filteredList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView")
    }

    // Adapter 클래스 (ChangeExerciseAdapter는 변경 없이 유지)
    class ChangeExerciseAdapter(
        private var items: List<Exercise>,
        private val onExerciseSelected: (exercise: Exercise, isSelected: Boolean) -> Unit
    ) : RecyclerView.Adapter<ChangeExerciseAdapter.ExerciseViewHolder>() {

        private lateinit var context: Context
        private var selectedItemId: Long? = null

        fun setContext(ctx: Context) {
            context = ctx
        }

        fun submitList(newItems: List<Exercise>) {
            val oldSelectedId = selectedItemId
            items = newItems

            // 새 목록에 이전 선택 항목이 없으면 선택 해제
            if (oldSelectedId != null && !items.any { it.id == oldSelectedId }) {
                selectedItemId = null
            }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
            if (!::context.isInitialized) {
                context = parent.context
            }
            val binding = ItemAddExerciseBinding.inflate(LayoutInflater.from(context), parent, false)
            return ExerciseViewHolder(binding)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
            holder.bind(items[position])
        }

        inner class ExerciseViewHolder(private val binding: ItemAddExerciseBinding) :
            RecyclerView.ViewHolder(binding.root) {

            init {
                binding.itemContentLayout.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val exercise = items[position]
                        val previouslySelectedId = selectedItemId

                        if (selectedItemId == exercise.id) { // 이미 선택된 아이템을 다시 클릭 -> 선택 해제
                            selectedItemId = null
                            onExerciseSelected(exercise, false)
                            notifyItemChanged(position)
                        } else { // 새 아이템 선택
                            selectedItemId = exercise.id
                            onExerciseSelected(exercise, true)
                            notifyItemChanged(position)
                            // 이전에 선택된 아이템이 있었다면 그것도 갱신하여 선택 해제 표시
                            if (previouslySelectedId != null) {
                                val prevPosition = items.indexOfFirst { it.id == previouslySelectedId }
                                if (prevPosition != -1) notifyItemChanged(prevPosition)
                            }
                        }
                    }
                }
                // 운동 변경 팝업이므로, 즐겨찾기 버튼은 숨깁니다.
                binding.favoriteButtonContainer.visibility = View.GONE
            }

            fun bind(exercise: Exercise) {
                binding.exerciseNameTextView.text = exercise.name

                // Glide 이미지 로드 (context 변수 사용)
                val imagePath = exercise.imagePath ?: ""
                val resId = if (imagePath.isNotBlank() && ::context.isInitialized) {
                    try { context.resources.getIdentifier(imagePath, "drawable", context.packageName).takeIf { it != 0 } }
                    catch (e: Exception) { R.drawable.ic_fitbuddy_logo }
                } else {
                    R.drawable.ic_fitbuddy_logo
                }

                Glide.with(binding.exerciseImageView.context)
                    .load(resId)
                    .placeholder(R.drawable.ic_fitbuddy_logo)
                    .error(R.drawable.ic_fitbuddy_logo)
                    .into(binding.exerciseImageView)

                // 선택 상태에 따른 배경색 변경
                binding.itemRootLayout.setBackgroundColor(
                    if (exercise.id == selectedItemId) Color.parseColor("#F0F0F0")
                    else Color.TRANSPARENT
                )
            }
        }
    }
}