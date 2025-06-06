package com.cookandroid.challengers

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton // Button -> ImageButton으로 수정
import android.widget.ImageView
import android.widget.TextView // TextView 임포트 추가
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.util.UserPreference // UserPreference 임포트
import kotlinx.coroutines.launch

// 캐릭터&아이템 레이어 상수
private const val ELEVATION_BEHIND_CHARACTER = 1f
private const val ELEVATION_CHARACTER = 2f
private const val ELEVATION_CLOTHING_BOTTOM = 3f
private const val ELEVATION_CLOTHING_TOP = 4f
private const val ELEVATION_ONEPIECE = 5f
private const val ELEVATION_COSTUME = 5.5f
private const val ELEVATION_GLASSES = 6f
private const val ELEVATION_HAIR_ACC = 6.1f
private const val ELEVATION_ACCESSORY_FRONT = 6.2f


class StoreFragment : Fragment(), StoreOpenFragment.StoreBottomSheetDismissListener {

    private lateinit var btnOpenStore: ImageButton // ★ 타입 ImageButton으로 변경
    private lateinit var placeholderView: View
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var storeViewModel: StoreViewModel
    private lateinit var characterImageView: ImageView
    private lateinit var topItemImageView: ImageView
    private lateinit var pantsItemImageView: ImageView
    private lateinit var onepieceItemImageView: ImageView
    private lateinit var accItemImageView: ImageView
    private lateinit var costumeItemImageView: ImageView
    private lateinit var hairAccItemImageView: ImageView
    private lateinit var glassesItemImageView: ImageView
    // ★ 레벨과 코인을 표시할 TextView 추가
    private lateinit var userLevelTextView: TextView
    private lateinit var userCoinTextView: TextView
    private lateinit var userPreference: UserPreference // ★ UserPreference 추가

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_store, container, false)
        // 뷰 초기화
        btnOpenStore = view.findViewById(R.id.btnOpenStore)
        placeholderView = view.findViewById(R.id.view)
        rootLayout = view.findViewById(R.id.store_fragment_root)
        characterImageView = view.findViewById(R.id.baseCharacterImageView)
        topItemImageView = view.findViewById(R.id.topItemImageView)
        pantsItemImageView = view.findViewById(R.id.pantsItemImageView)
        onepieceItemImageView = view.findViewById(R.id.onepieceItemImageView)
        accItemImageView = view.findViewById(R.id.accItemImageView)
        costumeItemImageView = view.findViewById(R.id.costumeItemImageView)
        hairAccItemImageView = view.findViewById(R.id.hairAccItemImageView)
        glassesItemImageView = view.findViewById(R.id.glassesItemImageView)
        // ★ 레벨/코인 TextView 초기화
        userLevelTextView = view.findViewById(R.id.userLevelTextView)
        userCoinTextView = view.findViewById(R.id.userCoinTextView)

        // ViewModel 및 UserPreference 인스턴스 가져오기
        storeViewModel = ViewModelProvider(requireActivity()).get(StoreViewModel::class.java)
        userPreference = UserPreference(requireContext())
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        placeholderView.visibility = View.GONE

        // Elevation 설정은 updateCharacterDisplay에서 동적으로 처리
        btnOpenStore.setOnClickListener {
            openStoreBottomSheet()
        }
        observeEquippedItems()
        loadUserInfo() // ★ 사용자 정보 로드 함수 호출
    }

    override fun onResume() {
        super.onResume()
        // 이 화면으로 돌아올 때마다 사용자 정보(특히 코인)를 새로고침
        loadUserInfo()
    }

    // ★★★ 사용자 레벨 및 코인 정보 로드 함수 추가 ★★★
    private fun loadUserInfo() {
        val userId = userPreference.getUserId()
        if (userId == -1) {
            Log.w("StoreFragment", "유효한 userId가 없어 사용자 정보를 로드할 수 없습니다.")
            // 필요하다면 로그인 화면으로 보내는 로직 추가
            return
        }

        lifecycleScope.launch {
            try {
                // RetrofitClient.challengeApi에 getUserChallengeProgress가 suspend 함수로 정의되어 있다고 가정
                val response = RetrofitClient.challengeApi.getUserChallengeProgress(userId)

                // isAdded 또는 _binding != null 체크는 코루틴 내 UI 업데이트 시 안전성을 위해 필요하지만
                // 이 프래그먼트는 ViewBinding을 사용하지 않으므로, isAdded로 확인합니다.
                if (isAdded) {
                    userLevelTextView.text = "Lv.${response.level}"
                    userCoinTextView.text = response.coin.toString()
                    Log.d("StoreFragment", "사용자 정보 로드 성공: Level=${response.level}, Coin=${response.coin}")
                }

            } catch (e: Exception) {
                Log.e("StoreFragment", "사용자 정보 로드 실패", e)
                if (isAdded) {
                    Toast.makeText(context, "사용자 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observeEquippedItems() {
        storeViewModel.equippedItems.observe(viewLifecycleOwner, Observer { equippedMap ->
            if (isAdded) { // View가 유효할 때만 업데이트
                updateCharacterDisplay(equippedMap)
            }
        })
    }

    private fun updateCharacterDisplay(equippedItems: Map<String, ProductItem?>) {

        // 1. 기본 캐릭터 이미지 및 Elevation 설정
        val baseCharacterItem = equippedItems["character"]
        characterImageView.setImageResource(
            baseCharacterItem?.imageResId ?: R.drawable.char_graycat
        )
        characterImageView.elevation = ELEVATION_CHARACTER

        if (baseCharacterItem?.id == "char_2") {
            val rabbitOffsetY = -44f
            characterImageView.translationY = rabbitOffsetY
        } else {
            characterImageView.translationY = 0f
        }

        // 아이템 가져오기
        val costumeItem = equippedItems["costume"]
        val onepieceItem = equippedItems["onepiece"]
        val topItem = equippedItems["top"]
        val pantsItem = equippedItems["pants"]
        val accItem = equippedItems["acc"]
        val hairAccItem = equippedItems["hairAcc"]
        val glassesItem = equippedItems["glasses"]

        // 2. 코스튬 처리 (가장 우선순위 높음)
        if (costumeItem != null) {
            costumeItemImageView.setImageResource(costumeItem.imageResId)
            costumeItemImageView.visibility = View.VISIBLE
            costumeItemImageView.elevation = ELEVATION_COSTUME

            // 코스튬 착용 시 상의, 하의, 원피스 숨김
            topItemImageView.visibility = View.GONE
            pantsItemImageView.visibility = View.GONE
            onepieceItemImageView.visibility = View.GONE
        } else {
            costumeItemImageView.visibility = View.GONE

            // 코스튬 미착용 시에만 원피스 vs 상/하의 로직 실행
            if (onepieceItem != null) {
                onepieceItemImageView.setImageResource(onepieceItem.imageResId)
                onepieceItemImageView.visibility = View.VISIBLE
                onepieceItemImageView.elevation = ELEVATION_ONEPIECE
                topItemImageView.visibility = View.GONE
                pantsItemImageView.visibility = View.GONE
            } else {
                onepieceItemImageView.visibility = View.GONE
                if (topItem != null) {
                    topItemImageView.setImageResource(topItem.imageResId)
                    topItemImageView.visibility = View.VISIBLE
                    topItemImageView.elevation = ELEVATION_CLOTHING_TOP
                } else {
                    topItemImageView.visibility = View.GONE
                }
                if (pantsItem != null) {
                    pantsItemImageView.setImageResource(pantsItem.imageResId)
                    pantsItemImageView.visibility = View.VISIBLE
                    pantsItemImageView.elevation = ELEVATION_CLOTHING_BOTTOM
                } else {
                    pantsItemImageView.visibility = View.GONE
                }
            }
        }

        // 3. 일반 액세서리 처리 (acc)
        if (accItem != null) {
            accItemImageView.setImageResource(accItem.imageResId)
            accItemImageView.visibility = View.VISIBLE
            if (accItem.id == "acc_1") {
                accItemImageView.elevation = ELEVATION_BEHIND_CHARACTER
            } else {
                accItemImageView.elevation = ELEVATION_ACCESSORY_FRONT
            }
        } else {
            accItemImageView.visibility = View.GONE
        }

        // 4. 헤어 액세서리
        if(hairAccItem != null){
            hairAccItemImageView.setImageResource(hairAccItem.imageResId)
            hairAccItemImageView.visibility = View.VISIBLE
            hairAccItemImageView.elevation = ELEVATION_HAIR_ACC
        } else {
            hairAccItemImageView.visibility = View.GONE
        }

        // 5. 안경
        if(glassesItem != null){
            glassesItemImageView.setImageResource(glassesItem.imageResId)
            glassesItemImageView.visibility = View.VISIBLE
            glassesItemImageView.elevation = ELEVATION_GLASSES
        } else {
            glassesItemImageView.visibility = View.GONE
        }
    }

    private fun openStoreBottomSheet() {
        val placeholderTransition = AutoTransition()
        placeholderTransition.duration = 300
        TransitionManager.beginDelayedTransition(rootLayout, placeholderTransition)
        placeholderView.visibility = View.VISIBLE

        val storeOpenFragment = StoreOpenFragment.newInstance()
        storeOpenFragment.setStoreBottomSheetDismissListener(this@StoreFragment)
        storeOpenFragment.show(parentFragmentManager, "StoreOpenFragmentTag")
    }

    override fun onBottomSheetDismissed() {
        if (!isAdded) return // 뷰가 이미 파괴된 경우
        val transition = AutoTransition()
        transition.duration = 300
        TransitionManager.beginDelayedTransition(rootLayout, transition)
        placeholderView.visibility = View.GONE
    }

    companion object {
        fun newInstance(): StoreFragment {
            return StoreFragment()
        }
    }
}
