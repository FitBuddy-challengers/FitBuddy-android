package com.cookandroid.challengers

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cookandroid.challengers.BuildConfig.BASE_URL
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.api.RetrofitClient.PhotoChallengeItem
import com.cookandroid.challengers.databinding.FragmentChallengePhotoBinding
import com.cookandroid.challengers.util.UserPreference
import com.cookandroid.challengers.viewmodel.ChallengePhotoViewModel
import com.google.android.material.imageview.ShapeableImageView

class ChallengePhotoFragment : Fragment() {
    private var _binding: FragmentChallengePhotoBinding? = null
    private val binding get() = _binding!!

    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var stampGridAdapter: StampGridAdapter // 하단 그리드용 어댑터
    private lateinit var viewModel: ChallengePhotoViewModel
    private lateinit var userPreference: UserPreference

    companion object {
        private const val TAG = "ChallengePhotoFragment"

        private const val SPAN_COUNT = 4
        private const val GRID_SPACING_DP = 16
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChallengePhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[ChallengePhotoViewModel::class]
        userPreference = UserPreference(requireContext())

        setupPhotoRecyclerView()
        setupExerciseNowRecyclerView()

        observeViewModel()

        binding.fabAddPhoto.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_challenge_to_uploadPhoto)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("GroupPageFragment", "Navigation to ChallengeUploadPhotoFragment failed", e)
                Toast.makeText(context, "사진 업로드 페이지 이동에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val userId = userPreference.getUserId()
        if (userId != -1) {
            viewModel.fetchWeeklyPhotos(userId)
            // 월간 운동 횟수 요청 함수 호출 추가
            viewModel.fetchMonthlyWorkoutCount(userId)
        }
    }

    // ★★★ observeViewModel 수정
    private fun observeViewModel() {
        viewModel.weeklyPhotos.observe(viewLifecycleOwner) { photoListWithNulls ->

            // 1️⃣ null 제외 + 최신순 정렬 (날짜 기준)
            val orderedPhotos = photoListWithNulls
                .filterNotNull()
                .sortedByDescending { it.date }   // 최신 → 오래된 순

            // 2️⃣ 나머지를 null 로 채워서 총 7개 유지
            val filled = orderedPhotos + List(7 - orderedPhotos.size) { null }

            // 3️⃣ RecyclerView 에 반영
            photoAdapter.submitList(filled)

            // 하단 grid(도장판)는 날짜 관계없이 업로드된 리스트만 유지
            val completedList = photoListWithNulls.filterNotNull()
            stampGridAdapter.submitList(completedList)
        }

        // 월간 운동 횟수 LiveData를 관찰하여 TextView 업데이트
        viewModel.monthlyWorkoutCount.observe(viewLifecycleOwner) { count ->
//            binding.exerciseCountTextView.text = "${count}일째 운동 중"
            binding.exerciseCountTextView.text = "4일째 운동 중"

        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupPhotoRecyclerView() {
        photoAdapter = PhotoAdapter()
        binding.photoRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                context, LinearLayoutManager.HORIZONTAL, false
            )
            adapter = photoAdapter
        }
    }

    private fun setupExerciseNowRecyclerView() {
        stampGridAdapter = StampGridAdapter()
        binding.exerciseNowRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 4)
            adapter = stampGridAdapter
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER

            // 기존 ItemDecoration 로직을 유지하여 간격을 설정할 수 있습니다.
            if (itemDecorationCount > 0) {
                removeItemDecorationAt(0)
            }
            // 필요에 따라 간격(spacing) 값을 조절하세요.
            val spacingPx = 8.dpToPx(requireContext())
            addItemDecoration(GridSpacingItemDecoration(4, spacingPx, includeEdge = true, applyVerticalSpacing = true))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun Int.dpToPx(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()

    // RecyclerView 그리드 아이템 간격 균등 분배
    // 생성자에 applyVerticalSpacing 추가, getItemOffsets에서 세로 간격 처리 수정
    class GridSpacingItemDecoration(
        private val spanCount: Int,
        private val spacing: Int,
        private val includeEdge: Boolean,
        private val applyVerticalSpacing: Boolean = true // 세로 간격 적용 여부 플래그
    ) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION) return
            val column = position % spanCount

