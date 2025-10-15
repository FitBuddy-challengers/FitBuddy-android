package com.cookandroid.challengers

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.databinding.FragmentChallengePersonalBinding
import com.cookandroid.challengers.util.UserPreference
import kotlinx.coroutines.launch

class ChallengePersonalFragment : Fragment() {

    private var _binding: FragmentChallengePersonalBinding? = null
    private val binding get() = _binding!!

    private lateinit var challengeAdapter: ChallengePersonalAdapter

    private lateinit var storeViewModel: StoreViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChallengePersonalBinding.inflate(inflater, container, false)
        // 공유 StoreViewModel 초기화
        storeViewModel = ViewModelProvider(requireActivity()).get(StoreViewModel::class.java)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        challengeAdapter = ChallengePersonalAdapter { clickedItem ->
            Log.d("ChallengeFragment", "Challenge item clicked: ${clickedItem.title}")
            // 100% 달성된 챌린지만 클릭에 반응 (어댑터에서 이미 필터링했지만, 여기서 한번 더 확인 가능)
            if (clickedItem.progressPercent >= 100) {
                val userId = UserPreference(requireContext()).getUserId()
                if (userId != -1) {
                    claimRewardAndRefresh(userId, clickedItem)
                }
            }
        }
        binding.personalChallengeRecyclerView.adapter = challengeAdapter
        observeViewModel() // ★ ViewModel 관찰 시작

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

    private fun observeViewModel() {
        // 공유 ViewModel의 characterBitmap LiveData를 관찰
        storeViewModel.characterBitmap.observe(viewLifecycleOwner) { bitmap ->
            if (!isAdded) return@observe // 프래그먼트가 화면에 없을 때 UI 조작 방지

            if (bitmap != null) {
                // 비트맵이 있으면 아바타 이미지로 설정
                Glide.with(this@ChallengePersonalFragment)
                    .load(bitmap)
                    .circleCrop()
                    .into(binding.userProfileImageView)
                Log.d("ChallengeFragment", "Profile image updated from StoreViewModel.")
            } else {
                // 비트맵이 없으면(초기 상태, 로딩 실패 등) 기본 샘플 이미지로 설정
                Glide.with(this@ChallengePersonalFragment)
                    .load(R.drawable.default_profile) // 기본 이미지 리소스
                    .circleCrop()
                    .into(binding.userProfileImageView)
                Log.d("ChallengeFragment", "Profile image set to default (bitmap from ViewModel is null).")
            }
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

                val challengeList = listOf(
                    RetrofitClient.ChallengeItemUiModel(
                        // ▼▼▼ null일 경우를 대비해 ?. 와 ?: 0 추가 ▼▼▼
                        title = "출석 ${response.current?.attendance ?: 0}회 / ${response.required?.attendance ?: 0}회",
                        progressPercent = calculatePercent(response.current?.attendance ?: 0, response.required?.attendance ?: 0),
                        reward = response.reward?.attendance ?: 0,
                        type = "attendance"
                    ),
                    RetrofitClient.ChallengeItemUiModel(
                        // ▼▼▼ null일 경우를 대비해 ?. 와 ?: 0 추가 ▼▼▼
                        title = "운동 횟수 ${response.current?.exercise ?: 0}회 / ${response.required?.exercise ?: 0}회",
                        progressPercent = calculatePercent(response.current?.exercise ?: 0, response.required?.exercise ?: 0),
                        reward = response.reward?.exercise ?: 0,
                        type = "exercise"
                    ),
                    RetrofitClient.ChallengeItemUiModel(
                        // ▼▼▼ null일 경우를 대비해 ?. 와 ?: 0 추가 ▼▼▼
                        title = "사진 인증 ${response.current?.photo ?: 0}회 / ${response.required?.photo ?: 0}회",
                        progressPercent = calculatePercent(response.current?.photo ?: 0, response.required?.photo ?: 0),
                        reward = response.reward?.photo ?: 0,
                        type = "photo"
                    )
                )
                challengeAdapter.submitList(challengeList)

                Log.d("ChallengeFragment", "✅ API 응답 nickname=${response.nickname}, level=${response.level}, coin=${response.coin}")
            } catch (e: Exception) {
                Log.e("ChallengeFragment", "❌ 챌린지 데이터 로드 실패: ${e.message}")
                if (isAdded) {
                    Toast.makeText(requireContext(), "데이터 로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

//    private fun loadUserChallengeProgress(userId: Int) {
//        lifecycleScope.launch {
//            try {
//                Log.d("ChallengeFragment", "📡 요청 보냄 → /api/user-challenge-progress/$userId")
//                val response = RetrofitClient.challengeApi.getUserChallengeProgress(userId)
//
//                // 👤 사용자 정보 표시
//                binding.userNameTextView.text = response.nickname
//                binding.userLevelTextView.text = "Lv.${response.level}"
//                binding.userCoinTextView.text = response.coin.toString()
//
//                val challengeList = listOf(
//                    RetrofitClient.ChallengeItemUiModel(
//                        title = "출석 ${response.current.attendance}회 / ${response.required.attendance}회",
//                        progressPercent = calculatePercent(response.current.attendance, response.required.attendance),
//                        reward = response.reward.attendance,
//                        type = "attendance"
//                    ),
//                    RetrofitClient.ChallengeItemUiModel(
//                        title = "운동 횟수 ${response.current.exercise}회 / ${response.required.exercise}회",
//                        progressPercent = calculatePercent(response.current.exercise, response.required.exercise),
//                        reward = response.reward.exercise,
//                        type = "exercise"
//                    ),
//                    RetrofitClient.ChallengeItemUiModel(
//                        title = "사진 인증 ${response.current.photo}회 / ${response.required.photo}회",
//                        progressPercent = calculatePercent(response.current.photo, response.required.photo),
//                        reward = response.reward.photo,
//                        type = "photo"
//                    )
//                )
//                challengeAdapter.submitList(challengeList)
//
//                Log.d("ChallengeFragment", "✅ API 응답 nickname=${response.nickname}, level=${response.level}, coin=${response.coin}")
//            } catch (e: Exception) {
//                Log.e("ChallengeFragment", "❌ 챌린지 데이터 로드 실패: ${e.message}")
//                if (isAdded) {
//                    Toast.makeText(requireContext(), "데이터 로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }

    // 실제 서버 API 호출 로직 추가
    private fun claimRewardAndRefresh(userId: Int, item: RetrofitClient.ChallengeItemUiModel) {
        Log.d("ChallengeFragment", "Claiming reward for challenge type: ${item.type}")
        lifecycleScope.launch {
            try {
                // 서버에 보상 요청 API 호출
                val request = RetrofitClient.ClaimRewardRequest(userId, item.type)
                val response = RetrofitClient.challengeApi.claimReward(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(context, "'${item.title}' 챌린지 보상을 획득했습니다!", Toast.LENGTH_SHORT).show()
                    // 보상 획득 성공 시, 최신 사용자 정보를 다시 불러와 화면을 갱신합니다.
                    loadUserChallengeProgress(userId)
                } else {
                    val errorMsg = response.body()?.message ?: "알 수 없는 오류"
                    Toast.makeText(context, "보상 획득 실패: $errorMsg", Toast.LENGTH_SHORT).show()
                    Log.e("ChallengeFragment", "❌ 보상 획득 실패: ${response.code()} - $errorMsg")
                }
            } catch (e: Exception) {
                Log.e("ChallengeFragment", "❌ 보상 획득 중 예외 발생: ${e.message}", e)
                if (isAdded) {
                    Toast.makeText(requireContext(), "보상 획득 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
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