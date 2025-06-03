package com.cookandroid.challengers

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.databinding.FragmentChallengePersonalBinding
import com.cookandroid.challengers.util.UserPreference
import kotlinx.coroutines.launch

class ChallengePersonalFragment : Fragment() {

    private var _binding: FragmentChallengePersonalBinding? = null
    private val binding get() = _binding!!


    private lateinit var challengeAdapter: ChallengePersonalAdapter // 어댑터 준비 필요

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChallengePersonalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        challengeAdapter = ChallengePersonalAdapter()
        binding.personalChallengeRecyclerView.adapter = challengeAdapter

        val userId = UserPreference(requireContext()).getUserId() // ✅ 저장된 로그인 사용자 ID
        if (userId != -1) {
            loadUserChallengeProgress(userId)
        }
    }

    // 새로 추가된 부분 → 탭 재진입 시 항상 최신 정보로 갱신
    override fun onResume() {
        super.onResume()
        val userId = UserPreference(requireContext()).getUserId()
        if (userId != -1) {
            loadUserChallengeProgress(userId)
        }
    }

    private fun loadUserChallengeProgress(userId: Int) {
        lifecycleScope.launch {
            try {
                Log.d("ChallengeFragment", "📡 요청 보냄 → /api/user-challenge-progress/$userId")
                val response = RetrofitClient.challengeApi.getUserChallengeProgress(userId)

                // 👤 사용자 정보 표시
                binding.userNameTextView.text = response.nickname
                binding.userLevelTextView.text = "Lv.${response.level}"
                binding.userCoinTextView.text = response.coin.toString()

                // 프로필 이미지 (기본 이미지로 설정됨)
                Glide.with(this@ChallengePersonalFragment)
                    //.load(response.profileImage)
                    //.placeholder(R.drawable.default_profile)
                    //.into(binding.userProfileImageView)
                    .load(R.drawable.default_profile)
                    .into(binding.userProfileImageView)

                // ✅ 퍼센트 직접 계산해서 전달
                val challengeList = listOf(
                    RetrofitClient.ChallengeItemUiModel(
                        title = "출석 ${response.current.attendance}회 / ${response.required.attendance}회",
                        progressPercent = calculatePercent(response.current.attendance, response.required.attendance),
                        reward = response.reward.attendance
                    ),
                    RetrofitClient.ChallengeItemUiModel(
                        title = "운동 횟수 ${response.current.exercise}회 / ${response.required.exercise}회",
                        progressPercent = calculatePercent(response.current.exercise, response.required.exercise),
                        reward = response.reward.exercise
                    ),
                    RetrofitClient.ChallengeItemUiModel(
                        title = "사진 인증 ${response.current.photo}회 / ${response.required.photo}회",
                        progressPercent = calculatePercent(response.current.photo, response.required.photo),
                        reward = response.reward.photo
                    )
                )

                challengeAdapter.submitList(challengeList)

                // 레벨업 버튼 조건 (선택적으로 사용 가능)
                val allCompleted = challengeList.all { it.progressPercent == 100 }
                //binding.levelUpButton.visibility = if (allCompleted) View.VISIBLE else View.GONE

                Log.d("ChallengeFragment", "✅ API 응답 nickname=${response.nickname}, level=${response.level}, coin=${response.coin}")
            } catch (e: Exception) {
                Log.e("ChallengeFragment", "❌ 챌린지 데이터 로드 실패: ${e.message}")
            }
        }
    }

    private fun calculatePercent(current: Int, required: Int): Int {
        return if (required > 0) (current * 100 / required).coerceAtMost(100) else 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
//
//import android.content.Context
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.ProgressBar
//import android.widget.TextView
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.cookandroid.challengers.data.ChallengePersonal
//import com.cookandroid.challengers.data.db.AppDatabase
//import androidx.recyclerview.widget.ListAdapter
//import androidx.recyclerview.widget.DiffUtil
//import com.cookandroid.challengers.data.ChallengePersonalDao
//
//class ChallengePersonalFragment : Fragment() {
//
//    private lateinit var recyclerView: RecyclerView
//    private lateinit var adapter: PersonalChallengeAdapter
//    private lateinit var userProfileImageView: ImageView
//    private lateinit var userNameTextView: TextView
//    private lateinit var userLevelTextView: TextView
//    private lateinit var userCoinTextView: TextView
//
//    private var db: AppDatabase? = null
//    private var challengePersonalDao: ChallengePersonalDao? = null
//
//    override fun onAttach(context: Context) {
//        super.onAttach(context)
//        db = AppDatabase.getDatabase(requireContext(), lifecycleScope)
//        challengePersonalDao = db?.challengePersonalDao()
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.fragment_challenge_personal, container, false)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        userProfileImageView = view.findViewById(R.id.userProfileImageView)
//        userNameTextView = view.findViewById(R.id.userNameTextView)
//        userLevelTextView = view.findViewById(R.id.userLevelTextView)
//        userCoinTextView = view.findViewById(R.id.userCoinTextView)
//
//        recyclerView = view.findViewById(R.id.personalChallengeRecyclerView)
//        recyclerView.layoutManager = LinearLayoutManager(requireContext())
//        adapter = PersonalChallengeAdapter()
//        recyclerView.adapter = adapter
//
//        // 사용자 정보 설정 (실제 데이터 연동 필요)
//        userNameTextView.text = "김슈니"
//        userLevelTextView.text = "Lv.8"
//        userCoinTextView.text = "2500"
//
//        // 데이터베이스에서 챌린지 목록 가져와서 어댑터에 연결 (LiveData 사용)
//        challengePersonalDao?.getAllChallenges()?.observe(viewLifecycleOwner) { challenges ->
//            adapter.submitList(challenges)
//        }
//
//        // 프로필 이미지 설정 (Glide 등의 라이브러리 사용 고려)
//        // Glide.with(requireContext()).load(userProfileImageUrl).into(userProfileImageView)
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        db = null
//        challengePersonalDao = null
//    }
//
//    // 내부 클래스로 어댑터 정의
//    inner class PersonalChallengeAdapter :
//        ListAdapter<ChallengePersonal, PersonalChallengeAdapter.ViewHolder>(PersonalChallengeDiffCallback()) {
//
//        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//            val challengeNameTextView: TextView = itemView.findViewById(R.id.challengeNameTextView)
//            val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
//            val progressTextView: TextView = itemView.findViewById(R.id.progressTextView)
//            val rewardCoinImageView: ImageView = itemView.findViewById(R.id.rewardCoinImageView)
//            val rewardCoinTextView: TextView = itemView.findViewById(R.id.rewardCoinTextView)
//        }
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//            val view = LayoutInflater.from(parent.context)
//                .inflate(R.layout.item_challenge_personal, parent, false)
//            return ViewHolder(view)
//        }
//
//        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//            val challenge = getItem(position)
//            holder.challengeNameTextView.text = challenge.name
//
//            if (challenge.targetCount != null) {
//                val progress = if (challenge.targetCount > 0) {
//                    (challenge.currentCount.toFloat() / challenge.targetCount * 100).toInt().coerceIn(0, 100)
//                } else {
//                    0
//                }
//                holder.progressBar.progress = progress
//                holder.progressTextView.text = "${progress}%"
//            } else {
//                // 횟수 기반이 아닌 챌린지의 경우 다른 방식으로 진행도 표시 또는 숨김
//                holder.progressBar.progress = 0
//                holder.progressTextView.text = ""
//            }
//            holder.rewardCoinTextView.text = challenge.coinReward.toString()
//        }
//    }
//
//    // 내부 클래스로 DiffUtil Callback 정의
//    class PersonalChallengeDiffCallback : DiffUtil.ItemCallback<ChallengePersonal>() {
//        override fun areItemsTheSame(oldItem: ChallengePersonal, newItem: ChallengePersonal): Boolean {
//            return oldItem.id == newItem.id // ID를 비교하도록 수정 (PK)
//        }
//
//        override fun areContentsTheSame(oldItem: ChallengePersonal, newItem: ChallengePersonal): Boolean {
//            return oldItem == newItem
//        }
//    }
//}