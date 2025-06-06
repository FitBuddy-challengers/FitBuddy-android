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
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.*
import com.bumptech.glide.Glide
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.data.Exercise // Exercise 데이터 클래스 (isFavorite, isHidden 필드 포함)
import com.cookandroid.challengers.data.db.AppDatabase // loadTodayPlannedExercises에서 임시 사용
import com.cookandroid.challengers.databinding.FragmentExerciseListBinding
import com.cookandroid.challengers.databinding.ItemExerciseListBinding
import com.cookandroid.challengers.network.dto.ExerciseDto // 서버 응답 DTO
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

// ItemClickListener는 현재 코드에서 직접 사용되지 않으므로 필요시 주석 해제 또는 별도 관리
// class ItemClickListener(...)

class ExerciseListFragment : Fragment() {

    private var _binding: FragmentExerciseListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ExerciseListAdapter
    // private lateinit var db: AppDatabase // 서버를 주 데이터 소스로 사용, loadTodayPlannedExercises에서 임시 사용
    private var recyclerViewState: Parcelable? = null
    private var recentlyHiddenExercise: Exercise? = null
    private val todayPlannedExerciseIds = mutableSetOf<Long>()

    // 서버에서 받아온 전체 운동 목록 (필터링 전 원본, isFavorite 및 isHidden 상태 포함)
    private var allExercisesFromServer = listOf<Exercise>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExerciseListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope) // 직접 사용 최소화

        setupAdapter()
        binding.exerciseListRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ExerciseListFragment.adapter
            itemAnimator = null
        }

        // adapter.setContext(requireContext()) // ListAdapter는 context를 ViewHolder에서 가져옴

        setupSwipeToHide()
        observeExercisesFromServer() // ★ 서버에서 데이터 로드
        setupChipGroups()
        setupButtonClickListeners()

        loadTodayPlannedExercisesFromServer() // ★ 오늘 계획된 운동 ID도 서버에서 가져오도록 변경 필요
    }

    private fun loadTodayPlannedExercisesFromServer() {
        // TODO: 이 함수는 서버에서 오늘 계획된 운동 ID 목록을 가져오도록 수정해야 합니다.
        // 현재는 로컬 DB를 사용하고 있으므로, 서버 연동 시 이 부분의 재설계가 필요합니다.
        Log.w("ExerciseListFragment", "loadTodayPlannedExercisesFromServer: 서버 연동 로직 구현 필요")
        // 임시로 기존 로컬 DB 접근 유지 (서버 연동 시 이 부분 반드시 수정 필요)
        val tempDb = AppDatabase.getDatabase(requireContext(), lifecycleScope)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val todayStartMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                val todayEndMillis = todayStartMillis + TimeUnit.DAYS.toMillis(1) - 1
                val todayExercisePlans = tempDb.exercisePlanDao().getExercisePlansByDateRange(todayStartMillis, todayEndMillis)
                val plannedIds = mutableSetOf<Long>()
                todayExercisePlans.forEach { plan ->
                    val details = tempDb.planDetailDao().getPlanDetailsByExercisePlanIdOnce(plan.id)
                    details.forEach { detail -> plannedIds.add(detail.exerciseId) }
                }
                withContext(Dispatchers.Main) {
                    if(_binding == null) return@withContext
                    todayPlannedExerciseIds.clear()
                    todayPlannedExerciseIds.addAll(plannedIds)
                    Log.d("ExerciseListFragment", "오늘 계획된 운동 ID (Local): $todayPlannedExerciseIds")
                    applyFilters() // 스와이프 제한을 위해 필터 재적용
                }
            } catch (e: Exception) {
                Log.e("ExerciseListFragment", "오늘 계획된 운동 ID 로드 중 오류: ${e.message}", e)
            }
        }
    }

    private fun setupAdapter() {
        adapter = ExerciseListAdapter(
            onItemClicked = { exercise ->
                recyclerViewState = binding.exerciseListRecyclerView.layoutManager?.onSaveInstanceState()
                navigateToDetail(exercise.id)
            },
            onFavoriteClicked = { exerciseToToggle -> // ★ 서버 API 호출로 변경
                val newFavoriteState = !(exerciseToToggle.isFavorite ?: false)
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val response = RetrofitClient.exerciseApi.toggleExerciseFavorite(
                            exerciseToToggle.id,
                            RetrofitClient.ToggleFavoriteRequest(newFavoriteState)
                        )
                        if (response.isSuccessful && response.body() != null) {
                            val serverResponse = response.body()!! // ExerciseStateUpdateResponse
                            withContext(Dispatchers.Main) {
                                if(_binding == null) return@withContext
                                // ★ 서버 응답의 isHidden 값도 사용하여 리스트 업데이트
                                updateExerciseInList(
                                    exerciseToToggle.id,
                                    newIsFavorite = serverResponse.isFavorite,
                                    newIsHidden = serverResponse.isHidden // 서버 응답에 isHidden이 포함되어 있다고 가정
                                )
                                Toast.makeText(
                                    requireContext(),
                                    if (serverResponse.isFavorite) "'${exerciseToToggle.name}' 즐겨찾기 추가" else "'${exerciseToToggle.name}' 즐겨찾기 해제",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                if(_binding == null) return@withContext
                                Log.e("ExerciseList", "즐겨찾기 변경 실패: ${response.code()} ${response.message()}")
                                Toast.makeText(requireContext(), "즐겨찾기 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            if(_binding == null) return@withContext
                            Log.e("ExerciseList", "즐겨찾기 업데이트 오류", e)
                            Toast.makeText(requireContext(), "즐겨찾기 변경 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    private fun navigateToDetail(exerciseId: Long) {
        val bundle = Bundle().apply {
            putLong("exerciseId", exerciseId)
        }
        findNavController().navigate(R.id.action_global_exerciseDetailFragment, bundle)
    }

    private fun hideExercise(exercise: Exercise) { // ★ 서버 API 호출로 변경
        recentlyHiddenExercise = exercise // Undo를 위해 임시 저장
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.exerciseApi.toggleExerciseHidden(
                    exercise.id,
                    RetrofitClient.ToggleHiddenRequest(true) // 숨김 상태로 변경
                )
                if (response.isSuccessful && response.body() != null) {
                    val serverResponse = response.body()!! // ExerciseStateUpdateResponse
                    withContext(Dispatchers.Main) {
                        if(_binding == null) return@withContext
                        // ★ 서버 응답의 isFavorite 값도 사용하여 리스트 업데이트
                        updateExerciseInList(
                            exercise.id,
                            newIsFavorite = serverResponse.isFavorite, // 서버 응답에 isFavorite가 포함되어 있다고 가정
                            newIsHidden = true // 또는 serverResponse.isHidden (항상 true일 것임)
                        )
                        showUndoSnackbar(exercise.name)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        if(_binding == null) return@withContext
                        Log.e("ExerciseList", "운동 숨김 처리 실패: ${response.code()} ${response.message()}")
                        Toast.makeText(requireContext(), "운동 숨김 처리에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        // 실패 시 recentlyHiddenExercise를 원래대로 돌리거나, 사용자에게 재시도 안내
                        recentlyHiddenExercise = null
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if(_binding == null) return@withContext
                    Log.e("ExerciseList", "운동 숨김 처리 오류", e)
                    Toast.makeText(requireContext(), "운동 숨김 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    recentlyHiddenExercise = null
                }
            }
        }
    }

    private fun undoHideExercise() { // ★ 서버 API 호출로 변경
        recentlyHiddenExercise?.let { exerciseToUnhide ->
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = RetrofitClient.exerciseApi.toggleExerciseHidden(
                        exerciseToUnhide.id,
                        RetrofitClient.ToggleHiddenRequest(false) // 숨김 해제 상태로 변경
                    )
                    if (response.isSuccessful && response.body() != null) {
                        val serverResponse = response.body()!! // ExerciseStateUpdateResponse
                        withContext(Dispatchers.Main) {
                            if(_binding == null) return@withContext
                            updateExerciseInList(
                                exerciseToUnhide.id,
                                newIsFavorite = serverResponse.isFavorite,
                                newIsHidden = false // 또는 serverResponse.isHidden (항상 false일 것임)
                            )
                            recentlyHiddenExercise = null // Undo 완료 후 초기화
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            if(_binding == null) return@withContext
                            Log.e("ExerciseList", "숨김 취소 실패: ${response.code()} ${response.message()}")
                            Toast.makeText(requireContext(), "숨김 취소에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        if(_binding == null) return@withContext
                        Log.e("ExerciseList", "숨김 취소 오류", e)
                        Toast.makeText(requireContext(), "숨김 취소 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showUndoSnackbar(exerciseName: String) {
        if (_binding == null) return
        Snackbar.make(binding.root, "'${exerciseName}' 숨김 처리됨", Snackbar.LENGTH_LONG)
            .setActionTextColor(ContextCompat.getColor(requireContext(), R.color.blue)) // colors.xml에 blue 정의 필요
            .setAction("실행 취소") { undoHideExercise() }
            .show()
    }

    private fun observeExercisesFromServer() { // ★ 서버에서 데이터 로드 및 Room으로 이름/이미지 보완
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.exerciseApi.getAllExercises()
                if (response.isSuccessful) {
                    val dtoList = response.body() ?: emptyList()
// ExerciseDto를 앱 내부 Exercise 모델로 변환하면서 Room DB 정보로 보완
                    val localDb = AppDatabase.getDatabase(requireContext(), lifecycleScope) // Room DB 접근

                    val enrichedList = dtoList.map { dto -> // dto는 ExerciseDto 타입
                        val localExercise = localDb.exerciseDao().getExerciseById(dto.id) // Room에서 해당 ID의 운동 정보 가져오기
                        Log.d("ExerciseListFragment", "🧪 exerciseId: ${dto.id} → 서버 DTO: $dto")
                        Log.d("ExerciseListFragment", "🧪 exerciseId: ${dto.id} → Room에서 찾은 운동: $localExercise")

                        Exercise( // 앱 내부 Exercise 모델 객체 생성
                            id = dto.id,
                            name = dto.name.ifBlank { localExercise?.name ?: "이름 없음" }, // 서버 이름이 비어있으면 로컬 이름, 그것도 없으면 기본값

                            // 아래 필드들은 로컬 DB 값을 우선적으로 사용하고, 없으면 서버 DTO 값, 그것도 없으면 기본값 사용
                            part = localExercise?.part?.ifBlank { dto.part } ?: dto.part ?: "부위 정보 없음",
                            equip = localExercise?.equip?.ifBlank { dto.equip } ?: dto.equip ?: "장비 정보 없음",
                            imagePath = localExercise?.imagePath?.ifBlank { dto.image_path } ?: dto.image_path ?: "", // 로컬 우선, 다음 서버, 다음 기본값

                            // 상세 정보 필드들: 로컬 DB 값이 있으면 사용, 없으면 서버 DTO 값 사용
                            startPosition = localExercise?.startPosition ?: dto.start_position,
                            exerciseMotion = localExercise?.exerciseMotion ?: dto.exercise_motion,
                            breathing = localExercise?.breathing ?: dto.breathing,
                            caution = localExercise?.caution ?: dto.caution,

                            mets = dto.mets, // 서버 값 사용 (또는 localExercise?.mets ?: dto.mets 로 보완 가능)

                            // 상태 플래그들은 서버 값을 우선적으로 사용
                            isFavorite = dto.isFavorite ?: false,
                            isTimeType = dto.isTimeType,
                            isHidden = dto.isHidden ?: false
                        )
                    }
                    allExercisesFromServer = enrichedList // 보완된 리스트를 멤버 변수에 저장
                    withContext(Dispatchers.Main) {
                        if (_binding != null) {
                            applyFilters()
                        }
                    }
                } else {
                    Log.e("ExerciseListFragment", "운동 목록 로드 실패: ${response.code()}")
                    withContext(Dispatchers.Main) {
                        if (_binding != null) Toast.makeText(requireContext(), "운동 목록을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ExerciseListFragment", "운동 목록 로드 중 오류: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (_binding != null) Toast.makeText(requireContext(), "운동 목록 로드 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupSwipeToHide() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private var isToastShownMap = mutableMapOf<Long, Boolean>()

            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < adapter.currentList.size) {
                    val exercise = adapter.currentList[position]
                    if (todayPlannedExerciseIds.contains(exercise.id)) {
                        if (isToastShownMap[exercise.id] != true) {
                            Toast.makeText(requireContext(), "오늘 계획에 있는 운동은 숨길 수 없습니다.", Toast.LENGTH_SHORT).show()
                            isToastShownMap[exercise.id] = true
                        }
                        return 0
                    } else {
                        return ItemTouchHelper.LEFT
                    }
                }
                return 0
            }
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < adapter.currentList.size) { // 범위 체크 추가
                    adapter.currentList.getOrNull(position)?.let { exercise -> // getOrNull로 안전하게 접근
                        isToastShownMap.remove(exercise.id)
                    }
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < adapter.currentList.size) { // 범위 체크 추가
                    val exercise = adapter.currentList[position]
                    hideExercise(exercise)
                }
            }

            override fun onChildDraw(c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) { // 왼쪽으로 스와이프 할 때만 그리기
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
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.exerciseListRecyclerView)
    }

    private fun setupChipGroups() { /* 기존 코드 유지 */
        setupChips(binding.myChipGroup, listOf("즐겨찾기", "최근 한 운동"))
        setupChips(binding.partChipGroup, listOf("가슴", "등", "하체", "어깨", "복근", "유산소"))
        setupChips(binding.equipmentChipGroup, listOf("맨몸", "덤벨", "케틀벨", "세라밴드", "스텝박스","짐볼"))
    }

    private fun setupButtonClickListeners() { /* 기존 코드 유지 */
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
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            if (_binding == null) return@launch

            val favFilter = binding.myChipGroup.checkedChipIds.any {
                binding.myChipGroup.findViewById<Chip>(it)?.text == "즐겨찾기"
            }
            val parts = binding.partChipGroup.checkedChipIds.mapNotNull {
                binding.partChipGroup.findViewById<Chip>(it)?.text?.toString()
            }
            val equips = binding.equipmentChipGroup.checkedChipIds.mapNotNull {
                binding.equipmentChipGroup.findViewById<Chip>(it)?.text?.toString()
            }

            val filtered = allExercisesFromServer // ★ DB 대신 멤버 변수 사용
                .filter { exercise -> !(exercise.isHidden ?: false) } // ★ 숨김 처리된 운동 제외
                .filter { e ->
                    (!favFilter || (e.isFavorite ?: false)) &&
                            (parts.isEmpty() || parts.any { e.part.contains(it, ignoreCase = true) }) &&
                            (equips.isEmpty() || equips.any { e.equip.contains(it, ignoreCase = true) })
                }

            adapter.submitList(filtered) { // submitList의 완료 콜백 사용
                recyclerViewState?.let {
                    if (_binding != null) { // 한 번 더 체크
                        binding.exerciseListRecyclerView.layoutManager?.onRestoreInstanceState(it)
                    }
                    recyclerViewState = null
                }
            }
        }
    }

    private fun updateExerciseInList(exerciseId: Long, newIsFavorite: Boolean?, newIsHidden: Boolean?) {
        val globalIndex = allExercisesFromServer.indexOfFirst { it.id == exerciseId }
        if (globalIndex != -1) {
            val currentItem = allExercisesFromServer[globalIndex]
            val updatedItem = currentItem.copy(
                isFavorite = newIsFavorite ?: currentItem.isFavorite, // null이면 기존 값 유지
                isHidden = newIsHidden ?: currentItem.isHidden       // null이면 기존 값 유지
            )
            // allExercisesFromServer를 변경 가능한 리스트로 만들거나, 새 리스트로 교체
            val tempList = allExercisesFromServer.toMutableList()
            if (globalIndex < tempList.size) { // 범위 체크 강화
                tempList[globalIndex] = updatedItem
                allExercisesFromServer = tempList.toList() // toList()로 불변 리스트로 다시 할당
            }
        }
        // 상태가 변경되었으므로 필터를 다시 적용하여 어댑터에 새 목록을 제출합니다.
        applyFilters()
    }


    private class ExerciseListAdapter(
        private val onItemClicked: (Exercise) -> Unit,
        private val onFavoriteClicked: (Exercise) -> Unit
    ) : ListAdapter<Exercise, ExerciseListAdapter.ExerciseViewHolder>(ExerciseDiffCallback()) {

        // private lateinit var context: Context // ViewHolder에서 가져오도록 변경

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
            val binding = ItemExerciseListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ExerciseViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class ExerciseViewHolder(private val binding: ItemExerciseListBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {
                binding.itemContentLayout.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClicked(getItem(position))
                    }
                }
                binding.favoriteButtonContainer.setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onFavoriteClicked(getItem(position))
                    }
                }
            }

            fun bind(exercise: Exercise) {
                binding.exerciseNameTextView.text = exercise.name
                binding.favoriteButton.isSelected = exercise.isFavorite ?: false

                // Glide 이미지 로드
                val imagePath = exercise.imagePath
                val context = itemView.context // ViewHolder의 itemView에서 context 가져오기
                val resId = if (!imagePath.isNullOrBlank()) {
                    context.resources.getIdentifier(
                        imagePath,
                        "drawable",
                        context.packageName
                    ).takeIf { it != 0 }
                } else {
                    null
                }

                Glide.with(context)
                    .load(resId ?: R.drawable.ic_launcher_background) // resId가 null이면 기본 이미지
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background) // ic_default_exercise_error drawable 필요
                    .into(binding.exerciseImageView)
            }
        }
    }

    private class ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean = oldItem == newItem
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.exerciseListRecyclerView.adapter = null // 어댑터 참조 해제
        _binding = null
    }
}
