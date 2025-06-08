package com.cookandroid.challengers

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.cookandroid.challengers.databinding.FragmentStoreBinding // ★ View Binding 임포트
import com.cookandroid.challengers.util.UserPreference

// ★ 캐릭터&아이템 레이어 상수 (변경 없음)
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

    // ★ View Binding으로 수정
    private var _binding: FragmentStoreBinding? = null
    private val binding get() = _binding!!

    private lateinit var storeViewModel: StoreViewModel
    private lateinit var userPreference: UserPreference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoreBinding.inflate(inflater, container, false)
        storeViewModel = ViewModelProvider(requireActivity()).get(StoreViewModel::class.java)
        userPreference = UserPreference(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.view.visibility = View.GONE // placeholderView

        binding.btnOpenStore.setOnClickListener {
            openStoreBottomSheet()
        }
        observeViewModel() // ★ ViewModel 관찰 시작
    }

    override fun onResume() {
        super.onResume()
        // 이 화면으로 돌아올 때마다 사용자 정보(레벨, 코인 등)를 새로고침
        val userId = userPreference.getUserId()
        if (userId != -1) {
            storeViewModel.loadUserData(userId)
        } else {
            Log.w("StoreFragment", "유효한 userId가 없어 사용자 정보를 로드할 수 없습니다.")
        }
    }

    private fun observeViewModel() {
        // 착용된 아이템 관찰
        storeViewModel.equippedItems.observe(viewLifecycleOwner, Observer { equippedMap ->
            updateCharacterDisplay(equippedMap)
        })

        // 사용자 레벨 관찰
        storeViewModel.userLevel.observe(viewLifecycleOwner, Observer { level ->
            binding.userLevelTextView.text = "Lv.$level"
        })

        // 사용자 코인 관찰
        storeViewModel.userCoin.observe(viewLifecycleOwner, Observer { coin ->
            binding.userCoinTextView.text = coin.toString()
        })
    }

    private fun updateCharacterDisplay(equippedItems: Map<String, ProductItem?>) {
        if (!isAdded || _binding == null) return

        // 기본 캐릭터 이미지 및 Elevation 설정
        val baseCharacterItem = equippedItems["character"]
        binding.baseCharacterImageView.setImageResource(
            baseCharacterItem?.imageResId ?: R.drawable.char_graycat
        )
        binding.baseCharacterImageView.elevation = ELEVATION_CHARACTER

        // 토끼 캐릭터 y오프셋 조정
        if (baseCharacterItem?.id == 2) {
            val rabbitOffsetY = -44f
            binding.baseCharacterImageView.translationY = rabbitOffsetY
        } else {
            binding.baseCharacterImageView.translationY = 0f
        }

        // 아이템 가져오기
        val costumeItem = equippedItems["costume"]
        val onepieceItem = equippedItems["onepiece"]
        val topItem = equippedItems["top"]
        val pantsItem = equippedItems["pants"]
        val accItem = equippedItems["acc"]
        val hairAccItem = equippedItems["hairAcc"]
        val glassesItem = equippedItems["glasses"]

        // 코스튬 (코스튬, 원피스, 상 하의)
        if (costumeItem != null) {
            binding.costumeItemImageView.setImageResource(costumeItem.imageResId)
            binding.costumeItemImageView.visibility = View.VISIBLE
            binding.costumeItemImageView.elevation = ELEVATION_COSTUME
            binding.topItemImageView.visibility = View.GONE
            binding.pantsItemImageView.visibility = View.GONE
            binding.onepieceItemImageView.visibility = View.GONE
        } else {
            binding.costumeItemImageView.visibility = View.GONE

            // 코스튬 미착용 시에만 원피스 vs 상/하의 로직 실행
            if (onepieceItem != null) {
                binding.onepieceItemImageView.setImageResource(onepieceItem.imageResId)
                binding.onepieceItemImageView.visibility = View.VISIBLE
                binding.onepieceItemImageView.elevation = ELEVATION_ONEPIECE
                binding.topItemImageView.visibility = View.GONE
                binding.pantsItemImageView.visibility = View.GONE
            } else {
                binding.onepieceItemImageView.visibility = View.GONE
                binding.topItemImageView.visibility = if (topItem != null) {
                    binding.topItemImageView.setImageResource(topItem.imageResId)
                    binding.topItemImageView.elevation = ELEVATION_CLOTHING_TOP
                    View.VISIBLE
                } else View.GONE
                binding.pantsItemImageView.visibility = if (pantsItem != null) {
                    binding.pantsItemImageView.setImageResource(pantsItem.imageResId)
                    binding.pantsItemImageView.elevation = ELEVATION_CLOTHING_BOTTOM
                    View.VISIBLE
                } else View.GONE
            }
        }

        // 일반 액세서리
        if (accItem != null) {
            binding.accItemImageView.setImageResource(accItem.imageResId)
            binding.accItemImageView.visibility = View.VISIBLE
            binding.accItemImageView.elevation = if (accItem.id == 1) ELEVATION_BEHIND_CHARACTER else ELEVATION_ACCESSORY_FRONT
        } else {
            binding.accItemImageView.visibility = View.GONE
        }

        // 헤어 액세서리
        binding.hairAccItemImageView.visibility = if (hairAccItem != null) {
            binding.hairAccItemImageView.setImageResource(hairAccItem.imageResId)
            binding.hairAccItemImageView.elevation = ELEVATION_HAIR_ACC
            View.VISIBLE
        } else View.GONE

        // 안경
        binding.glassesItemImageView.visibility = if (glassesItem != null) {
            binding.glassesItemImageView.setImageResource(glassesItem.imageResId)
            binding.glassesItemImageView.elevation = ELEVATION_GLASSES
            View.VISIBLE
        } else View.GONE
    }

    private fun openStoreBottomSheet() {
        val placeholderTransition = AutoTransition()
        placeholderTransition.duration = 300
        TransitionManager.beginDelayedTransition(binding.storeFragmentRoot, placeholderTransition)
        binding.view.visibility = View.VISIBLE

        val storeOpenFragment = StoreOpenFragment.newInstance()
        storeOpenFragment.setStoreBottomSheetDismissListener(this@StoreFragment)
        storeOpenFragment.show(parentFragmentManager, "StoreOpenFragmentTag")
    }

    override fun onBottomSheetDismissed() {
        if (!isAdded || _binding == null) return
        val transition = AutoTransition()
        transition.duration = 300
        TransitionManager.beginDelayedTransition(binding.storeFragmentRoot, transition)
        binding.view.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지
    }

    companion object {
        fun newInstance(): StoreFragment {
            return StoreFragment()
        }
    }
}
