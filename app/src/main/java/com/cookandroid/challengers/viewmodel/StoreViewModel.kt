package com.cookandroid.challengers

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.data.StoreItemData
import com.cookandroid.challengers.util.UserPreference
import kotlinx.coroutines.launch

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    // 각 카테고리별로 착용된 아이템을 저장 (Key: 카테고리 ID, Value: ProductItem 또는 null)
    private val _equippedItems = MutableLiveData<MutableMap<String, ProductItem?>>(mutableMapOf())
    val equippedItems: LiveData<Map<String, ProductItem?>> get() = _equippedItems as LiveData<Map<String, ProductItem?>>

    // 사용자 레벨과 코인 정보를 관리하는 LiveData
    private val _userLevel = MutableLiveData<Int>()
    val userLevel: LiveData<Int> get() = _userLevel

    private val _userCoin = MutableLiveData<Int>()
    val userCoin: LiveData<Int> get() = _userCoin

    // 생성된 캐릭터 비트맵을 저장할 LiveData
    private val _characterBitmap = MutableLiveData<Bitmap?>()
    val characterBitmap: LiveData<Bitmap?> = _characterBitmap

    private val userPreference = UserPreference(application)
    // 관리할 카테고리 ID 목록
    val categories = listOf("top", "onepiece", "costume", "pants", "glasses", "hairAcc", "acc", "character")

    init {
        loadInitialEquippedState()
    }

    private fun loadInitialEquippedState() {
        val savedItemIds = userPreference.getEquippedItemIds()
        val initialMap = mutableMapOf<String, ProductItem?>()
        categories.forEach { category ->
            val itemId = savedItemIds[category]
            initialMap[category] = itemId?.let { StoreItemData.findItemById(it) }
        }
        _equippedItems.value = initialMap
        Log.d("StoreViewModel", "Initial state loaded from SharedPreferences: $savedItemIds")
    }

    fun loadUserData(userId: Int) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.challengeApi.getUserChallengeProgress(userId)

                // Response가 성공했는지 검사
                if (response.isSuccessful && response.body() != null) {
                    val userData = response.body()!!

                    _userLevel.postValue(userData.level ?: 1)
                    _userCoin.postValue(userData.coin ?: 0)

                } else {
                    Log.e("StoreViewModel",
                        "Failed to load user data: ${response.code()} - ${response.message()}"
                    )
                }

            } catch (e: Exception) {
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
        saveEquippedState() // ★상태 변경 후 저장
    }

    // 특정 카테고리의 아이템 착용을 해제
    fun unequipItem(category: String) {
        val currentMap = _equippedItems.value?.toMutableMap() ?: return
        if (currentMap.containsKey(category)) {
            currentMap[category] = null
            _equippedItems.value = currentMap
            saveEquippedState()
        }
    }

    // 현재 착용 상태를 SharedPreferences에 저장
    fun saveEquippedState() {
        val currentMap = _equippedItems.value ?: return
        val savableMap = currentMap.mapValues { it.value?.id } // ProductItem -> Int? (ID)
        userPreference.saveEquippedItemIds(savableMap)
        Log.d("StoreViewModel", "Equipped state saved to SharedPreferences: $savableMap")
    }

    // 특정 카테고리에 현재 착용된 아이템을 반환
    fun getEquippedItemForCategory(category: String): ProductItem? {
        return _equippedItems.value?.get(category)
    }

    // 아이템 구매 성공 후 호출.
    fun onItemPurchased(purchasedItem: ProductItem, updatedCoin: Int) {
        // 1. 사용자 코인 정보 업데이트
        _userCoin.value = updatedCoin
        // 2. 구매 후 바로 착용
        equipItem(purchasedItem.category, purchasedItem)
        Log.d("StoreViewModel", "Item purchased: ${purchasedItem.name}. User coin updated to: $updatedCoin. Item equipped.")
    }


    // 사용자 레벨과 코인 정보를 직접 설정할 때 사용
    fun setUserLevelAndCoin(level: Int?, coin: Int?) {
        if (_userLevel.value != level) {
            _userLevel.value = level
        }
        if (_userCoin.value != coin) {
            _userCoin.value = coin
        }
    }

    // 비트맵을 업데이트하는 함수 추가 ★★★
    fun updateCharacterBitmap(bitmap: Bitmap?) {
        _characterBitmap.value = bitmap
    }

}
