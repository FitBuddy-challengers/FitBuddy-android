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
        setupChallengeUpdateListener(userId)
    }

    // 새로 추가된 부분 → 탭 재진입 시 항상 최신 정보로 갱신
    override fun onResume() {
        super.onResume()
        val userId = UserPreference(requireContext()).getUserId()
        if (userId != -1) {
            loadUserChallengeProgress(userId)
        }
    }

    private fun setupChallengeUpdateListener(userId: Int) {
        // '운동 완료 프래그먼트' 등에서 챌린지 상태 갱신이 필요하다고 알리는 결과를 수신합니다.
        parentFragmentManager.setFragmentResultListener("CHALLENGE_STATUS_UPDATED", viewLifecycleOwner) { _, bundle ->
            val needsRefresh = bundle.getBoolean("NEEDS_REFRESH", false)
            if (needsRefresh && userId != -1) {
                Log.d("ChallengeFragment", "🔄 운동 완료 이벤트 수신. 챌린지 데이터 갱신 시작.")
                loadUserChallengeProgress(userId)
            }
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
                Log.d("ChallengeFragment", "📡 요청 → /api/user-challenge-progress/$userId")

                // 1) 진행도 API 호출
                val progressResponse = RetrofitClient.challengeApi.getUserChallengeProgress(userId)

                if (!progressResponse.isSuccessful || progressResponse.body() == null) {
                    updateUiOnFailure()
                    return@launch
                }

                val response = progressResponse.body()!!   // ← 반드시 body() 사용

                // 2) 현재 레벨 조회
                val currentLevel = response.level ?: 1

                // 3) 레벨 정보 API 호출 (보상 정보)
                val levelResponse = RetrofitClient.challengeApi.getAllLevels()

                if (!levelResponse.isSuccessful || levelResponse.body() == null) {
                    updateUiOnFailure()
                    return@launch
                }

                val levelDataList = levelResponse.body()!!

                val currentLevelRewards =
                    levelDataList.find { it.level == currentLevel }

                val rewardAtt = currentLevelRewards?.rewardAttendance ?: 0
                val rewardExe = currentLevelRewards?.rewardExercise ?: 0
                val rewardPho = currentLevelRewards?.rewardPhoto ?: 0

                // 4) UI 세팅
                binding.userNameTextView.text = response.nickname ?: "사용자"
                binding.userLevelTextView.text = "Lv.$currentLevel"
                binding.userCoinTextView.text = response.coin?.toString() ?: "0"

                // 5) 리스트 구성
                val challengeList = listOf(
                    RetrofitClient.ChallengeItemUiModel(
                        title = "출석 ${response.current?.attendance ?: 0}회 / ${response.required?.attendance ?: 0}회",
                        progressPercent = calculatePercent(response.current?.attendance ?: 0, response.required?.attendance ?: 0),
                        reward = rewardAtt,
                        type = "attendance"
                    ),
                    RetrofitClient.ChallengeItemUiModel(
                        title = "운동 횟수 ${response.current?.exercise ?: 0}회 / ${response.required?.exercise ?: 0}회",
                        progressPercent = calculatePercent(response.current?.exercise ?: 0, response.required?.exercise ?: 0),
                        reward = rewardExe,
                        type = "exercise"
                    ),
                    RetrofitClient.ChallengeItemUiModel(
                        title = "사진 인증 ${response.current?.photo ?: 0}회 / ${response.required?.photo ?: 0}회",
                        progressPercent = calculatePercent(response.current?.photo ?: 0, response.required?.photo ?: 0),
                        reward = rewardPho,
                        type = "photo"
                    )
                )

                challengeAdapter.submitList(challengeList)

            } catch (e: Exception) {
                Log.e("ChallengeFragment", "❌ 오류: ${e.message}", e)
                updateUiOnFailure()
            }
        }
    }

