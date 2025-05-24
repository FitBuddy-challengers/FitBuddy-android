package com.cookandroid.challengers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

sealed class ChatMessage {
    abstract val timestamp: String

    data class FromBot(val message: String, override val timestamp: String) : ChatMessage()
    data class FromUser(val message: String, override val timestamp: String) : ChatMessage()
}

class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_BOT = 0
        private const val TYPE_USER = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (messages[position]) {
            is ChatMessage.FromBot -> TYPE_BOT
            is ChatMessage.FromUser -> TYPE_USER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_BOT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chatbot, parent, false)
            BotViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_user, parent, false)
            UserViewHolder(view)
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val message = messages[position]) {
            is ChatMessage.FromBot -> (holder as BotViewHolder).bind(message)
            is ChatMessage.FromUser -> (holder as UserViewHolder).bind(message)
        }
    }

    inner class BotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(msg: ChatMessage.FromBot) {
            itemView.findViewById<TextView>(R.id.tvMessage).text = msg.message
            itemView.findViewById<TextView>(R.id.tvTime).text = msg.timestamp
        }
    }

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(msg: ChatMessage.FromUser) {
            itemView.findViewById<TextView>(R.id.tvUserMessage).text = msg.message
            itemView.findViewById<TextView>(R.id.tvUserTime).text = msg.timestamp
        }
    }
}