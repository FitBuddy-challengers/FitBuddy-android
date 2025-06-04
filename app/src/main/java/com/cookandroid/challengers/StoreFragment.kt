package com.cookandroid.challengers

import android.os.Bundle
import android.transition.Transition // TransitionListener 사용을 위해 import
import android.transition.TransitionManager
import android.transition.AutoTransition // AutoTransition 사용 (또는 원하는 다른 Transition)
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer

// 캐릭터&아이템 레이어
private const val ELEVATION_BEHIND_CHARACTER = 1f // 캐릭터 뒤 액세서리 (날개)
private const val ELEVATION_CHARACTER = 2f
private const val ELEVATION_CLOTHING_BOTTOM = 3f //  바지
private const val ELEVATION_CLOTHING_TOP = 4f    //  상의
private const val ELEVATION_ONEPIECE = 5f      //  원피스 (상의/하의보다 위)
private const val ELEVATION_COUTUME = 6f      //  코스튬
private const val ELEVATION_GLASSES = 7f      //  안경
private const val ELEVATION_HAIR_ACC = 8f      //  헤어 액세서리
private const val ELEVATION_ACCESSORY_FRONT = 9f // 캐릭터 앞 액세서리 (EX.마법봉)

class StoreFragment : Fragment(), StoreOpenFragment.StoreBottomSheetDismissListener {

