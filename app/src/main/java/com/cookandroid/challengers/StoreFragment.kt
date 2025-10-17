package com.cookandroid.challengers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.cookandroid.challengers.databinding.FragmentStoreBinding
import com.cookandroid.challengers.util.UserPreference

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
    private var _binding: FragmentStoreBinding? = null
    private val binding get() = _binding!!

    private lateinit var storeViewModel: StoreViewModel
    private lateinit var userPreference: UserPreference
    private lateinit var characterContainer: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoreBinding.inflate(inflater, container, false)
        storeViewModel = ViewModelProvider(requireActivity()).get(StoreViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userPreference = UserPreference(requireContext())
        characterContainer = binding.characterContainer
        binding.view.visibility = View.GONE
        binding.btnOpenStore.setOnClickListener {
            openStoreBottomSheet()
        }
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        val userId = userPreference.getUserId()
        if (userId != -1) {
            storeViewModel.loadUserData(userId)
        } else {
            Log.w("StoreFragment", "유효한 userId가 없어 사용자 정보를 로드할 수 없습니다.")
        }
    }

    private fun observeViewModel() {
        storeViewModel.equippedItems.observe(viewLifecycleOwner, Observer { equippedMap ->
            updateCharacterDisplay(equippedMap)
        })
        storeViewModel.userLevel.observe(viewLifecycleOwner, Observer { level ->
            binding.userLevelTextView.text = "Lv.$level"
        })
        storeViewModel.userCoin.observe(viewLifecycleOwner, Observer { coin ->
            binding.userCoinTextView.text = coin.toString()
        })
    }

    // 유선 10.18 상점 토끼캐릭터 위치
    private fun updateCharacterDisplay(equippedItems: Map<String, ProductItem?>) {
        if (!isAdded || _binding == null) return

        val baseCharacterItem = equippedItems["character"]
        val characterOffsetY = when (baseCharacterItem?.id) {
            2 -> -44f // 토끼 캐릭터(ID: 2)일 경우 44f만큼 위로 이동
            else -> 0f // 그 외에는 이동 없음
        }

        // 2. 기본 캐릭터 이미지에만 계산된 오프셋을 적용합니다.
        binding.baseCharacterImageView.setImageResource(baseCharacterItem?.imageResId ?: R.drawable.char_graycat)
        binding.baseCharacterImageView.elevation = ELEVATION_CHARACTER
        binding.baseCharacterImageView.translationY = characterOffsetY

        // 아이템들을 가져옵니다.
        val costumeItem = equippedItems["costume"]
        val onepieceItem = equippedItems["onepiece"]
        val topItem = equippedItems["top"]
        val pantsItem = equippedItems["pants"]
        val accItem = equippedItems["acc"]
        val hairAccItem = equippedItems["hairAcc"]
        val glassesItem = equippedItems["glasses"]

        // 3. 착용한 모든 아이템 이미지의 위치는 항상 0f로 고정합니다.
        if (costumeItem != null) {
            binding.costumeItemImageView.setImageResource(costumeItem.imageResId)
            binding.costumeItemImageView.visibility = View.VISIBLE
            binding.costumeItemImageView.elevation = ELEVATION_COSTUME
            binding.costumeItemImageView.translationY = 0f // 아이템 위치 고정
            binding.topItemImageView.visibility = View.GONE
            binding.pantsItemImageView.visibility = View.GONE
            binding.onepieceItemImageView.visibility = View.GONE
        } else {
            binding.costumeItemImageView.visibility = View.GONE
            if (onepieceItem != null) {
                binding.onepieceItemImageView.setImageResource(onepieceItem.imageResId)
                binding.onepieceItemImageView.visibility = View.VISIBLE
                binding.onepieceItemImageView.elevation = ELEVATION_ONEPIECE
                binding.onepieceItemImageView.translationY = 0f // 아이템 위치 고정
                binding.topItemImageView.visibility = View.GONE
                binding.pantsItemImageView.visibility = View.GONE
            } else {
                binding.onepieceItemImageView.visibility = View.GONE
                binding.topItemImageView.visibility = if (topItem != null) {
                    binding.topItemImageView.setImageResource(topItem.imageResId)
                    binding.topItemImageView.elevation = ELEVATION_CLOTHING_TOP
                    binding.topItemImageView.translationY = 0f // 아이템 위치 고정
                    View.VISIBLE
                } else View.GONE
                binding.pantsItemImageView.visibility = if (pantsItem != null) {
                    binding.pantsItemImageView.setImageResource(pantsItem.imageResId)
                    binding.pantsItemImageView.elevation = ELEVATION_CLOTHING_BOTTOM
                    binding.pantsItemImageView.translationY = 0f // 아이템 위치 고정
                    View.VISIBLE
                } else View.GONE
            }
        }

        binding.accItemImageView.visibility = if (accItem != null) {
            binding.accItemImageView.setImageResource(accItem.imageResId)
            binding.accItemImageView.elevation = if (accItem.id == 1) ELEVATION_BEHIND_CHARACTER else ELEVATION_ACCESSORY_FRONT
            binding.accItemImageView.translationY = 0f // 아이템 위치 고정
            View.VISIBLE
        } else View.GONE

        binding.hairAccItemImageView.visibility = if (hairAccItem != null) {
            binding.hairAccItemImageView.setImageResource(hairAccItem.imageResId)
            binding.hairAccItemImageView.elevation = ELEVATION_HAIR_ACC
            binding.hairAccItemImageView.translationY = 0f // 아이템 위치 고정
            View.VISIBLE
        } else View.GONE

        binding.glassesItemImageView.visibility = if (glassesItem != null) {
            binding.glassesItemImageView.setImageResource(glassesItem.imageResId)
            binding.glassesItemImageView.elevation = ELEVATION_GLASSES
            binding.glassesItemImageView.translationY = 0f // 아이템 위치 고정
            View.VISIBLE
        } else View.GONE

        characterContainer.post {
            val bitmap = createBitmapFromView(characterContainer)
            storeViewModel.updateCharacterBitmap(bitmap)
        }
    }

    private fun createBitmapFromView(view: View): Bitmap {
        // 비트맵 해상도를 높여 화질 개선
        val scale = 2.0f
        val width = (view.width * scale).toInt()
        val height = (view.height * scale).toInt()

        if (width <= 0 || height <= 0) {
            // 뷰의 크기가 0이하일 경우 빈 비트맵을 반환하거나 예외처리
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        view.draw(canvas)
        return bitmap
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
        _binding = null
    }

    companion object {
        fun newInstance(): StoreFragment {
            return StoreFragment()
        }
    }
}