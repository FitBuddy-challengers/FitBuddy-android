package com.cookandroid.challengers

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.databinding.FragmentStoreCategoryBinding
import com.cookandroid.challengers.util.UserPreference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import retrofit2.HttpException

class StoreCategoryFragment : Fragment() {

    private var _binding: FragmentStoreCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var productAdapter: ProductAdapter
    private lateinit var storeViewModel: StoreViewModel
    private lateinit var userPreference: UserPreference
    private var categoryId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            categoryId = it.getString(ARG_CATEGORY_ID)
        }
        storeViewModel = ViewModelProvider(requireActivity()).get(StoreViewModel::class.java)
        userPreference = UserPreference(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoreCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()

        setFragmentResultListener(StoreItemPopUpFragment.REQUEST_KEY_PURCHASE) { _, bundle ->
            val purchaseSuccess = bundle.getBoolean(StoreItemPopUpFragment.RESULT_KEY_PURCHASE_SUCCESS)
            if (purchaseSuccess) {
                Log.d(TAG, "Item purchase successful, reloading data.")
                loadInitialData()
            }
        }

        // ★ 뷰가 처음 생성될 때 한 번만 데이터 로드
        loadInitialData()
    }

    // ★ onResume에서 매번 로드하는 로직 제거 (탭 전환 시 불필요한 API 호출 및 취소 오류 방지)
    override fun onResume() {
        super.onResume()
        // 데이터 갱신은 구매 성공 시 또는 화면에 처음 진입 시에만 수행
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(1) { clickedItem ->
            val userLevel = storeViewModel.userLevel.value ?: 1
            when {
                userLevel < clickedItem.requiredLevel -> {
                    Toast.makeText(requireContext(), "레벨 ${clickedItem.requiredLevel} 달성 시 구매할 수 있습니다.", Toast.LENGTH_SHORT).show()
                }
                clickedItem.isOwned -> {
                    categoryId?.let { catId ->
                        if (storeViewModel.getEquippedItemForCategory(catId)?.id == clickedItem.id) {
                            storeViewModel.unequipItem(catId)
                        } else {
                            storeViewModel.equipItem(catId, clickedItem)
                        }
                    }
                }
                else -> {
                    StoreItemPopUpFragment.newInstance(clickedItem)
                        .show(parentFragmentManager, StoreItemPopUpFragment.TAG)
                }
            }
        }
        binding.recyclerViewCategoryItems.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = productAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        storeViewModel.equippedItems.observe(viewLifecycleOwner) { equippedMap ->
            productAdapter.setSelectedItemId(equippedMap[categoryId]?.id)
        }
        storeViewModel.userLevel.observe(viewLifecycleOwner) { level ->
            productAdapter.updateUserLevel(level)
        }
    }

    private fun loadInitialData() {
        val userId = userPreference.getUserId()
        val currentCategory = categoryId ?: return
        if (userId == -1) {
            Toast.makeText(requireContext(), "사용자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "[$currentCategory] 1. Starting data load for userId: $userId")

        lifecycleScope.launch {
            try {
                // 1. 서버에서 사용자 정보(레벨)와 소유 아이템 ID 목록을 동시에 가져옴
                val userProgressDeferred = async { RetrofitClient.challengeApi.getUserChallengeProgress(userId) }
                val ownedItemsDeferred = async { RetrofitClient.storeApi.getOwnedItemIds(userId) }

                // 2. await으로 API 응답 대기
                val userData = userProgressDeferred.await()
                val ownedItemsResponse = ownedItemsDeferred.await()

                if (!ownedItemsResponse.isSuccessful) {
                    throw HttpException(ownedItemsResponse) // 오류 응답을 예외로 던짐
                }
                val ownedItemIdsFromServer = ownedItemsResponse.body()?.toSet() ?: emptySet()

                Log.d(TAG, "[$currentCategory] 2. API calls successful. User Level: ${userData.level}, Owned Items Count: ${ownedItemIdsFromServer.size}")

                // 3. ViewModel 및 어댑터에 데이터 반영
                storeViewModel.setUserLevelAndCoin(userData.level, userData.coin)
                productAdapter.updateUserLevel(userData.level)

                // 4. 로컬에서 해당 카테고리의 모든 아이템 정적 데이터 목록을 가져옴
                val allLocalItems = getLocalItemsForCategory(currentCategory)
                Log.d(TAG, "[$currentCategory] 3. Loaded ${allLocalItems.size} local items.")

                // 5. 서버의 소유 정보와 로컬의 정적 데이터를 병합하여 최종 리스트 생성
                val finalList = allLocalItems.map { localItem ->
                    localItem.copy(isOwned = localItem.isOwned || ownedItemIdsFromServer.contains(localItem.id))
                }
                Log.d(TAG, "[$currentCategory] 4. Merged list created. Total items: ${finalList.size}")

                // 6. 어댑터에 최종 목록 제출
                productAdapter.submitList(finalList)

            } catch (e: CancellationException) {
                Log.i(TAG, "[$currentCategory] Data loading was cancelled.")
            } catch (e: Exception) {
                val errorMessage = if (e is HttpException) "데이터 로드 실패 (코드: ${e.code()})" else "데이터를 불러오는 데 실패했습니다."
                Log.e(TAG, "[$currentCategory] Failed to load store data", e)
                if(isAdded) {
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getLocalItemsForCategory(category: String): List<ProductItem> {
        val items = mutableListOf<ProductItem>()
        // isOwned: 기본 지급 아이템일 경우에만 true로 설정, 나머지는 서버 데이터와 병합
        when (category) {
            "character" -> {
                items.add(ProductItem(1, "회색 고양이", R.drawable.char_graycat, category, 0, 1, true))
                items.add(ProductItem(2, "토끼", R.drawable.char_rabbit, category, 500, 1))
                items.add(ProductItem(3, "곰", R.drawable.char_bear, category, 500, 2))
                items.add(ProductItem(4, "오리", R.drawable.char_duck, category, 500, 2))
                items.add(ProductItem(5, "수달", R.drawable.char_otter, category, 500, 2))
                items.add(ProductItem(6, "치즈 고양이", R.drawable.char_gingercat, category, 500, 3))
            }
            "top" -> {
                items.add(ProductItem(101, "민트 티셔츠", R.drawable.t_mint, category, 100, 1,true))
                items.add(ProductItem(102, "핑크 티셔츠", R.drawable.t_pink, category, 100, 1))
                items.add(ProductItem(103, "퍼플 티셔츠", R.drawable.t_purple, category, 150, 2))
                items.add(ProductItem(104, "화이트 티셔츠", R.drawable.t_white, category, 150, 2))
                items.add(ProductItem(105, "옐로우 티셔츠", R.drawable.t_yellow, category, 100, 1))
            }
            "onepiece" -> {
                items.add(ProductItem(201, "블루 원피스", R.drawable.opc_blue, category, 250, 1, true))
                items.add(ProductItem(202, "그린 원피스", R.drawable.opc_green, category, 250, 2))
                items.add(ProductItem(203, "핑크 원피스", R.drawable.opc_pink, category, 300, 3))
                items.add(ProductItem(204, "퍼플 원피스", R.drawable.opc_purple, category, 300, 3))
            }
            "costume" -> {
                items.add(ProductItem(301, "유령옷", R.drawable.cos_ghost, category, 200, 2))
                items.add(ProductItem(302, "블루 파자마", R.drawable.cos_pajama_b, category, 200, 1))
                items.add(ProductItem(303, "핑크 파자마", R.drawable.cos_pajama_p, category, 200, 1))
                items.add(ProductItem(304, "푸딩옷", R.drawable.cos_puding, category, 250, 4))
                items.add(ProductItem(305, "우비", R.drawable.cos_raincoat, category, 250, 4))
                items.add(ProductItem(306, "새우 튀김", R.drawable.cos_shrimp, category, 300, 5))
            }
            "pants" -> {
                items.add(ProductItem(401, "블루 팬츠", R.drawable.pants_blue, category, 100, 1, true))
                items.add(ProductItem(402, "오렌지 팬츠", R.drawable.pants_orange, category, 100, 1))
                items.add(ProductItem(403, "핑크 팬츠", R.drawable.pants_pink, category, 100, 1))
            }
            "hairAcc" -> {
                items.add(ProductItem(501, "천사 날개", R.drawable.acc_angel, category, 500, 5))
                items.add(ProductItem(502, "천사 날개", R.drawable.acc_angel, category, 500, 5))
            }
            "acc" -> {
                items.add(ProductItem(601, "천사 날개", R.drawable.acc_angel, category, 500, 5))
                items.add(ProductItem(602, "네잎클로버", R.drawable.acc_clover, category, 300, 1))
                items.add(ProductItem(603, "마법봉", R.drawable.acc_magicstick, category, 400, 3))
            }
        }
        return items
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "StoreCategoryFragment"
        private const val ARG_CATEGORY_ID = "category_id"
        @JvmStatic
        fun newInstance(categoryId: String) =
            StoreCategoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY_ID, categoryId)
                }
            }
    }
}
