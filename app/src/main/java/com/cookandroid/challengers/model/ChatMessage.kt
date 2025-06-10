package com.cookandroid.challengers.model


sealed class ChatMessage {
    abstract val timestamp: String

    data class FromBot(val message: String, override val timestamp: String) : ChatMessage()
    data class FromUser(val message: String, override val timestamp: String) : ChatMessage()
}