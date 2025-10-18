package com.cookandroid.challengers

import android.graphics.Color
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cookandroid.challengers.databinding.ItemStoreProductBinding
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProductItem(
    val id: Int,
    val name: String,
    val imageResId: Int,
    val category: String,
    val price: Int,
    val requiredLevel: Int = 1,
    var isOwned: Boolean = false
) : Parcelable

class ProductAdapter(
    private var userLevel: Int, // 생성 시 초기 레벨을 받음
    private val onItemClick: (ProductItem) -> Unit
) : ListAdapter<ProductItem, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    private var selectedItemId: Int? = null

    inner class ProductViewHolder(private val binding: ItemStoreProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProductItem) {
            // 이미지 로드
            Glide.with(itemView.context)
                .load(item.imageResId)
                .placeholder(R.drawable.char_graycat) // 로딩 중 이미지
                .error(R.drawable.char_graycat)       // 에러 시 이미지
                .into(binding.imageViewProduct)

            // UI 상태 초기화 (재사용 시 이전 상태가 남는 것을 방지)
            binding.lockOverlay.visibility = View.GONE
            binding.statusContainer.visibility = View.VISIBLE
            binding.statusIcon.visibility = View.GONE
            binding.materialCardView.strokeWidth = 0
            binding.itemImage.setBackgroundResource(R.drawable.bg_store_item_top)
            binding.statusContainer.setBackgroundResource(R.drawable.bg_store_item_bottom)
            binding.statusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))

            // 아이템 상태에 따라 UI 분기 처리
            when {
                // 상태 1: 레벨 제한으로 잠긴 아이템
                userLevel < item.requiredLevel -> {
                    binding.lockOverlay.visibility = View.VISIBLE
                    binding.lockLevelText.text = "Lv.${item.requiredLevel}"
                    binding.statusContainer.visibility = View.VISIBLE // 하단 상태 영역을 보이도록 변경
                    binding.statusIcon.visibility = View.GONE // 하단 아이콘은 숨김
                    binding.statusText.text = "잠긴 아이템"
                    binding.statusText.setTextColor("#7E7E7E".toColorInt()) // 텍스트 색상 변경
                    binding.root.isClickable = true
                }

                // 상태 2: 구매했고, 현재 착용 중인 아이템
                item.isOwned && item.id == selectedItemId -> {
                    // 배경 변경
                    binding.itemImage.setBackgroundResource(R.drawable.bg_store_item_top_black)
                    binding.statusContainer.setBackgroundResource(R.drawable.bg_store_item_bottom_black)

                    // 텍스트 및 아이콘 설정
                    binding.statusIcon.visibility = View.GONE
                    binding.statusText.text = "착용중"
                    binding.statusText.setTextColor(Color.WHITE)
                    binding.root.isClickable = true
                }

                // 상태 3: 구매했지만, 착용하지 않은 아이템
                item.isOwned -> {
                    binding.statusIcon.visibility = View.GONE
                    binding.statusText.text = "구매완료"
                    binding.root.isClickable = true
                }

                // 상태 4: 구매하지 않은 아이템 (구매 가능)
                else -> {
                    binding.statusIcon.setImageResource(R.drawable.ic_coin) // 코인 아이콘 설정
                    binding.statusIcon.visibility = View.VISIBLE
                    binding.statusText.text = item.price.toString()
                    binding.root.isClickable = true
                }
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemStoreProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateUserLevel(newUserLevel: Int?) {
        if (userLevel != newUserLevel) {
            if (newUserLevel != null) {
                userLevel = newUserLevel
            }
            // 레벨이 변경되면 모든 아이템의 잠금 상태가 바뀔 수 있으므로 전체 목록을 다시 그림
            notifyDataSetChanged()
        }
    }

    fun setSelectedItemId(itemId: Int?) {
        val oldSelectedId = selectedItemId
        selectedItemId = itemId

        // 이전 선택 아이템과 새 선택 아이템의 UI를 갱신
        oldSelectedId?.let { id ->
            currentList.indexOfFirst { it.id == id }.takeIf { it != -1 }?.let { notifyItemChanged(it) }
        }
        selectedItemId?.let { id ->
            currentList.indexOfFirst { it.id == id }.takeIf { it != -1 }?.let { notifyItemChanged(it) }
        }
    }
}

class ProductDiffCallback : DiffUtil.ItemCallback<ProductItem>() {
    override fun areItemsTheSame(oldItem: ProductItem, newItem: ProductItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ProductItem, newItem: ProductItem): Boolean {
        // isOwned 상태가 변경될 수 있으므로 전체 내용을 비교
        return oldItem == newItem
    }
}