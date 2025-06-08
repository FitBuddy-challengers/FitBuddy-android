package com.cookandroid.challengers

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookandroid.challengers.api.RetrofitClient
import kotlinx.coroutines.launch

class StoreViewModel : ViewModel() {

    // 각 카테고리별로 착용된 아이템을 저장 (Key: 카테고리 ID, Value: ProductItem 또는 null)
    private val _equippedItems = MutableLiveData<MutableMap<String, ProductItem?>>(mutableMapOf())
    val equippedItems: LiveData<Map<String, ProductItem?>> get() = _equippedItems as LiveData<Map<String, ProductItem?>>

    // 사용자 레벨과 코인 정보를 관리하는 LiveData
    private val _userLevel = MutableLiveData<Int>()
    val userLevel: LiveData<Int> get() = _userLevel

    private val _userCoin = MutableLiveData<Int>()
    val userCoin: LiveData<Int> get() = _userCoin

    // 관리할 카테고리 ID 목록
    val categories = listOf("top", "onepiece", "costume", "pants", "glasses", "hairAcc", "acc", "character")

    init {
        // ViewModel 초기화 시 모든 카테고리를 null로 설정 (아무것도 착용하지 않은 상태)
        val initialMap = mutableMapOf<String, ProductItem?>()
        categories.forEach { categoryId ->
            initialMap[categoryId] = null
        }
        _equippedItems.value = initialMap
        _userLevel.value = 1 // 기본 레벨
        _userCoin.value = 0  // 기본 코인
    }

    /**
     * 서버에서 사용자 정보를 비동기적으로 가져와 LiveData를 업데이트합니다.
     * @param userId 현재 로그인한 사용자의 ID
     */
    fun loadUserData(userId: Int) {
        viewModelScope.launch {
            try {
                // ★★★ suspend 함수는 DTO를 직접 반환하므로, isSuccessful 체크 없이 바로 사용합니다. ★★★
                val userData = RetrofitClient.challengeApi.getUserChallengeProgress(userId)

                // 성공 시 LiveData 업데이트
                _userLevel.postValue(userData.level)
                _userCoin.postValue(userData.coin)
                Log.d("StoreViewModel", "User data loaded: Level=${userData.level}, Coin=${userData.coin}")

            } catch (e: Exception) {
                // 네트워크 오류나 서버 에러(4xx, 5xx) 등 모든 예외는 여기서 처리됩니다.
                Log.e("StoreViewModel", "Exception while loading user data", e)
            }
        }
    }

    // 특정 카테고리의 아이템을 착용
    fun equipItem(categoryOfNewItem: String, newItem: ProductItem) {
        val currentMap = _equippedItems.value?.toMutableMap() ?: mutableMapOf()

        // 규칙 1: 새로 착용하는 아이템이 "코스튬"인 경우
        if (categoryOfNewItem == "costume") {
            currentMap["top"] = null
            currentMap["pants"] = null
            currentMap["onepiece"] = null
            Log.d("StoreViewModel", "Top, Pants, OnePiece unequipped because costume was equipped.")
        }

        // 규칙 2: "상의", "하의", "원피스" 착용 시 코스튬 해제
        if (categoryOfNewItem == "top" || categoryOfNewItem == "pants" || categoryOfNewItem == "onepiece") {
            if (currentMap["costume"] != null) {
                currentMap["costume"] = null
                Log.d("StoreViewModel", "Costume unequipped because clothing was equipped.")
            }
        }

        // 규칙 3: "원피스" 착용 시 "상의", "하의" 해제
        if (categoryOfNewItem == "onepiece") {
            currentMap["top"] = null
            currentMap["pants"] = null
            Log.d("StoreViewModel", "Top and Pants unequipped because onepiece was equipped.")
        } else if (categoryOfNewItem == "top" || categoryOfNewItem == "pants") {
            // "상의" 또는 "하의" 착용 시 "원피스" 해제
            if (currentMap["onepiece"] != null) {
                currentMap["onepiece"] = null
                Log.d("StoreViewModel", "OnePiece unequipped because clothing was equipped.")
            }
        }

        // 새로운 아이템을 해당 카테고리에 착용
        currentMap[categoryOfNewItem] = newItem
        _equippedItems.value = currentMap // LiveData 업데이트하여 UI에 변경 알림
    }

    // 특정 카테고리의 아이템 착용을 해제
    fun unequipItem(category: String) {
        val currentMap = _equippedItems.value?.toMutableMap() ?: return
        if (currentMap.containsKey(category)) {
            currentMap[category] = null
            _equippedItems.value = currentMap
            Log.d("StoreViewModel", "Unequipped item from category: $category")
        }
    }

    /**
     * 특정 카테고리에 현재 착용된 아이템을 반환합니다.
     */
    fun getEquippedItemForCategory(category: String): ProductItem? {
        return _equippedItems.value?.get(category)
    }

    /**
     * 아이템 구매 성공 후 호출됩니다.
     * @param purchasedItem 구매한 아이템 객체
     * @param updatedCoin 구매 후 서버로부터 받은 새로운 코인 잔액
     */
    fun onItemPurchased(purchasedItem: ProductItem, updatedCoin: Int) {
        // 1. 사용자 코인 정보 업데이트
        _userCoin.value = updatedCoin

        // 2. 구매 후 바로 착용
        equipItem(purchasedItem.category, purchasedItem)
        Log.d("StoreViewModel", "Item purchased: ${purchasedItem.name}. User coin updated to: $updatedCoin. Item equipped.")
    }

    /**
     * 외부에서 사용자 레벨과 코인 정보를 직접 설정할 때 사용합니다.
     * (loadUserData가 비동기이므로, 동기적인 업데이트가 필요할 때 유용)
     */
    fun setUserLevelAndCoin(level: Int, coin: Int) {
        if (_userLevel.value != level) {
            _userLevel.value = level
        }
        if (_userCoin.value != coin) {
            _userCoin.value = coin
        }
    }
}
