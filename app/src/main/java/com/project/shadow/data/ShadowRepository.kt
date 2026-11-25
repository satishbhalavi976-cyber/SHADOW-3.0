package com.project.shadow.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * SHADOW 3.0 Repository
 * Shared conversation store across MainActivity, HistoryActivity, and SettingsActivity.
 */
object ShadowRepository {

    private val _currentConversation = MutableStateFlow(Conversation(title = "New Chat"))
    val currentConversation: StateFlow<Conversation> = _currentConversation

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    fun addMessage(message: ChatMessage) {
        val current = _currentConversation.value
        val updatedMessages = current.messages.toMutableList().apply { add(message) }
        val updatedTitle = if (current.title == "New Chat" && message.role == MessageRole.USER) {
            message.content.take(40)
        } else {
            current.title
        }
        _currentConversation.value = current.copy(
            title = updatedTitle,
            messages = updatedMessages,
            updatedAt = System.currentTimeMillis()
        )
    }

    fun updateLastMessage(content: String) {
        val current = _currentConversation.value
        if (current.messages.isNotEmpty()) {
            val updatedMessages = current.messages.toMutableList()
            val last = updatedMessages.last()
            updatedMessages[updatedMessages.size - 1] = last.copy(
                content = content,
                isLoading = false
            )
            _currentConversation.value = current.copy(
                messages = updatedMessages,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun startNewConversation() {
        val old = _currentConversation.value
        if (old.messages.isNotEmpty()) {
            val newList = _conversations.value.toMutableList()
            newList.removeAll { it.id == old.id }
            newList.add(0, old)
            _conversations.value = newList
        }
        _currentConversation.value = Conversation(title = "New Chat")
    }

    fun clearCurrentChat() {
        _currentConversation.value = Conversation(title = "New Chat")
    }

    fun clearAllHistory() {
        _conversations.value = emptyList()
        _currentConversation.value = Conversation(title = "New Chat")
    }

    fun loadConversation(conversation: Conversation) {
        val current = _currentConversation.value
        if (current.messages.isNotEmpty() && current.id != conversation.id) {
            val newList = _conversations.value.toMutableList()
            newList.removeAll { it.id == current.id }
            newList.add(0, current)
            _conversations.value = newList
        }
        _currentConversation.value = conversation
    }

    fun getAllConversations(): List<Conversation> {
        val current = _currentConversation.value
        val list = _conversations.value.toMutableList()
        if (current.messages.isNotEmpty() && list.none { it.id == current.id }) {
            list.add(0, current)
        }
        return list
    }

    fun getMessages(): List<ChatMessage> = _currentConversation.value.messages.toList()
}
