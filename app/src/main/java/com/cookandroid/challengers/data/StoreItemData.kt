package com.cookandroid.challengers.data

import android.R.attr.category
import com.cookandroid.challengers.ProductItem
import com.cookandroid.challengers.R

object StoreItemData {

    // 앱의 모든 아이템 정보를 가지고 있는 마스터 리스트
    private val allItems: List<ProductItem> by lazy {
        mutableListOf<ProductItem>().apply {
            // character
            add(ProductItem(1, "회색 고양이", R.drawable.char_graycat, "character", 0, 1, true))
            add(ProductItem(2, "토끼", R.drawable.char_rabbit, "character", 500, 1))
            add(ProductItem(3, "곰", R.drawable.char_bear, "character", 500, 2))
            add(ProductItem(4, "오리", R.drawable.char_duck, "character", 500, 2))
            add(ProductItem(5, "수달", R.drawable.char_otter, "character", 500, 2))
            add(ProductItem(6, "치즈 고양이", R.drawable.char_gingercat, "character", 500, 3))
            // top
            add(ProductItem(101, "민트 티셔츠", R.drawable.t_mint, "top", 100, 1, true))
            add(ProductItem(102, "핑크 티셔츠", R.drawable.t_pink, "top", 100, 1))
            add(ProductItem(103, "퍼플 티셔츠", R.drawable.t_purple, "top", 150, 2))
            add(ProductItem(104, "화이트 티셔츠", R.drawable.t_white, "top", 150, 2))
            add(ProductItem(105, "옐로우 티셔츠", R.drawable.t_yellow, "top", 100, 1))
            // onepiece
            add(ProductItem(201, "블루 원피스", R.drawable.opc_blue, "onepiece", 250, 2, true))
            add(ProductItem(202, "그린 원피스", R.drawable.opc_green, "onepiece", 250, 2))
            add(ProductItem(203, "핑크 원피스", R.drawable.opc_pink, "onepiece", 300, 3))
            add(ProductItem(204, "퍼플 원피스", R.drawable.opc_purple, "onepiece", 300, 3))
            // costume
            add(ProductItem(301, "유령옷", R.drawable.cos_ghost, "costume", 200, 2))
            add(ProductItem(302, "블루 파자마", R.drawable.cos_pajama_b, "costume", 200, 1))
            add(ProductItem(303, "핑크 파자마", R.drawable.cos_pajama_p, "costume", 200, 1))
            add(ProductItem(304, "푸딩옷", R.drawable.cos_puding, "costume", 250, 4))
            add(ProductItem(305, "우비", R.drawable.cos_raincoat, "costume", 250, 4))
            add(ProductItem(306, "새우 튀김", R.drawable.cos_shrimp, "costume", 300, 5))
            // pants
            add(ProductItem(401, "블루 팬츠", R.drawable.pants_blue, "pants", 100, 1, true))
            add(ProductItem(402, "오렌지 팬츠", R.drawable.pants_orange, "pants", 100, 1))
            add(ProductItem(403, "핑크 팬츠", R.drawable.pants_pink, "pants", 100, 1))
            // hairAcc
            add(ProductItem(501, "블루 리본", R.drawable.ribbon_blue, "hairAcc", 200, 1))
            add(ProductItem(502, "핑크 리본", R.drawable.ribbon_pink, "hairAcc", 200, 1))
            add(ProductItem(503, "퍼플 리본", R.drawable.ribbon_purple, "hairAcc", 200, 1))
            add(ProductItem(504, "레드 리본", R.drawable.ribbon_red, "hairAcc", 200, 1))
            add(ProductItem(505, "옐로우 리본", R.drawable.ribbon_yellow, "hairAcc", 200, 1))
            // acc
            add(ProductItem(601, "천사 날개", R.drawable.acc_angel, "acc", 500, 5))
            add(ProductItem(602, "네잎클로버", R.drawable.acc_clover, "acc", 300, 1))
            add(ProductItem(603, "마법봉", R.drawable.acc_magicstick, "acc", 400, 3))
            add(ProductItem(604, "파란 풍선", R.drawable.acc_balloon_blue, "acc", 400, 3))
            add(ProductItem(605, "핑크 풍선", R.drawable.acc_balloon_pink, "acc", 400, 3))
            add(ProductItem(606, "보라 풍선", R.drawable.acc_balloon_purple, "acc", 400, 3))
            add(ProductItem(607, "빨간 풍선", R.drawable.acc_balloon_red, "acc", 400, 3))
            // glasses
            add(ProductItem(701, "안경", R.drawable.eyewear_glasses, "glasses", 300, 1))
            add(ProductItem(702, "선글라스", R.drawable.eyewear_sunglasses, "glasses", 400, 3))

        }
    }

    fun getItemsForCategory(category: String): List<ProductItem> {
        return allItems.filter { it.category == category }
    }

    fun findItemById(itemId: Int): ProductItem? {
        return allItems.find { it.id == itemId }
    }
}