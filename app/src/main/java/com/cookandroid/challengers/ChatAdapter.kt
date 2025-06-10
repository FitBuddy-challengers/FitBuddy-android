package com.cookandroid.challengers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cookandroid.challengers.databinding.ItemChatbotBinding
import com.cookandroid.challengers.databinding.ItemUserBinding
import com.cookandroid.challengers.model.ChatMessage


//sealed class ChatMessage {
//    abstract val timestamp: String
//
//    data class FromBot(val message: String, override val timestamp: String) : ChatMessage()
//    data class FromUser(val message: String, override val timestamp: String) : ChatMessage()
//}

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()

    companion object {
        private const val TYPE_BOT = 0
        private const val TYPE_USER = 1
    }

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun setMessages(newMessages: List<ChatMessage>) {
        messages.clear()                  // 기존 메시지 모두 제거
        messages.addAll(newMessages)     // 새 메시지 추가
        notifyDataSetChanged()           // 전체 갱신
    }

    override fun getItemViewType(position: Int): Int {
        return when (messages[position]) {
            is ChatMessage.FromBot -> TYPE_BOT
            is ChatMessage.FromUser -> TYPE_USER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_BOT -> {
                val binding = ItemChatbotBinding.inflate(inflater, parent, false)
                BotViewHolder(binding)
            }
            TYPE_USER -> {
                val binding = ItemUserBinding.inflate(inflater, parent, false)
                UserViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val message = messages[position]) {
            is ChatMessage.FromBot -> (holder as BotViewHolder).bind(message)
            is ChatMessage.FromUser -> (holder as UserViewHolder).bind(message)
        }
    }

    inner class BotViewHolder(private val binding: ItemChatbotBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: ChatMessage.FromBot) {
            binding.tvMessage.text = msg.message
            binding.tvTime.text = msg.timestamp
        }
    }

    inner class UserViewHolder(private val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: ChatMessage.FromUser) {
            binding.tvUserMessage.text = msg.message
            binding.tvUserTime.text = msg.timestamp
        }
    }
}

//sealed class ChatMessage {
//    abstract val timestamp: String
//
//    data class FromBot(val message: String, override val timestamp: String) : ChatMessage()
//    data class FromUser(val message: String, override val timestamp: String) : ChatMessage()
//}
//
//class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
//
//    companion object {
//        private const val TYPE_BOT = 0
//        private const val TYPE_USER = 1
//    }
//
//    override fun getItemViewType(position: Int): Int {
//        return when (messages[position]) {
//            is ChatMessage.FromBot -> TYPE_BOT
//            is ChatMessage.FromUser -> TYPE_USER
//        }
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
//        val inflater = LayoutInflater.from(parent.context)
//        return when (viewType) {
//            TYPE_BOT -> {
//                val binding = ItemChatbotBinding.inflate(inflater, parent, false)
//                BotViewHolder(binding)
//            }
//            TYPE_USER -> {
//                val binding = ItemUserBinding.inflate(inflater, parent, false)
//                UserViewHolder(binding)
//            }
//            else -> throw IllegalArgumentException("Invalid view type")
//        }
//    }
//
//    override fun getItemCount(): Int = messages.size
//
//    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        when (val message = messages[position]) {
//            is ChatMessage.FromBot -> (holder as BotViewHolder).bind(message)
//            is ChatMessage.FromUser -> (holder as UserViewHolder).bind(message)
//        }
//    }
//
//    inner class BotViewHolder(private val binding: ItemChatbotBinding) : RecyclerView.ViewHolder(binding.root) {
//        fun bind(msg: ChatMessage.FromBot) {
//            binding.tvMessage.text = msg.message
//            binding.tvTime.text = msg.timestamp
//        }
//    }
//
//    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
//        fun bind(msg: ChatMessage.FromUser) {
//            binding.tvUserMessage.text = msg.message
//            binding.tvUserTime.text = msg.timestamp
//        }
//    }
//}