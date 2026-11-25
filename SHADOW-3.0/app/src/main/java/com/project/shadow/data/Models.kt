package com.project.shadow.data

import java.text.SimpleDateFormat
import java.util.*

/**
 * SHADOW 3.0 Data Models
 */

enum class MessageRole { USER, SHADOW, SYSTEM }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val imageUri: String? = null
) {
    fun formattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val messages: MutableList<ChatMessage> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun preview(): String = messages.lastOrNull()?.content?.take(60) ?: "Empty conversation"
    fun formattedDate(): String {
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        return sdf.format(Date(updatedAt))
    }
}

// Quick action suggestions shown on empty state
data class SuggestionChip(
    val emoji: String,
    val label: String,
    val prompt: String
)

val DEFAULT_SUGGESTIONS = listOf(
    SuggestionChip("🕐", "Set a timer", "Set a timer for 10 minutes"),
    SuggestionChip("🌤", "Weather today", "What's the weather like today?"),
    SuggestionChip("📸", "Scan text", "Open camera to scan text"),
    SuggestionChip("📞", "Call someone", "Call my contact"),
    SuggestionChip("💡", "Explain something", "Explain quantum computing simply"),
    SuggestionChip("📝", "Take a note", "Create a quick note"),
    SuggestionChip("🧮", "Calculate", "Calculate 15% tip on 850"),
    SuggestionChip("🗓", "Reminder", "Remind me to drink water every hour")
)
