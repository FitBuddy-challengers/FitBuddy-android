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
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.*
import com.bumptech.glide.Glide // Glide 추가
import com.cookandroid.challengers.api.RetrofitClient // API 클라이언트
import com.cookandroid.challengers.data.Exercise
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentExerciseHiddenBinding
import com.cookandroid.challengers.databinding.ItemExerciseListBinding
import com.cookandroid.challengers.network.dto.ExerciseDto // 서버 DTO
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExerciseHiddenFragment : Fragment() {

    private var _binding: FragmentExerciseHiddenBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HiddenExerciseListAdapter
    private var recyclerViewState: Parcelable? = null

    // 서버에서 받아온 전체 숨김 운동 목록 (필터링 전 원본)
    private var allHiddenExercisesFromServer = listOf<Exercise>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseHiddenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        binding.exerciseListRecyclerView.apply { // RecyclerView ID가 exerciseListRecyclerView라고 가정
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ExerciseHiddenFragment.adapter
            itemAnimator = null
        }

        setupSwipeToUnhide()
        observeHiddenExercisesFromServer() // ★ 서버에서 데이터 로드
        initChips() // 필터 칩 초기화

        binding.backButton.setOnClickListener { findNavController().popBackStack() }

        // ExerciseListFragment에서 즐겨찾기/숨김 상태 변경 시 이 화면도 갱신하기 위한 리스너 (선택적)
        parentFragmentManager.setFragmentResultListener("favorite_status_updated", viewLifecycleOwner) { _, bundle ->
            val changedExerciseId = bundle.getLong("exerciseId")
            val newIsFavorite = bundle.getBoolean("isFavorite")
            // 숨김 목록에 있는 아이템의 즐겨찾기 상태만 업데이트 (isHidden은 변경되지 않음)
            updateExerciseStateInHiddenList(changedExerciseId, newIsFavorite = newIsFavorite, newIsHidden = null)
        }
        parentFragmentManager.setFragmentResultListener("exercise_unhidden_from_list", viewLifecycleOwner) { _, bundle ->
            // ExerciseListFragment에서 숨김 해제 시 이 목록에서도 제거 (또는 다시 로드)
            val unhiddenExerciseId = bundle.getLong("exerciseId")
            removeExerciseFromHiddenList(unhiddenExerciseId)
        }
        parentFragmentManager.setFragmentResultListener("exercise_hidden_from_list", viewLifecycleOwner) { _, bundle ->
            // ExerciseListFragment에서 숨김 시 이 목록에 추가 (또는 다시 로드)
            observeHiddenExercisesFromServer() // 간단하게 전체 다시 로드
        }
    }

    private fun setupAdapter() {
        adapter = HiddenExerciseListAdapter(
            onItemClicked = { exercise ->
                // 숨김 목록 아이템 클릭 시 상세 화면 이동 등
                val bundle = Bundle().apply { putLong("exerciseId", exercise.id) }
                findNavController().navigate(R.id.action_global_exerciseDetailFragment, bundle)
            },
            // ★ 수정: 파라미터 이름 일치
            onUnhideClickedListener = { exerciseToUnhide ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val response = RetrofitClient.exerciseApi.toggleExerciseHidden(
                            exerciseToUnhide.id,
                            RetrofitClient.ToggleHiddenRequest(false) // 숨김 해제
                        )
                        if (response.isSuccessful && response.body() != null) {
                            // val serverResponse = response.body()!! // 사용하지 않으므로 제거 가능
                            withContext(Dispatchers.Main) {
                                if(_binding == null) return@withContext
                                Toast.makeText(requireContext(), "'${exerciseToUnhide.name}' 숨김 해제됨", Toast.LENGTH_SHORT).show()
                                removeExerciseFromHiddenList(exerciseToUnhide.id)
                                parentFragmentManager.setFragmentResult("exercise_unhidden", Bundle().apply {
                                    putLong("exerciseId", exerciseToUnhide.id)
                                })
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                if(_binding == null) return@withContext
                                Log.e("ExerciseHiddenFragment", "숨김 해제 실패: ${response.code()} ${response.message()}")
                                Toast.makeText(requireContext(), "숨김 해제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            if(_binding == null) return@withContext
                            Log.e("ExerciseHiddenFragment", "숨김 해제 중 오류", e)
                            Toast.makeText(requireContext(), "숨김 해제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            // ★ 수정: 파라미터 이름 일치
            onFavoriteClickedListener = { exerciseToToggle ->
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
                                updateExerciseStateInHiddenList(
                                    exerciseToToggle.id,
                                    newIsFavorite = serverResponse.isFavorite,
                                    newIsHidden = serverResponse.isHidden // isHidden은 이 API 응답에서 변경되지 않을 수 있음
                                )
                                Toast.makeText(requireContext(), if (serverResponse.isFavorite) "'${exerciseToToggle.name}' 즐겨찾기 추가" else "'${exerciseToToggle.name}' 즐겨찾기 해제", Toast.LENGTH_SHORT).show()
                                parentFragmentManager.setFragmentResult("favorite_status_updated", Bundle().apply {
                                    putLong("exerciseId", exerciseToToggle.id)
                                    putBoolean("isFavorite", serverResponse.isFavorite)
                                })
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                if(_binding == null) return@withContext
                                Log.e("ExerciseHiddenFragment", "즐겨찾기 변경 실패: ${response.code()} ${response.message()}")
                                Toast.makeText(requireContext(), "즐겨찾기 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            if(_binding == null) return@withContext
                            Log.e("ExerciseHiddenFragment", "즐겨찾기 변경 중 오류", e)
                            Toast.makeText(requireContext(), "즐겨찾기 변경 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    private fun observeHiddenExercisesFromServer() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.exerciseApi.getAllExercises()
                if (response.isSuccessful) {
                    val dtoList = response.body() ?: emptyList()
                    val localDbForEnrich = AppDatabase.getDatabase(requireContext(), lifecycleScope)
                    allHiddenExercisesFromServer = dtoList.mapNotNull { dto ->
                        if (dto.isHidden == true) {
                            val localExercise = localDbForEnrich.exerciseDao().getExerciseById(dto.id)
                            Exercise(
                                id = dto.id,
                                name = dto.name.ifBlank { localExercise?.name ?: "이름 없음" },
                                part = dto.part,
                                equip = dto.equip,
                                imagePath = if (dto.image_path.isNullOrBlank()) localExercise?.imagePath else dto.image_path,
                                startPosition = dto.start_position ?: localExercise?.startPosition,
                                exerciseMotion = dto.exercise_motion ?: localExercise?.exerciseMotion,
                                breathing = dto.breathing ?: localExercise?.breathing,
                                caution = dto.caution ?: localExercise?.caution,
                                mets = dto.mets,
                                isFavorite = dto.isFavorite ?: false,
                                isTimeType = dto.isTimeType,
                                isNoise = dto.is_noise,
                                isHidden = true
                            )
                        } else {
                            null
                        }
                    }
                    withContext(Dispatchers.Main) {
                        if (_binding != null) {
                            applyFilters()
                        }
                    }
                } else {
                    Log.e("ExerciseHiddenFragment", "숨김 운동 목록 로드 실패: ${response.code()}")
                    withContext(Dispatchers.Main) {
                        if (_binding != null) Toast.makeText(requireContext(), "숨김 운동 목록을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ExerciseHiddenFragment", "숨김 운동 목록 로드 중 오류: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (_binding != null) Toast.makeText(requireContext(), "숨김 운동 목록 로드 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupSwipeToUnhide() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove( rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < adapter.currentList.size) {
                    val exercise = adapter.currentList[position]
                    adapter.callOnUnhideClicked(exercise)
                }
            }

            override fun onChildDraw(c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    val itemView = vh.itemView
                    val color = ContextCompat.getColor(requireContext(), R.color.light_gray2) // colors.xml에 light_green 정의 필요
                    val background = ColorDrawable(color)
                    background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    background.draw(c)

                    val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_unhide) // ic_unhide drawable 필요
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

            val filtered = allHiddenExercisesFromServer
                .filter { e ->
                    (!favFilter || (e.isFavorite ?: false)) &&
                            (parts.isEmpty() || parts.any { e.part.contains(it, ignoreCase = true) }) &&
                            (equips.isEmpty() || equips.any { e.equip.contains(it, ignoreCase = true) })
                }

            adapter.submitList(filtered) {
                recyclerViewState?.let {
                    if (_binding != null) {
                        binding.exerciseListRecyclerView.layoutManager?.onRestoreInstanceState(it)
                    }
                    recyclerViewState = null
                }
            }
        }
    }

    private fun removeExerciseFromHiddenList(exerciseId: Long) {
        allHiddenExercisesFromServer = allHiddenExercisesFromServer.filterNot { it.id == exerciseId }
        applyFilters()
    }

    private fun updateExerciseStateInHiddenList(exerciseId: Long, newIsFavorite: Boolean?, newIsHidden: Boolean?) {
        val globalIndex = allHiddenExercisesFromServer.indexOfFirst { it.id == exerciseId }
        if (globalIndex != -1) {
            val currentItem = allHiddenExercisesFromServer[globalIndex]
            val updatedItem = currentItem.copy(
                isFavorite = newIsFavorite ?: currentItem.isFavorite,
                isHidden = newIsHidden ?: currentItem.isHidden
            )
            val tempList = allHiddenExercisesFromServer.toMutableList()
            if (globalIndex < tempList.size) {
                if (updatedItem.isHidden == false) {
                    tempList.removeAt(globalIndex)
                } else {
                    tempList[globalIndex] = updatedItem
                }
                allHiddenExercisesFromServer = tempList.toList()
            }
        }
        applyFilters()
    }

    override fun onPause() {
        super.onPause()
        if (_binding != null) {
            recyclerViewState = binding.exerciseListRecyclerView.layoutManager?.onSaveInstanceState()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.exerciseListRecyclerView.adapter = null
        _binding = null
    }
}

private class HiddenExerciseListAdapter(
    private val onItemClicked: (Exercise) -> Unit,
    private val onUnhideClickedListener: (Exercise) -> Unit,
    private val onFavoriteClickedListener: (Exercise) -> Unit
) : ListAdapter<Exercise, HiddenExerciseListAdapter.HiddenExerciseViewHolder>(ExerciseDiffCallback()) {

    fun callOnUnhideClicked(exercise: Exercise) {
        onUnhideClickedListener(exercise)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HiddenExerciseViewHolder {
        val binding = ItemExerciseListBinding.inflate(LayoutInflater.from(parent.context), parent, false) // ★ ItemExerciseListBinding으로 변경
        return HiddenExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HiddenExerciseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HiddenExerciseViewHolder(private val binding: ItemExerciseListBinding) : // ★ ItemExerciseListBinding으로 변경
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.itemContentLayout.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { onItemClicked(it) }
                }
            }
            binding.favoriteButtonContainer.setOnClickListener { // ★ favoriteButtonContainer ID가 ItemExerciseListBinding에 있어야 함
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    getItem(position)?.let { onFavoriteClickedListener(it) }
                }
            }
        }

        fun bind(exercise: Exercise) {
            binding.exerciseNameTextView.text = exercise.name
            binding.favoriteButton.isSelected = exercise.isFavorite ?: false // ★ favoriteButton ID가 ItemExerciseListBinding에 있어야 함

            val context = itemView.context
            val imagePath = exercise.imagePath
            val resId = if (!imagePath.isNullOrBlank()) {
                context.resources.getIdentifier(
                    imagePath, "drawable", context.packageName
                ).takeIf { it != 0 }
            } else { null }

            Glide.with(context)
                .load(resId ?: R.drawable.ic_launcher_background) // 기본 이미지 리소스 필요
                .placeholder(R.drawable.ic_launcher_background) // 로딩 중 이미지 리소스 필요
                .into(binding.exerciseImageView)
        }
    }
}

private class ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
    override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean = oldItem == newItem
}