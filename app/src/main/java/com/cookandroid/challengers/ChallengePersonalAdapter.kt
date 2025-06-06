package com.cookandroid.challengers


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cookandroid.challengers.R
import com.cookandroid.challengers.api.RetrofitClient

import com.google.android.material.progressindicator.LinearProgressIndicator

class ChallengePersonalAdapter :
    ListAdapter<RetrofitClient.ChallengeItemUiModel, ChallengePersonalAdapter.ChallengeViewHolder>(DiffCallback) {

    inner class ChallengeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val challengeNameTextView: TextView = itemView.findViewById(R.id.challengeNameTextView)
        private val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.progressBar)
        private val progressTextView: TextView = itemView.findViewById(R.id.progressTextView)
        private val rewardCoinTextView: TextView = itemView.findViewById(R.id.rewardCoinTextView)
        private val rewardCoinImageView: ImageView = itemView.findViewById(R.id.rewardCoinImageView)

        fun bind(item: RetrofitClient.ChallengeItemUiModel) {
            challengeNameTextView.text = item.title
            progressBar.progress = item.progressPercent
            progressTextView.text = "${item.progressPercent}%"
            rewardCoinTextView.text = item.reward.toString()
            rewardCoinImageView.setImageResource(R.drawable.ic_coin) // drawable/ic_coin 이미지를 사용
            if (item.progressPercent >= 100) {
                itemView.setBackgroundResource(R.drawable.bg_challenge_complete) // ✅ 달성 배경
            } else {
                itemView.setBackgroundResource(R.drawable.set_item_background) // 기본 배경
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_challenge_personal, parent, false)
        return ChallengeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<RetrofitClient.ChallengeItemUiModel>() {
        override fun areItemsTheSame(oldItem: RetrofitClient.ChallengeItemUiModel, newItem: RetrofitClient.ChallengeItemUiModel): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: RetrofitClient.ChallengeItemUiModel, newItem: RetrofitClient.ChallengeItemUiModel): Boolean {
            return oldItem == newItem
        }
    }
}