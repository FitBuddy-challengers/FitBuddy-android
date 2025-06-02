package com.cookandroid.challengers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StoreViewModel : ViewModel() {

    // 각 카테고리별로 착용된 아이템을 저장 (Key: 카테고리 ID, Value: ProductItem 또는 null)
    private val _equippedItems = MutableLiveData<MutableMap<String, ProductItem?>>(mutableMapOf())
    val equippedItems: LiveData<Map<String, ProductItem?>> get() = _equippedItems as LiveData<Map<String, ProductItem?>>

    // 미리 정의된 카테고리 ID (StoreOpenFragment의 tabIcons 순서와 매칭되거나, 고유 ID 사용)
    // 예시: "top", "onepiece", "pants", "glasses", "acc", "character"
    // 이 ID들은 StoreCategoryFragment.newInstance()에 전달되는 categoryId와 일치해야 합니다.

    init {
        // ViewModel 초기화 시 모든 카테고리를 null로 설정 (아무것도 착용하지 않은 상태)
        // 예시 카테고리 ID들, 실제 사용하는 ID로 변경 필요
        val categories = listOf("top", "onepiece", "pants", "glasses", "acc", "character") // StoreCategoryFragment로 전달하는 ID와 동일해야 함
        val initialMap = mutableMapOf<String, ProductItem?>()
        categories.forEach { categoryId ->
            initialMap[categoryId] = null
        }
        _equippedItems.value = initialMap
    }

    /**
     * 아이템을 착용/선택합니다.
     * 동일 카테고리에 이미 아이템이 선택되어 있었다면 교체됩니다.
     */
    fun equipItem(category: String, item: ProductItem) {
        val currentMap = _equippedItems.value ?: mutableMapOf()
        currentMap[category] = item
        _equippedItems.value = currentMap // LiveData 업데이트
    }

    /**
     * 특정 카테고리의 아이템 착용을 해제합니다. (선택 사항)
     */
    fun unequipItem(category: String) {
        val currentMap = _equippedItems.value ?: mutableMapOf()
        if (currentMap.containsKey(category)) {
            currentMap[category] = null
            _equippedItems.value = currentMap // LiveData 업데이트
        }
    }

    /**
     * 특정 카테고리에 현재 착용된 아이템을 반환합니다.
     */
    fun getEquippedItemForCategory(category: String): ProductItem? {
        return _equippedItems.value?.get(category)
    }
}