//    private fun loadUserChallengeProgress(userId: Int) {
//        lifecycleScope.launch {
//            try {
//                Log.d("ChallengeFragment", "📡 요청 보냄 → /api/user-challenge-progress/$userId")
//
//                // 1. 현재 챌린지 진행 상태 로드
//                val progressResponse = RetrofitClient.challengeApi.getUserChallengeProgress(userId)
//
//                // API 호출 실패 또는 본문 null 처리 (Null-Safe 코드는 그대로 유지)
//                // (Retrofit Response<T> 타입 대신 DTO를 직접 반환한다고 가정하고 코드를 수정합니다.)
//                val response = progressResponse // DTO 자체라고 가정
//                val currentLevel = response.level ?: 1 // 현재 레벨 획득
//
//                // 2. 🚨 우회 로직: 전체 레벨 목록을 로드하여 현재 레벨의 보상 정보를 찾음
//                val levelDataList = RetrofitClient.challengeApi.getAllLevels()
//                val currentLevelRewards = levelDataList.find { it.level == currentLevel }
//
//                // 3. UI 바인딩 및 데이터 모델 생성
//                binding.userNameTextView.text = response.nickname ?: "사용자"
//                binding.userLevelTextView.text = "Lv.${currentLevel}"
//                binding.userCoinTextView.text = response.coin?.toString() ?: "0"
//
//                // 획득한 보상 금액을 사용하거나, 찾지 못하면 0을 사용 (Null-Safe)
//                // DTO 필드명은 ChallengeLevelDto에 정의된 대로 사용합니다.
//                val rewardAtt = currentLevelRewards?.rewardAttendance ?: 0
//                val rewardExe = currentLevelRewards?.rewardExercise ?: 0
//                val rewardPho = currentLevelRewards?.rewardPhoto ?: 0
//
//                // DTO 필드명 매핑에 따라 'rewardAttendance' 필드를 사용한다고 가정
//                // (ChallengeLevelDto의 필드가 rewardAttendance, rewardExercise, rewardPhoto 라고 가정)
//
//
//                val challengeList = listOf(
//                    RetrofitClient.ChallengeItemUiModel(
//                        title = "출석 ${response.current?.attendance ?: 0}회 / ${response.required?.attendance ?: 0}회",
//                        progressPercent = calculatePercent(response.current?.attendance ?: 0, response.required?.attendance ?: 0),
//                        reward = rewardAtt, // ★ 획득한 보상 금액 사용
//                        type = "attendance"
//                    ),
//                    RetrofitClient.ChallengeItemUiModel(
//                        title = "운동 횟수 ${response.current?.exercise ?: 0}회 / ${response.required?.exercise ?: 0}회",
//                        progressPercent = calculatePercent(response.current?.exercise ?: 0, response.required?.exercise ?: 0),
//                        reward = rewardExe, // ★ 획득한 보상 금액 사용
//                        type = "exercise"
//                    ),
//                    RetrofitClient.ChallengeItemUiModel(
//                        title = "사진 인증 ${response.current?.photo ?: 0}회 / ${response.required?.photo ?: 0}회",
//                        progressPercent = calculatePercent(response.current?.photo ?: 0, response.required?.photo ?: 0),
//                        reward = rewardPho, // ★ 획득한 보상 금액 사용
//                        type = "photo"
//                    )
//                )
//                challengeAdapter.submitList(challengeList)
//
//                Log.d("ChallengeFragment", "✅ API 응답 nickname=${response.nickname}, level=${response.level}, coin=${response.coin}")
//            } catch (e: Exception) {
//                // 네트워크 오류, HTTP 오류 등 예외 발생 시 처리
//                Log.e("ChallengeFragment", "❌ 챌린지 데이터 로드 실패: ${e.message}", e)
//                if (isAdded) {
//                    Toast.makeText(requireContext(), "데이터 로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
//                }
//                // 실패 시 UI를 기본값으로 설정하는 보조 함수 호출
//                updateUiOnFailure()
//            }
//        }
//    }

    private fun updateUiOnFailure() {
        if (!isAdded || _binding == null) return
        // 데이터 로드 실패 시 UI를 기본값으로 설정하여 오류를 방지합니다.
        binding.userNameTextView.text = "사용자"
        binding.userLevelTextView.text = "Lv.1"
        binding.userCoinTextView.text = "0"

        // 챌린지 리스트를 빈 목록으로 업데이트하여 RecyclerView 오류 방지
        challengeAdapter.submitList(emptyList())
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