            // 가로 간격 설정
            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount
                outRect.right = (column + 1) * spacing / spanCount
            } else {
                outRect.left = column * spacing / spanCount
                outRect.right = spacing - (column + 1) * spacing / spanCount
            }

            // 세로 간격 설정 (플래그에 따라 또는 아이템 XML에서 처리)
            if (applyVerticalSpacing) {
                if (includeEdge) {
                    if (position < spanCount) outRect.top = spacing // 첫 번째 행 상단
                    outRect.bottom = spacing // 모든 아이템 하단
                } else {
                    if (position >= spanCount) outRect.top = spacing // 첫 번째 행이 아닌 아이템의 상단
                    outRect.bottom = 0 // 가장자리 미포함 시 하단 간격은 0 (또는 필요에 따라 spacing)
                }
            } else {
                outRect.top = 0
                outRect.bottom = 0 // 아이템 XML에서 paddingBottom으로 처리하므로 여기서는 0
            }
        }
    }

    private class StampGridAdapter : ListAdapter<PhotoChallengeItem, StampGridAdapter.ViewHolder>(PhotoDiffCallback()) {
        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val dateTextView: TextView = itemView.findViewById(R.id.tv_date)
            fun bind(item: PhotoChallengeItem) {
                try {
                    val date = org.threeten.bp.LocalDate.parse(item.date)
                    val formatter = org.threeten.bp.format.DateTimeFormatter.ofPattern("M/d")
                    dateTextView.text = date.format(formatter)
                } catch (e: Exception) {
                    dateTextView.text = ""
                }
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_challenge_count, parent, false)
            return ViewHolder(view)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private class PhotoAdapter : ListAdapter<PhotoChallengeItem, PhotoAdapter.PhotoViewHolder>(PhotoDiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_challenge_photo, parent, false)
            return PhotoViewHolder(view)
        }

        override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
            holder.bind(getItem(position), position)
        }

        // ★ ListAdapter는 자체적으로 getItemCount를 관리하므로 오버라이드할 필요가 없습니다.
        // override fun getItemCount(): Int = 7

        class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageView: ImageView = itemView.findViewById(R.id.ivMainBackground)
            // item_challenge_photo.xml에 요일 표시용 TextView가 있다면 아래 코드 사용
            // private val dayTextView: TextView = itemView.findViewById(R.id.tvDayOfWeek)

            fun bind(item: PhotoChallengeItem?, position: Int) {
                // val dayNames = listOf("일", "월", "화", "수", "목", "금", "토")
                // dayTextView.text = dayNames[position]

                if (item != null) {
                    // ★ 해결 2: RetrofitClient.BASE_URL 대신 companion object에 정의된 BASE_URL 사용
                    //val fullUrl = BASE_URL.removeSuffix("/") + item.imageUrl
                    Glide.with(itemView.context)
                        .load(item.imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_fitbuddy_logo) // 기본 이미지 리소스
                        .error(R.drawable.ic_fitbuddy_logo) // 에러 시 이미지 리소스
                        .into(imageView)
                    itemView.alpha = 1.0f
                } else {
                    // 인증 사진이 없는 경우
                    imageView.setImageResource(R.drawable.ic_fitbuddy_logo) // 사진 추가 유도 이미지
                    itemView.alpha = 0.5f // 비활성화된 느낌
                }
            }
        }
    }

    private class PhotoDiffCallback : DiffUtil.ItemCallback<PhotoChallengeItem>() {
        override fun areItemsTheSame(oldItem: PhotoChallengeItem, newItem: PhotoChallengeItem): Boolean {
            return oldItem.date == newItem.date
        }
        override fun areContentsTheSame(oldItem: PhotoChallengeItem, newItem: PhotoChallengeItem): Boolean {
            return oldItem == newItem
        }
    }

    private data class TempUserItem(val name: String, val progress: String, val avatarResId: Int)
    private class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatar: ShapeableImageView = itemView.findViewById(R.id.sivAvatar)
        private val nameTv: TextView = itemView.findViewById(R.id.userNameTextView)
        private val progTv: TextView = itemView.findViewById(R.id.exerciseProgressTextView)
        fun bind(item: TempUserItem?) {
            if (item == null) {
                itemView.visibility = View.INVISIBLE
            } else {
                itemView.visibility = View.VISIBLE
                nameTv.text = item.name
                progTv.text = item.progress
                avatar.setImageResource(item.avatarResId)
                itemView.setOnClickListener {
                    Toast.makeText(itemView.context, "${item.name} 운동 중 클릭", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private class UserAdapter(private val items: List<TempUserItem?>) :
        RecyclerView.Adapter<UserViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = UserViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_challenge_count, parent, false)
        )
        override fun onBindViewHolder(holder: UserViewHolder, pos: Int) = holder.bind(items[pos])
        override fun getItemCount() = items.size
    }
}
