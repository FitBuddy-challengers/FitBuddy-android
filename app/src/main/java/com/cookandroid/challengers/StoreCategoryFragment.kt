package com.cookandroid.challengers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class StoreCategoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private lateinit var storeViewModel: StoreViewModel
    private var categoryId: String? = null // onCreateView에서 ARG_CATEGORY_ID로부터 설정됨

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryId = it.getString(ARG_CATEGORY_ID)
        }
        // ViewModel 인스턴스 가져오기 (Activity 범위)
        storeViewModel = ViewModelProvider(requireActivity()).get(StoreViewModel::class.java)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // ViewBinding 사용 시
        // _binding = FragmentStoreCategoryBinding.inflate(inflater, container, false)
        // return binding.root
        val view = inflater.inflate(R.layout.fragment_store_category, container, false) // ViewBinding 미사용 시
        recyclerView = view.findViewById(R.id.recycler_view_category_items)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView() // 어댑터 설정 먼저
        observeViewModel()  // ViewModel 관찰 시작

        view.post {
            loadCategoryItems()
        }
    }

    private fun setupRecyclerView() {
        // ProductAdapter 초기화 시 아이템 클릭 리스너 전달
        productAdapter = ProductAdapter(emptyList()) { clickedItem ->
            // 아이템 클릭 시 ViewModel 업데이트
            categoryId?.let { catId ->
                // 현재 선택된 아이템과 클릭된 아이템이 다를 경우에만 equip
                // 만약 같은 아이템을 다시 클릭해서 해제하는 로직을 원한다면 추가 구현
                if (storeViewModel.getEquippedItemForCategory(catId)?.id != clickedItem.id) {
                    storeViewModel.equipItem(catId, clickedItem)
                } else {
                    // 선택 사항: 같은 아이템을 다시 클릭하면 착용 해제
                    // storeViewModel.unequipItem(catId)
                }
            }
        }
        recyclerView.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = productAdapter
            setHasFixedSize(true) // <<< 이 라인 추가

        }
    }

    private fun observeViewModel() {
        categoryId?.let { catId ->
            storeViewModel.equippedItems.observe(viewLifecycleOwner, Observer { equippedMap ->
                val selectedItemForThisCategory = equippedMap[catId]
                productAdapter.setSelectedItem(selectedItemForThisCategory)
            })
        }
    }

    private fun loadCategoryItems() {
        val itemsForCategory = mutableListOf<ProductItem>()
        val currentCategory = categoryId ?: return // categoryId가 null이면 실행 중단

        when (currentCategory) {
            "character" -> {
                itemsForCategory.add(ProductItem("char_1", "회색 고양이", R.drawable.char_graycat, currentCategory))
                itemsForCategory.add(ProductItem("char_2", "토끼", R.drawable.char_rabbit, currentCategory))
                itemsForCategory.add(ProductItem("char_3", "곰", R.drawable.char_bear, currentCategory))
                itemsForCategory.add(ProductItem("char_4", "오리", R.drawable.char_duck, currentCategory))
                itemsForCategory.add(ProductItem("char_5", "수달", R.drawable.char_otter, currentCategory))
                itemsForCategory.add(ProductItem("char_6", "치즈 고양이", R.drawable.char_gingercat, currentCategory))
            }
            "top" -> {
                itemsForCategory.add(ProductItem("top_1", "민트 티셔츠", R.drawable.t_mint, currentCategory))
                itemsForCategory.add(ProductItem("top_2", "핑크 티셔츠", R.drawable.t_pink, currentCategory))
                itemsForCategory.add(ProductItem("top_3", "퍼플 티셔츠", R.drawable.t_purple, currentCategory))
                itemsForCategory.add(ProductItem("top_4", "화이트 티셔츠", R.drawable.t_white, currentCategory))
                itemsForCategory.add(ProductItem("top_5", "옐로우 티셔츠", R.drawable.t_yellow, currentCategory))
            }
            "onepiece" -> {
                itemsForCategory.add(ProductItem("onepiece_1", "블루 원피스", R.drawable.opc_blue, currentCategory))
                itemsForCategory.add(ProductItem("onepiece_2", "그린 원피스", R.drawable.opc_green, currentCategory))
                itemsForCategory.add(ProductItem("onepiece_3", "핑크 원피스", R.drawable.opc_pink, currentCategory))
                itemsForCategory.add(ProductItem("onepiece_4", "퍼플 원피스", R.drawable.opc_purple, currentCategory))
            }
            "costume" -> {
                itemsForCategory.add(ProductItem("costume_1", "유령옷", R.drawable.cos_ghost, currentCategory))
                itemsForCategory.add(ProductItem("costume_2", "블루 파자마", R.drawable.cos_pajama_b, currentCategory))
                itemsForCategory.add(ProductItem("costume_3", "핑크 파자마", R.drawable.cos_pajama_p, currentCategory))
                itemsForCategory.add(ProductItem("costume_4", "푸딩옷", R.drawable.cos_puding, currentCategory))
                itemsForCategory.add(ProductItem("costume_5", "우비", R.drawable.cos_puding, currentCategory))
                itemsForCategory.add(ProductItem("costume_6", "새우 튀김", R.drawable.cos_shrimp, currentCategory))
            }
            "pants" -> {
                itemsForCategory.add(ProductItem("pants_1", "블루 팬츠", R.drawable.pants_blue, currentCategory))
                itemsForCategory.add(ProductItem("pants_2", "오렌지 팬츠", R.drawable.pants_orange, currentCategory))
                itemsForCategory.add(ProductItem("pants_2", "핑크 팬츠", R.drawable.pants_pink, currentCategory))
            }
//            "glasses" -> {
//                itemsForCategory.add(ProductItem("glasses_1", "둥근 안경", R.drawable.item_glasses_round, currentCategory))
//                itemsForCategory.add(ProductItem("glasses_2", "선글라스", R.drawable.item_glasses_sunnies, currentCategory))
//            }
            "hairAcc" -> {
                itemsForCategory.add(ProductItem("hariAcc_1", "천사 날개", R.drawable.acc_angel, currentCategory))
                itemsForCategory.add(ProductItem("hariAcc_2", "네잎클로버", R.drawable.acc_clover, currentCategory))
                itemsForCategory.add(ProductItem("hariAcc_3", "마법봉", R.drawable.acc_magicstick, currentCategory))
            }
            "acc" -> {
                itemsForCategory.add(ProductItem("acc_1", "천사 날개", R.drawable.acc_angel, currentCategory))
                itemsForCategory.add(ProductItem("acc_2", "네잎클로버", R.drawable.acc_clover, currentCategory))
                itemsForCategory.add(ProductItem("acc_3", "마법봉", R.drawable.acc_magicstick, currentCategory))
            }
            // 다른 카테고리가 있다면 여기에 추가
            else -> {
                // 기본 또는 알 수 없는 카테고리에 대한 처리 (예: 빈 리스트 또는 기본 아이템)
                itemsForCategory.add(ProductItem("default_1", "기본 아이템", R.drawable.char_graycat, currentCategory))
            }
        }

        productAdapter.updateItems(itemsForCategory)
    }

    companion object {
        private const val ARG_CATEGORY_ID = "category_id"
        @JvmStatic
        fun newInstance(categoryId: String) = // categoryId가 "top", "character" 등 문자열로 전달됨
            StoreCategoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY_ID, categoryId)
                }
            }
    }
}