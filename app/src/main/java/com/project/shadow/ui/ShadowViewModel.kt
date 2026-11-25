package com.project.shadow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.shadow.data.ChatMessage
import com.project.shadow.data.MessageRole
import com.project.shadow.data.ShadowRepository
import com.project.shadow.utils.ShadowAIEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * SHADOW 3.0 ViewModel
 * Bridges UI ↔ AI Engine ↔ Repository
 */
class ShadowViewModel : ViewModel() {

    private val repository = ShadowRepository
    private val aiEngine = ShadowAIEngine()

    private val _messages = MutableStateFlow<List<ChatMessage>>(repository.getMessages())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _speakText = MutableSharedFlow<String>()
    val speakText: SharedFlow<String> = _speakText.asSharedFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    fun refreshMessages() {
        _messages.value = repository.getMessages()
    }

    fun sendMessage(text: String, imageUri: String? = null) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val userMsg = ChatMessage(
                role = MessageRole.USER,
                content = text,
                imageUri = imageUri
            )
            repository.addMessage(userMsg)
            _messages.value = repository.getMessages()

            val loadingMsg = ChatMessage(
                role = MessageRole.SHADOW,
                content = "...",
                isLoading = true
            )
            repository.addMessage(loadingMsg)
            _messages.value = repository.getMessages()
            _isLoading.value = true

            try {
                val response = aiEngine.processQuery(text, repository.getMessages())
                repository.updateLastMessage(response)
                _messages.value = repository.getMessages()
                _speakText.emit(response)
            } catch (e: Exception) {
                val errorMsg = "I encountered an error. Please try again."
                repository.updateLastMessage(errorMsg)
                _messages.value = repository.getMessages()
                _error.emit(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startNewChat() {
        viewModelScope.launch {
            repository.startNewConversation()
            _messages.value = emptyList()
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearCurrentChat()
            _messages.value = emptyList()
        }
    }

    fun getConversations() = repository.conversations
}