    private lateinit var btnOpenStore: Button
    private lateinit var placeholderView: View
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var storeViewModel: StoreViewModel
    private lateinit var characterImageView: ImageView
    private lateinit var topItemImageView: ImageView
    private lateinit var pantsItemImageView: ImageView
    private lateinit var onepieceItemImageView: ImageView
    private lateinit var accItemImageView: ImageView
    private lateinit var hairAccItemImageView: ImageView
    private lateinit var glassesItemImageView: ImageView
    private lateinit var costumeItemImageView: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_store, container, false)
        btnOpenStore = view.findViewById(R.id.btnOpenStore)
        placeholderView = view.findViewById(R.id.view)
        rootLayout = view.findViewById(R.id.store_fragment_root)
        characterImageView = view.findViewById(R.id.baseCharacterImageView)
        topItemImageView = view.findViewById(R.id.topItemImageView)
        pantsItemImageView = view.findViewById(R.id.pantsItemImageView)
        onepieceItemImageView = view.findViewById(R.id.onepieceItemImageView)
        accItemImageView = view.findViewById(R.id.accItemImageView)
        hairAccItemImageView = view.findViewById(R.id.hairAccItemImageView)
        glassesItemImageView = view.findViewById(R.id.glassesItemImageView)
        costumeItemImageView = view.findViewById(R.id.costumeItemImageView)

        // ViewModel 인스턴스 가져오기
        storeViewModel = ViewModelProvider(requireActivity()).get(StoreViewModel::class.java)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        placeholderView.visibility = View.GONE

        // 기본 Elevation 설정
        characterImageView.elevation = ELEVATION_CHARACTER
        pantsItemImageView.elevation = ELEVATION_CLOTHING_BOTTOM
        topItemImageView.elevation = ELEVATION_CLOTHING_TOP
        onepieceItemImageView.elevation = ELEVATION_ONEPIECE
        accItemImageView.elevation = ELEVATION_ACCESSORY_FRONT
        hairAccItemImageView.elevation = ELEVATION_HAIR_ACC
        glassesItemImageView.elevation = ELEVATION_GLASSES
        costumeItemImageView.elevation = ELEVATION_COUTUME

        btnOpenStore.setOnClickListener {
            openStoreBottomSheet()
        }
        observeEquippedItems() // 착용 아이템 관찰 시작
    }


    private fun observeEquippedItems() {
        storeViewModel.equippedItems.observe(viewLifecycleOwner, Observer { equippedMap ->
            // equippedMap에 따라 characterImageView 업데이트
            updateCharacterDisplay(equippedMap)
        })
    }

    private fun updateCharacterDisplay(equippedItems: Map<String, ProductItem?>) {
        // 1. 기본 캐릭터 이미지 및 Elevation 설정
        val baseCharacterItem = equippedItems["character"]
        characterImageView.setImageResource(
            baseCharacterItem?.imageResId ?: R.drawable.char_graycat
        )
        characterImageView.elevation = ELEVATION_CHARACTER

        //토끼 아이템 y오프셋 조정 : 44f
        if (baseCharacterItem?.id == "char_2") {
            val rabbitOffsetY = -44f
            characterImageView.translationY = rabbitOffsetY
        } else {
            // 다른 캐릭터는 기본 Y위치 (오프셋 없음)
            characterImageView.translationY = 0f
        }

        // 아이템 가져오기
        val onepieceItem = equippedItems["onepiece"]
        val topItem = equippedItems["top"]
        val pantsItem = equippedItems["pants"]
        val accItem = equippedItems["acc"]
        val hairAccItem = equippedItems["hairAcc"]
        val glassesItem = equippedItems["glasses"]
        val costumeItem = equippedItems["costume"]

        // 원피스 vs 상/하의 처리 및 Elevation 설정
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

        // 액세서리 처리 (단일 accItemImageView 사용)
        if (accItem != null) {
            accItemImageView.setImageResource(accItem.imageResId)
            accItemImageView.visibility = View.VISIBLE
            if (accItem.id == "acc_1") { // "천사 날개" ID가 "acc_1"이라고 가정
                // 천사 날개는 캐릭터 뒤로 (캐릭터보다 낮은 elevation)
                accItemImageView.elevation = ELEVATION_BEHIND_CHARACTER
            } else {
                accItemImageView.elevation = ELEVATION_ACCESSORY_FRONT
            }
        } else {
            // 착용한 액세서리가 없으면 숨김
            accItemImageView.visibility = View.GONE
        }

        // 헤어 액세서리
        if(hairAccItem != null){
            hairAccItemImageView.setImageResource(hairAccItem.imageResId)
            hairAccItemImageView.visibility = View.VISIBLE
            hairAccItemImageView.elevation = ELEVATION_HAIR_ACC
        }else{
            hairAccItemImageView.visibility = View.GONE
        }

        // 안경
        if(glassesItem != null){
            glassesItemImageView.setImageResource(glassesItem.imageResId)
            glassesItemImageView.visibility = View.VISIBLE
            glassesItemImageView.elevation = ELEVATION_GLASSES
        }else{
            glassesItemImageView.visibility = View.GONE
        }

        // 코스튬
        if(costumeItem != null){
            costumeItemImageView.setImageResource(costumeItem.imageResId)
            costumeItemImageView.visibility = View.VISIBLE
            costumeItemImageView.elevation = ELEVATION_COUTUME
        }else{
            costumeItemImageView.visibility = View.GONE
        }
    }

    private fun openStoreBottomSheet() {
        // 1. placeholderView 애니메이션 시작 준비
        val placeholderTransition = AutoTransition()
        placeholderTransition.duration = 300 // 애니메이션 효과를 원하면 지속 시간 유지
        TransitionManager.beginDelayedTransition(rootLayout, placeholderTransition)
        placeholderView.visibility = View.VISIBLE

        // 2. 바텀시트 표시 (애니메이션 시작과 거의 동시에)
        // Handler나 짧은 delay를 사용하여 TransitionManager가 적용될 시간을 아주 약간 줄 수도 있지만,
        // 대부분의 경우 거의 동시에 실행해도 괜찮습니다.
        val storeOpenFragment = StoreOpenFragment.newInstance()
        storeOpenFragment.setStoreBottomSheetDismissListener(this@StoreFragment)
        storeOpenFragment.show(parentFragmentManager, "StoreOpenFragmentTag")
    }

    override fun onBottomSheetDismissed() {
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