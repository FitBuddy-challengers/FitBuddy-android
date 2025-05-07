package com.cookandroid.challengers

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Parcelable
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cookandroid.challengers.data.Exercise
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentExerciseListBinding
import com.cookandroid.challengers.databinding.ItemAddExerciseBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import kotlin.also
import kotlin.apply
import kotlin.collections.any
import kotlin.collections.filter
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.let
import kotlin.text.contains

class ExerciseListFragment : Fragment() {

    private var _binding: FragmentExerciseListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ExerciseListAdapter
    private lateinit var db: AppDatabase
    private var recyclerViewState: Parcelable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExerciseListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)

        // 어댑터 초기화 및 클릭 리스너 설정
        adapter = ExerciseListAdapter(
            onItemClicked = { exercise ->
                // 상세 화면으로 이동하기 전에 현재 RecyclerView 상태 저장
                recyclerViewState = binding.exerciseListRecyclerView.layoutManager?.onSaveInstanceState()
                val bundle = Bundle().apply {
                    putLong("exerciseId", exercise.id)
                }
//                findNavController().navigate(R.id.action_exerciseListFragment_to_exerciseDetailFragment, bundle)
            },
            onFavoriteClicked = { ex ->
                lifecycleScope.launch(Dispatchers.IO) {
                    db.exerciseDao().update(ex.copy(isFavorite = !ex.isFavorite))
                }
            }
        )

        // RecyclerView 설정
        binding.exerciseListRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ExerciseListFragment.adapter
        }

        // 어댑터에 Context 설정
        adapter.setContext(requireContext())

        // 데이터베이스에서 운동 목록을 관찰하고 어댑터에 제출
        lifecycleScope.launch(Dispatchers.IO) {
            db.exerciseDao().getAllExercises().collectLatest { all ->
                withContext(Dispatchers.Main) {
                    adapter.submitList(all)
                    // 저장된 RecyclerView 상태가 있다면 복원
                    if (recyclerViewState != null) {
                        binding.exerciseListRecyclerView.layoutManager?.onRestoreInstanceState(
                            recyclerViewState
                        )
                        recyclerViewState = null // 상태를 한 번 복원했으므로 null로 초기화
                    }
                }
            }
        }

        // 칩 그룹 설정
        setupChips(binding.myChipGroup, listOf("즐겨찾기", "최근 한 운동"))
        setupChips(binding.partChipGroup, listOf("가슴", "등", "하체", "어깨", "복근", "유산소"))
        setupChips(binding.equipmentChipGroup, listOf("맨몸", "덤벨", "케틀벨", "세라밴드", "스텝박스"))

        // 뒤로가기 버튼 클릭 리스너
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        // 숨김 운동 버튼 클릭 리스너 (TODO: 로직 구현 필요)
        binding.hiddenExerciseButton.setOnClickListener{
            //TODO: 숨김 운동을 처리하는 로직을 구현
        }
    }

    override fun onPause() {
        super.onPause()
        recyclerViewState = binding.exerciseListRecyclerView.layoutManager?.onSaveInstanceState()
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
            val binding = ItemAddExerciseBinding.inflate(LayoutInflater.from(context), parent, false)
            return ExerciseViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class ExerciseViewHolder(private val binding: ItemAddExerciseBinding) :
            RecyclerView.ViewHolder(binding.root) {
            init {
                binding.root.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        getItem(position)?.let {
                            onItemClicked(it)
                        }
                    }
                }
                binding.favoriteButton.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        getItem(position)?.let { onFavoriteClicked(it) }
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
