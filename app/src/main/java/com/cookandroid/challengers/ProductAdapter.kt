package com.cookandroid.challengers

import android.graphics.Color.blue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

data class ProductItem(
    val id: String,
    val name: String,
    val imageResId: Int,
    val category: String
)
class ProductAdapter(
    private var items: List<ProductItem>,
    private val onItemClick: (ProductItem) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private var selectedItem: ProductItem? = null

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewProduct: ImageView = itemView.findViewById(R.id.image_view_product)
        private val materialCardView: com.google.android.material.card.MaterialCardView = itemView as com.google.android.material.card.MaterialCardView

        fun bind(item: ProductItem, isSelected: Boolean, onItemClick: (ProductItem) -> Unit) {
            imageViewProduct.setImageResource(item.imageResId)
            itemView.setOnClickListener { onItemClick(item) }

            if (isSelected) {
                materialCardView.strokeWidth = 4
                materialCardView.strokeColor = itemView.context.getColor(R.color.blue)
            } else {
                materialCardView.strokeWidth = 0
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_store_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, item.id == selectedItem?.id, onItemClick) // id로 비교
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<ProductItem>) {
        items = newItems
        notifyDataSetChanged() // DiffUtil 사용 권장
    }

    fun setSelectedItem(item: ProductItem?) {
        val oldSelectedItem = selectedItem
        selectedItem = item
        oldSelectedItem?.let { old ->
            items.indexOfFirst { it.id == old.id }.takeIf { it != -1 }
                ?.let { notifyItemChanged(it) }
        }
        selectedItem?.let { current ->
            items.indexOfFirst { it.id == current.id }.takeIf { it != -1 }
                ?.let { notifyItemChanged(it) }
        }
    }
}