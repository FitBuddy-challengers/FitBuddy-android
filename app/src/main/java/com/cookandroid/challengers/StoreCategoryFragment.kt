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
        loadCategoryItems() // 아이템 로드
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
            }
            "top" -> {
                itemsForCategory.add(ProductItem("top_1", "민트 티셔츠", R.drawable.t_mint, currentCategory))
                itemsForCategory.add(ProductItem("top_2", "핑크 티셔츠", R.drawable.t_pink, currentCategory))
            }
            "onepiece" -> {
                itemsForCategory.add(ProductItem("onepiece_1", "블루 원피스", R.drawable.opc_blue, currentCategory))
                itemsForCategory.add(ProductItem("onepiece_2", "그린 원피스", R.drawable.opc_green, currentCategory))
            }
            "pants" -> {
                itemsForCategory.add(ProductItem("pants_1", "블루 팬츠", R.drawable.pants_blue, currentCategory))
                itemsForCategory.add(ProductItem("pants_2", "오렌지 팬츠", R.drawable.pants_orange, currentCategory))
            }
//            "glasses" -> {
//                itemsForCategory.add(ProductItem("glasses_1", "둥근 안경", R.drawable.item_glasses_round, currentCategory))
//                itemsForCategory.add(ProductItem("glasses_2", "선글라스", R.drawable.item_glasses_sunnies, currentCategory))
//            }
            "acc" -> {
                itemsForCategory.add(ProductItem("acc_1", "천사 날개", R.drawable.acc_angel, currentCategory))
                itemsForCategory.add(ProductItem("acc_2", "네잎클로버", R.drawable.acc_clover, currentCategory))
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