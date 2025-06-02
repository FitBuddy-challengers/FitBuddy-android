package com.cookandroid.challengers

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf // bundleOf 사용
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cookandroid.challengers.databinding.FragmentChallengeGroupBinding
import com.google.android.material.imageview.ShapeableImageView

class ChallengeGroupFragment : Fragment() {
    private var _binding: FragmentChallengeGroupBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val ARG_GROUP_NAME = "groupNameKey" // 그룹 이름 표시용
        const val ARG_PAGE_GROUP_ID = "pageGroupIdKey" // 이 페이지의 그룹 ID
        const val ARG_IS_CURRENT_USER_LEADER = "isCurrentUserLeaderKey" // 현재 사용자가 이 그룹의 리더인지
        const val ARG_EXERCISE_TYPE = "argExerciseTypeKey" // 운동 종목을 받기 위한 키
        const val ARG_LEADER_NAME = "argLeaderNameKey"     // 그룹장 이름을 받기 위한 키

        private const val SPAN_COUNT = 4
        private const val GRID_SPACING_DP = 16
    }

    private var pageGroupId: String? = null
    private var isCurrentUserLeaderOfThisPage: Boolean = false
    private var receivedGroupName: String? = null // onCreateView에서 사용할 수 있도록 멤버 변수로 변경
    private var pageExerciseType: String? = null // 운동 종목 저장 변수
    private var pageLeaderName: String? = null   // 그룹장 이름 저장 변수

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            receivedGroupName = it.getString(ARG_GROUP_NAME) ?: "챌린지 그룹" // onCreateView 전에 값 설정
            pageGroupId = it.getString(ARG_PAGE_GROUP_ID)
            isCurrentUserLeaderOfThisPage = it.getBoolean(ARG_IS_CURRENT_USER_LEADER, false)
            pageExerciseType = it.getString(ARG_EXERCISE_TYPE) // ⭐ 운동 종목 수신
            pageLeaderName = it.getString(ARG_LEADER_NAME)     // ⭐ 그룹장 이름 수신

            Log.d("GroupPageFragment", "Received Data: GroupName=$receivedGroupName, GroupID=$pageGroupId, IsLeader=$isCurrentUserLeaderOfThisPage, Exercise=$pageExerciseType, Leader=$pageLeaderName")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChallengeGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPhotoRecyclerView()
        setupExerciseNowRecyclerView()

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

    private fun setupPhotoRecyclerView() {
        val photoItems = List(7) { TempPhotoItem("사진 ${it + 1}", R.drawable.avartar_sample) }
        binding.photoRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 1, GridLayoutManager.HORIZONTAL, false)
            adapter = PhotoAdapter(photoItems)
        }
    }

    private fun setupExerciseNowRecyclerView() {
        val rawUsers = List(7) {
            TempUserItem(
                name = "사용자 ${it + 1}",
                progress = "${it % 3 + 1}/${it % 4 + 5}",
                avatarResId = R.drawable.avartar_sample
            )
        }
        val remainder = rawUsers.size % SPAN_COUNT
        val placeholders = if (remainder == 0) 0 else SPAN_COUNT - remainder
        val items: List<TempUserItem?> = rawUsers + List(placeholders) { null }

        binding.exerciseNowRecyclerView.apply {
            layoutManager = GridLayoutManager(context, SPAN_COUNT)
            adapter = UserAdapter(items)
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            if (itemDecorationCount > 0) {
                removeItemDecorationAt(0)
            }
            val spacingPx = GRID_SPACING_DP.dpToPx(requireContext())
            // ⭐ GridSpacingItemDecoration의 세로 간격 관련 로직은 제거 또는 0으로 설정
            addItemDecoration(GridSpacingItemDecoration(SPAN_COUNT, spacingPx, includeEdge = false, applyVerticalSpacing = false))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun Int.dpToPx(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()

    // RecyclerView 그리드 아이템 간격 균등 분배
    // ⭐ 생성자에 applyVerticalSpacing 추가, getItemOffsets에서 세로 간격 처리 수정
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

    // --- Photo RecyclerView Adapter ---
    private data class TempPhotoItem(val title: String, val avatarResId: Int)
    private class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bg: ImageView = itemView.findViewById(R.id.ivMainBackground)
        private val avatar: ShapeableImageView = itemView.findViewById(R.id.sivAvatar)
        fun bind(item: TempPhotoItem, pos: Int) {
            val colors = listOf(
                android.R.color.holo_blue_light, android.R.color.holo_green_light,
                android.R.color.holo_orange_light, android.R.color.holo_purple
            )
            bg.setBackgroundColor(ContextCompat.getColor(itemView.context, colors[pos % colors.size]))
            avatar.setImageResource(item.avatarResId)
            itemView.setOnClickListener {
                Toast.makeText(itemView.context, "${item.title} 사진 클릭", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private class PhotoAdapter(private val items: List<TempPhotoItem>) :
        RecyclerView.Adapter<PhotoViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PhotoViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_challenge_photo, parent, false)
        )
        override fun onBindViewHolder(holder: PhotoViewHolder, pos: Int) = holder.bind(items[pos], pos)
        override fun getItemCount() = items.size
    }

    // --- ExerciseNow RecyclerView Adapter ---
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
