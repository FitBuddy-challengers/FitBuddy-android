package com.cookandroid.challengers

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.data.StoreItemData
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

        // 구매 완료 후 아이템 목록을 새로고침하기 위한 리스너
        setFragmentResultListener(StoreItemPopUpFragment.REQUEST_KEY_PURCHASE) { _, bundle ->
            val purchaseSuccess = bundle.getBoolean(StoreItemPopUpFragment.RESULT_KEY_PURCHASE_SUCCESS)
            if (purchaseSuccess) {
                Log.d(TAG, "Item purchase successful, reloading data for category: $categoryId")
                loadInitialData()
            }
        }

        // ★★★ 뷰가 처음 생성될 때 한 번만 데이터 로드 ★★★
        loadInitialData()
    }

    // ★★★ onResume에서 데이터 로드 로직 제거 ★★★
    override fun onResume() {
        super.onResume()
        // 데이터 로드는 onViewCreated에서 한 번만 하고, 이후 갱신은 필요 시에만 수행
    }

    private fun setupRecyclerView() {
        // 어댑터 초기화 (사용자 레벨은 나중에 서버에서 받아와서 동적으로 업데이트)
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
        // 착용된 아이템이 변경되면 어댑터에 알려 UI(파란 테두리)를 업데이트
        storeViewModel.equippedItems.observe(viewLifecycleOwner) { equippedMap ->
            productAdapter.setSelectedItemId(equippedMap[categoryId]?.id)
        }
        // 사용자 레벨이 변경되면 어댑터에 알려 UI(잠금 상태)를 업데이트
        storeViewModel.userLevel.observe(viewLifecycleOwner) { level ->
            productAdapter.updateUserLevel(level)
        }
    }

    private fun loadInitialData() {
        val userId = userPreference.getUserId()
        val currentCategory = categoryId ?: return
        if (userId == -1) {
            // 로그인 정보가 없으면 로드 시도조차 하지 않음
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
                    throw HttpException(ownedItemsResponse)
                }
                val ownedItemIdsFromServer = ownedItemsResponse.body()?.toSet() ?: emptySet()

                Log.d(TAG, "[$currentCategory] 2. API calls successful. User Level: ${userData.level}, Owned Items Count: ${ownedItemIdsFromServer.size}")

                // 3. ViewModel 및 어댑터에 데이터 반영
                storeViewModel.setUserLevelAndCoin(userData.level, userData.coin)
                productAdapter.updateUserLevel(userData.level)

                // 4. 로컬에서 해당 카테고리의 모든 아이템 (정적 데이터) 목록을 가져옴
                val allLocalItems = StoreItemData.getItemsForCategory(currentCategory)
                Log.d(TAG, "[$currentCategory] 3. Loaded ${allLocalItems.size} local items from StoreItemData.")

                // 5. 서버의 소유 정보와 로컬의 정적 데이터를 병합하여 최종 리스트 생성
                val finalList = allLocalItems.map { localItem ->
                    // isOwned: 기본 지급 아이템이거나, 서버에서 소유했다고 알려준 경우 true
                    localItem.copy(isOwned = localItem.isOwned || ownedItemIdsFromServer.contains(localItem.id))
                }
                Log.d(TAG, "[$currentCategory] 4. Merged list created. Total items: ${finalList.size}")

                // 6. 어댑터에 최종 목록 제출
                productAdapter.submitList(finalList)

            } catch (e: CancellationException) {
                // 사용자가 화면을 벗어나 코루틴이 취소된 경우, 정상적인 동작이므로 로그만 남기고 무시
                Log.i(TAG, "[$currentCategory] Data loading was cancelled.")
            } catch (e: Exception) {
                // 그 외 네트워크 오류나 서버 에러
                val errorMessage = if (e is HttpException) "데이터 로드 실패 (코드: ${e.code()})" else "데이터를 불러오는 데 실패했습니다."
                Log.e(TAG, "[$currentCategory] Failed to load store data", e)
                if(isAdded) {
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
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
