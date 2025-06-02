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

class StoreFragment : Fragment(), StoreOpenFragment.StoreBottomSheetDismissListener {

    private lateinit var btnOpenStore: Button
    private lateinit var placeholderView: View
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var storeViewModel: StoreViewModel
    private lateinit var characterImageView: ImageView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_store, container, false)
        btnOpenStore = view.findViewById(R.id.btnOpenStore)
        placeholderView = view.findViewById(R.id.view)
        rootLayout = view.findViewById(R.id.store_fragment_root)
        characterImageView = view.findViewById(R.id.characterImageView) // ID 확인 필요

        // ViewModel 인스턴스 가져오기
        storeViewModel = ViewModelProvider(requireActivity()).get(StoreViewModel::class.java)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        placeholderView.visibility = View.GONE

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
        // 1. "character" 카테고리에서 기본 캐릭터 이미지를 가져와 설정
        val baseCharacterItem = equippedItems["character"] // "character"는 ViewModel에서 사용하는 ID와 일치해야 함
        if (baseCharacterItem != null) {
            characterImageView.setImageResource(baseCharacterItem.imageResId)
        } else {
            characterImageView.setImageResource(R.drawable.avartar_sample) // 기본 캐릭터 이미지
        }

        // 2. 나머지 카테고리 (상의, 바지, 안경 등) 아이템들을 캐릭터 위에 덧입힘
        // 이 부분은 실제 구현 시 복잡할 수 있습니다.
        // 방법 1: 여러 ImageView를 FrameLayout 위에 겹쳐서 표시
        // 방법 2: LayerDrawable을 동적으로 생성하여 하나의 ImageView에 설정
        // 방법 3: Custom View를 만들어 직접 Canvas에 그리기

        // 예시: FrameLayout에 ImageView들을 동적으로 추가/제거하거나 visibility 조절
        // characterContainer.removeAllViews() // 이전 아이템들 제거
        // characterContainer.addView(baseCharacterImageView) // 기본 캐릭터 ImageView 추가

        // equippedItems.forEach { (category, item) ->
        //     if (item != null && category != "character") {
        //         val itemImageView = ImageView(requireContext())
        //         itemImageView.setImageResource(item.imageResId)
        //         // itemImageView 레이아웃 파라미터 설정 (FrameLayout 내 위치 등)
        //         characterContainer.addView(itemImageView)
        //     }
        // }
        // 이 부분은 상세한 UI/UX 디자인에 따라 구현 방식이 달라집니다.
        // 현재는 characterImageView 하나에 기본 캐릭터만 설정하는 예시입니다.
        // 아이템 덧입히기는 추가적인 ImageView 레이어링 로직이 필요합니다.
        // 예를 들어 Glide나 Picasso 같은 라이브러리로 여러 이미지를 중첩 로드할 수도 있습니다.
    }

    private fun openStoreBottomSheet() {
        val transition = AutoTransition() // 사용할 트랜지션 정의
        transition.duration = 300 // 애니메이션 지속 시간 설정

        transition.addListener(object : Transition.TransitionListener {
            override fun onTransitionStart(transition: Transition) {}

            override fun onTransitionEnd(transition: Transition) {
                // 애니메이션이 끝나면 StoreOpenFragment를 표시
                val storeOpenFragment = StoreOpenFragment.newInstance()
                storeOpenFragment.setStoreBottomSheetDismissListener(this@StoreFragment)
                storeOpenFragment.show(parentFragmentManager, "StoreOpenFragmentTag")

                // 리스너를 제거하여 중복 호출 방지
                transition.removeListener(this)
            }

            override fun onTransitionCancel(transition: Transition) {
                transition.removeListener(this)
            }
            override fun onTransitionPause(transition: Transition) {}
            override fun onTransitionResume(transition: Transition) {}
        })
        TransitionManager.beginDelayedTransition(rootLayout, transition)
        placeholderView.visibility = View.VISIBLE
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