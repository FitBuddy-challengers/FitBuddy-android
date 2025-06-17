package com.cookandroid.challengers

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
// import android.service.notification.Condition.newId // 사용하지 않는 import, 제거 가능
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment // 사용하지 않으므로 제거 가능
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil // 사용하지 않으므로 제거 가능
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter // 사용하지 않으므로 제거 가능
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.data.Exercise
import com.cookandroid.challengers.data.ExercisePlan // 사용하지 않으므로 제거 가능
import com.cookandroid.challengers.data.ExerciseSet // 사용하지 않으므로 제거 가능
import com.cookandroid.challengers.data.PlanDetail // 사용하지 않으므로 제거 가능
import com.cookandroid.challengers.data.PlanDetailDao // 사용하지 않으므로 제거 가능
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

    companion object {
        private const val TAG = "ExerciseChangeFragment" // 로그 태그 추가
        private const val ARG_PLAN_ID = "planId"
        private const val ARG_ORIGINAL_EXERCISE_ID = "originalExerciseId" // 명확한 이름으로 변경
        private const val ARG_CURRENT_SCHEDULE_ID = "currentScheduleId" // 세 번째 인자를 위한 키 추가

        // newInstance 함수 수정: currentScheduleId 인자 추가
        fun newInstance(planId: Long, originalExerciseId: Long, currentScheduleId: Long): ExerciseChangeFragment {
            val fragment = ExerciseChangeFragment()
            val args = Bundle().apply {
                putLong(ARG_PLAN_ID, planId)
                putLong(ARG_ORIGINAL_EXERCISE_ID, originalExerciseId)
                putLong(ARG_CURRENT_SCHEDULE_ID, currentScheduleId) // 새 인자 저장
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
    private var planId: Long = -1L
    private var originalExerciseId: Long = -1L // 전달받은 기존 운동 ID
    private var currentScheduleId: Long = -1L // ExerciseEditFragment로부터 전달받은 scheduleId
    private var selectedExercise: Exercise? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            planId = it.getLong(ARG_PLAN_ID, -1L)
            originalExerciseId = it.getLong(ARG_ORIGINAL_EXERCISE_ID, -1L)
            currentScheduleId = it.getLong(ARG_CURRENT_SCHEDULE_ID, -1L) // 전달받은 currentScheduleId 사용
        }
        Log.d(TAG, "onCreate: planId=$planId, originalExerciseId=$originalExerciseId, currentScheduleId=$currentScheduleId")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheet = (dialogInterface as BottomSheetDialog)
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                it.setBackgroundColor(Color.WHITE)
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                // isDraggable 기본값은 true 이므로 생략 가능
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

        adapter = ChangeExerciseAdapter(emptyList()) { ex, isSelected ->
            selectedExercise = if (isSelected) ex else null
            Log.d(TAG, "Exercise selected: ${selectedExercise?.name}, isSelected: $isSelected")
        }
        adapter.setContext(requireContext()) // Context 전달

        binding.exerciseListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.exerciseListRecyclerView.adapter = adapter

        loadExercisesFromRoom()
        setupChips(binding.myChipGroup, listOf("즐겨찾기")) // "최근 한 운동" 필터는 현재 로직에 없음
        setupChips(binding.partChipGroup, listOf("가슴", "등", "하체", "어깨", "팔", "복근", "유산소"))
        setupChips(binding.equipmentChipGroup, listOf("맨몸", "덤벨", "케틀벨", "세라밴드", "스텝박스", "짐볼"))

        binding.changeCompleteButton.setOnClickListener {
            val newSelectedExerciseId = selectedExercise?.id
            if (newSelectedExerciseId == null) {
                Toast.makeText(requireContext(), "운동을 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentScheduleId <= 0) { // 전달받은 currentScheduleId 유효성 검사
                Toast.makeText(requireContext(), "운동 변경을 위한 스케줄 정보가 올바르지 않습니다.", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "changeCompleteButton: Invalid currentScheduleId: $currentScheduleId")
                return@setOnClickListener
            }

            Log.d(TAG, "🚀 변경 요청할 새 운동 ID: $newSelectedExerciseId, 이름: ${selectedExercise?.name}. 기존 scheduleId: $currentScheduleId")

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // 서버에 운동 변경 요청 (전달받은 currentScheduleId 사용)
                    Log.d(TAG, "📤 서버에 운동 변경 요청 - scheduleId: $currentScheduleId, newExerciseId: $newSelectedExerciseId")
                    val changeResponse = RetrofitClient.scheduleApi.changeExerciseServer(
                        currentScheduleId, // 경로 파라미터로 전달받은 scheduleId 사용
                        RetrofitClient.ChangeExerciseServerRequest(newSelectedExerciseId) // 요청 본문
                    )

                    if (!changeResponse.isSuccessful || changeResponse.body() == null) {
                        val errorBody = changeResponse.errorBody()?.string() ?: "알 수 없는 오류"
                        throw Exception("❌ 운동 변경 API 실패: ${changeResponse.code()} - $errorBody")
                    }
                    Log.d(TAG, "✅ 운동 변경 API 성공: ${changeResponse.body()?.message}")


                    // 운동 변경 성공 후, 부모 프래그먼트에 결과 전달
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "'${selectedExercise?.name}'(으)로 운동이 변경되었습니다.", Toast.LENGTH_SHORT).show()
                        setFragmentResult("exercise_changed", bundleOf(
                            "newId" to newSelectedExerciseId,
                            "changedScheduleId" to currentScheduleId // 변경된 운동이 적용된 scheduleId 전달
                        ))
                        dismiss()
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "운동 변경 전체 과정 오류: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "운동 변경 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        // 실패 시 dismiss()를 할지 여부는 정책에 따라 결정
                    }
                }
            }
        }

        binding.backButton.setOnClickListener {
            // findNavController().popBackStack() // BottomSheetDialogFragment에서는 dismiss() 사용 권장
            dismiss()
        }
    }

    private fun loadExercisesFromRoom() {
        val db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
        lifecycleScope.launch(Dispatchers.IO) {
            db.exerciseDao().getAllExercises().collectLatest { allExercises ->
                Log.d(TAG, "📥 RoomDB 전체 운동 수: ${allExercises.size}")
                // 현재 변경 대상인 originalExerciseId는 목록에서 제외
                val filteredExercises = allExercises.filter { it.id != originalExerciseId }
                Log.d(TAG, "📤 현재 운동(ID: $originalExerciseId) 제외 후 목록 수: ${filteredExercises.size}")
                withContext(Dispatchers.Main) {
                    adapter.submitList(filteredExercises)
                }
            }
        }
    }

    private fun setupChips(chipGroup: ChipGroup, chipTexts: List<String>) {
        val defaultCornerRadius = resources.getDimension(R.dimen.default_chip_corner_radius)
        // val selectedCornerRadius = resources.getDimension(R.dimen.selected_chip_corner_radius) // 필요시 사용

        chipTexts.forEach { text ->
            val chip = Chip(ContextThemeWrapper(context, R.style.TextChip)).apply { // 스타일 적용된 Context 사용
                this.text = text
                this.isCheckable = true
                this.isClickable = true
                // 초기 모양 설정
                this.shapeAppearanceModel = ShapeAppearanceModel.builder()
                    .setAllCorners(CornerFamily.ROUNDED, defaultCornerRadius)
                    .build()
                // 칩 배경 및 텍스트 색상 등은 스타일에 정의된 것을 따르도록 함.
                // 필요시 여기서 직접 설정 가능:
                // this.setChipBackgroundColorResource(R.color.chip_default_background_color)
                // this.setTextColor(ContextCompat.getColor(requireContext(), R.color.chip_default_text_color))
            }
            chip.setOnCheckedChangeListener { _, _ -> applyFilters() }
            chipGroup.addView(chip)
        }
    }

    private fun applyFilters() {
        val db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)
        lifecycleScope.launch(Dispatchers.IO) {
            db.exerciseDao().getAllExercises().collectLatest { allExercises ->
                val isFavoriteFilterEnabled = binding.myChipGroup.checkedChipIds.any {
                    binding.myChipGroup.findViewById<Chip>(it).text == "즐겨찾기"
                }
                val selectedParts = binding.partChipGroup.checkedChipIds.map {
                    binding.partChipGroup.findViewById<Chip>(it).text.toString()
                }
                val selectedEquips = binding.equipmentChipGroup.checkedChipIds.map {
                    binding.equipmentChipGroup.findViewById<Chip>(it).text.toString()
                }

                Log.d(TAG, "Applying filters - Favorite: $isFavoriteFilterEnabled, Parts: $selectedParts, Equips: $selectedEquips")

                val filteredList = allExercises.filter { exercise ->
                    (exercise.id != originalExerciseId) && // 현재 운동 제외
                            (!isFavoriteFilterEnabled || exercise.isFavorite) &&
                            (selectedParts.isEmpty() || selectedParts.any { part -> exercise.part.contains(part, ignoreCase = true) }) &&
                            (selectedEquips.isEmpty() || selectedEquips.any { equip -> exercise.equip.contains(equip, ignoreCase = true) })
                }
                Log.d(TAG, "Filtered list size: ${filteredList.size}")
                withContext(Dispatchers.Main) {
                    adapter.submitList(filteredList)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지
        Log.d(TAG, "onDestroyView")
    }

    // Adapter 클래스 (내부 또는 별도 파일로 분리 가능)
    class ChangeExerciseAdapter(
        private var items: List<Exercise>,
        private val onExerciseSelected: (exercise: Exercise, isSelected: Boolean) -> Unit
    ) : RecyclerView.Adapter<ChangeExerciseAdapter.ExerciseViewHolder>() {

        private lateinit var context: Context // ViewHolder에서 사용하기 위해 클래스 레벨로 이동
        private var selectedItemId: Long? = null // 현재 선택된 아이템의 ID

        fun setContext(ctx: Context) { // 외부에서 Context 설정
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
            // 생성자에서 context를 받거나, adapter 생성 시점에 setContext가 호출되었다고 가정
            if (!::context.isInitialized) {
                context = parent.context // 안전장치
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
                            notifyItemChanged(position) // 현재 아이템만 갱신
                        } else { // 새 아이템 선택
                            selectedItemId = exercise.id
                            onExerciseSelected(exercise, true)
                            notifyItemChanged(position) // 현재 아이템 갱신
                            // 이전에 선택된 아이템이 있었다면 그것도 갱신하여 선택 해제 표시
                            if (previouslySelectedId != null) {
                                val prevPosition = items.indexOfFirst { it.id == previouslySelectedId }
                                if (prevPosition != -1) notifyItemChanged(prevPosition)
                            }
                        }
                    }
                }
            }

            fun bind(exercise: Exercise) {
                binding.exerciseNameTextView.text = exercise.name
                // Glide 이미지 로드 (context 변수 사용)
                val imagePath = exercise.imagePath ?: ""
                val resId = if (imagePath.isNotBlank() && ::context.isInitialized) {
                    try { context.resources.getIdentifier(imagePath, "drawable", context.packageName).takeIf { it != 0 } }
                    catch (e: Exception) { R.drawable.ic_fitbuddy_logo } // 리소스 못찾을 시 기본 이미지
                } else {
                    R.drawable.ic_fitbuddy_logo // 경로 없거나 context 미초기화 시 기본 이미지
                }

                Glide.with(binding.exerciseImageView.context) // ViewHolder의 context 사용
                    .load(resId)
                    .placeholder(R.drawable.ic_fitbuddy_logo)
                    .error(R.drawable.ic_fitbuddy_logo)
                    .into(binding.exerciseImageView)

                // 선택 상태에 따른 배경색 변경
                binding.itemRootLayout.setBackgroundColor(
                    if (exercise.id == selectedItemId) Color.parseColor("#E0E0E0") // 약간 더 연한 회색
                    else Color.TRANSPARENT
                )
                // 즐겨찾기 버튼은 ItemAddExerciseBinding에 없으므로 관련 코드 제거 또는 주석 처리
                // binding.favoriteButton.isSelected = exercise.isFavorite
            }
        }
    }
}