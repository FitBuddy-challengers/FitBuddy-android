package com.cookandroid.challengers

import android.util.Log // Log 사용을 위해 추가 (선택 사항)
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StoreViewModel : ViewModel() {

    // 각 카테고리별로 착용된 아이템을 저장 (Key: 카테고리 ID, Value: ProductItem 또는 null)
    private val _equippedItems = MutableLiveData<MutableMap<String, ProductItem?>>(mutableMapOf())
    val equippedItems: LiveData<Map<String, ProductItem?>> get() = _equippedItems as LiveData<Map<String, ProductItem?>>

    // 미리 정의된 카테고리 ID
    val categories = listOf("top", "onepiece", "pants", "glasses", "acc", "character")

    init {
        // ViewModel 초기화 시 모든 카테고리를 null로 설정 (아무것도 착용하지 않은 상태)
        val initialMap = mutableMapOf<String, ProductItem?>()
        categories.forEach { categoryId ->
            initialMap[categoryId] = null
        }
        _equippedItems.value = initialMap
    }

    /**
     * 아이템을 착용/선택합니다.
     * 상호 배제 규칙(원피스 vs 상/하의)이 적용됩니다.
     */
    fun equipItem(categoryOfNewItem: String, newItem: ProductItem) {
        val currentMap = _equippedItems.value?.toMutableMap() ?: mutableMapOf()

        // --- 상호 배제 규칙 적용 ---
        // 규칙 1: 새로 착용하는 아이템이 "top" 또는 "pants"인 경우
        if (categoryOfNewItem == "top" || categoryOfNewItem == "pants") {
            if (currentMap["onepiece"] != null) { // 현재 "onepiece"가 착용되어 있다면
                currentMap["onepiece"] = null // "onepiece" 착용 해제
                Log.d("StoreViewModel", "OnePiece unequipped because $categoryOfNewItem was equipped.")
            }
        }

        // 규칙 2: 새로 착용하는 아이템이 "onepiece"인 경우
        if (categoryOfNewItem == "onepiece") {
            if (currentMap["top"] != null) { // 현재 "top"이 착용되어 있다면
                currentMap["top"] = null // "top" 착용 해제
                Log.d("StoreViewModel", "Top unequipped because onepiece was equipped.")
            }
            if (currentMap["pants"] != null) { // 현재 "pants"가 착용되어 있다면
                currentMap["pants"] = null // "pants" 착용 해제
                Log.d("StoreViewModel", "Pants unequipped because onepiece was equipped.")
            }
        }
        // --- 상호 배제 규칙 적용 끝 ---

        // 새로운 아이템을 해당 카테고리에 착용
        currentMap[categoryOfNewItem] = newItem
        // Log.d("StoreViewModel", "Equipped ${newItem.name} in category $categoryOfNewItem.") // ProductItem에 name 필드가 있다면 로그 사용 가능

        _equippedItems.value = currentMap // LiveData 업데이트하여 UI에 변경 알림
    }

    /**
     * 특정 카테고리의 아이템 착용을 해제합니다.
     * (주의: 이 함수는 상호 배제 규칙을 직접 트리거하지 않습니다.
     * 예를 들어, 이 함수로 'top'을 해제해도 'onepiece'가 자동으로 착용되지는 않습니다.
     * 필요하다면 이 함수도 규칙에 맞게 확장할 수 있습니다.)
     */
    fun unequipItem(category: String) {
        val currentMap = _equippedItems.value ?: mutableMapOf()
        if (currentMap.containsKey(category)) {
            currentMap[category] = null
            _equippedItems.value = currentMap // LiveData 업데이트
            Log.d("StoreViewModel", "Unequipped item from category: $category.")
        }
    }

    /**
     * 특정 카테고리에 현재 착용된 아이템을 반환합니다.
     */
    fun getEquippedItemForCategory(category: String): ProductItem? {
        return _equippedItems.value?.get(category)
    }
}