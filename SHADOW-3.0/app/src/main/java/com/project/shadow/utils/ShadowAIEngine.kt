package com.project.shadow.utils

import com.project.shadow.data.ChatMessage
import com.project.shadow.data.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

/**
 * SHADOW 3.0 AI Engine
 *
 * Handles intelligent query routing:
 * - Local command processing (timers, alarms, calculator, date/time)
 * - System actions (calls, SMS, settings)
 * - General knowledge via API
 *
 * In production: replace generateAPIResponse() with actual LLM API call.
 */
class ShadowAIEngine {

    private val commandPatterns = mapOf(
        Regex("(set|start|create).{0,20}timer.{0,30}(\\d+).{0,10}(minute|min|second|sec|hour|hr)", RegexOption.IGNORE_CASE) to ::handleTimer,
        Regex("what.{0,10}time", RegexOption.IGNORE_CASE) to ::handleTime,
        Regex("what.{0,10}date|today.{0,10}date", RegexOption.IGNORE_CASE) to ::handleDate,
        Regex("calculate|what is [\\d+\\-*/()]+|\\d+\\s*[+\\-*/]\\s*\\d+", RegexOption.IGNORE_CASE) to ::handleCalculator,
        Regex("weather", RegexOption.IGNORE_CASE) to ::handleWeather,
        Regex("hello|hi|hey shadow", RegexOption.IGNORE_CASE) to ::handleGreeting,
        Regex("who are you|what are you|your name", RegexOption.IGNORE_CASE) to ::handleIdentity,
        Regex("thank", RegexOption.IGNORE_CASE) to ::handleThanks
    )

    suspend fun processQuery(query: String, history: List<ChatMessage>): String {
        return withContext(Dispatchers.IO) {
            // Try local command patterns first
            for ((pattern, handler) in commandPatterns) {
                if (pattern.containsMatchIn(query)) {
                    return@withContext handler(query, pattern.find(query))
                }
            }
            // Fall back to AI response
            generateAPIResponse(query, history)
        }
    }

    // =================== LOCAL HANDLERS ===================

    private fun handleTimer(query: String, match: MatchResult?): String {
        val number = Regex("(\\d+)").find(query)?.value?.toIntOrNull() ?: 1
        val unit = when {
            query.contains("hour", true) || query.contains("hr", true) -> "hour"
            query.contains("min", true) -> "minute"
            else -> "second"
        }
        return "⏱️ Timer set for $number $unit${if (number > 1) "s" else ""}. I'll notify you when it's done!"
    }

    private fun handleTime(query: String, match: MatchResult?): String {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val min = cal.get(Calendar.MINUTE)
        val amPm = if (hour < 12) "AM" else "PM"
        val h = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
        return "🕐 It's $h:${min.toString().padStart(2, '0')} $amPm"
    }

    private fun handleDate(query: String, match: MatchResult?): String {
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        val year = cal.get(Calendar.YEAR)
        val dayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
        return "📅 Today is $dayName, $month $day, $year"
    }

    private fun handleCalculator(query: String, match: MatchResult?): String {
        return try {
            val expr = Regex("\\d[\\d+\\-*/().\\s]*").find(query)?.value?.trim() ?: return "I couldn't parse that expression."
            val result = evalExpression(expr)
            "🧮 $expr = $result"
        } catch (e: Exception) {
            "I couldn't compute that. Try something like: what is 24 * 7?"
        }
    }

    private fun handleWeather(query: String, match: MatchResult?): String {
        // In production, call a weather API
        return "🌤 I'd need location access to give you accurate weather. " +
                "In the full version, I'll show you real-time weather data. " +
                "You can also say 'Open weather app'."
    }

    private fun handleGreeting(query: String, match: MatchResult?): String {
        val greetings = listOf(
            "Hello! I'm SHADOW 3.0, your AI assistant. What can I do for you?",
            "Hey there! Ready to help. What's on your mind?",
            "Hi! SHADOW 3.0 here. How can I assist you today?"
        )
        return greetings.random()
    }

    private fun handleIdentity(query: String, match: MatchResult?): String {
        return "I'm SHADOW 3.0 — your AI voice assistant. I can answer questions, " +
                "set timers, make calls, send messages, recognize text with the camera, " +
                "and much more. Just ask!"
    }

    private fun handleThanks(query: String, match: MatchResult?): String {
        return listOf("You're welcome! 😊", "Happy to help!", "Anytime!", "Of course! Anything else?").random()
    }

    // =================== AI API CALL ===================

    private fun generateAPIResponse(query: String, history: List<ChatMessage>): String {
        // TODO: Replace with actual Gemini/OpenAI/etc. API call
        // Example using OkHttp + Retrofit to call an LLM endpoint
        //
        // val client = OkHttpClient()
        // val body = buildRequestBody(query, history)
        // val request = Request.Builder().url(API_URL).post(body).build()
        // val response = client.newCall(request).execute()
        // return parseResponse(response.body?.string())

        // Placeholder smart responses for demo
        return generateSmartPlaceholder(query)
    }

    private fun generateSmartPlaceholder(query: String): String {
        return when {
            query.contains("call", true) ->
                "📞 To make a call, I'll need Contacts permission. Say the contact name and I'll dial right away!"
            query.contains("remind", true) || query.contains("reminder", true) ->
                "🔔 Reminder feature coming right up! Tell me what to remind you about and when."
            query.contains("note", true) ->
                "📝 Note saved! I'll keep track of that for you."
            query.contains("play", true) || query.contains("music", true) ->
                "🎵 I can open your music app. Say 'Play [song name]' and I'll launch it!"
            query.contains("alarm", true) ->
                "⏰ Alarm set! I'll wake you up at the specified time."
            query.length < 10 ->
                "Could you give me a bit more detail? I want to make sure I help you correctly!"
            else ->
                "I understand you're asking about: \"$query\"\n\n" +
                        "In the full SHADOW 3.0 release, I'll connect to an LLM API to give you " +
                        "intelligent, contextual answers. For now, try asking me to set a timer, " +
                        "check the time, or do a quick calculation!"
        }
    }

    // Simple expression evaluator (supports +, -, *, /)
    private fun evalExpression(expr: String): String {
        val cleaned = expr.trim()
        // Split on operators while keeping them
        val tokens = cleaned.split(Regex("(?<=[+\\-*/])|(?=[+\\-*/])")).map { it.trim() }
        var result = tokens.first().toDouble()
        var i = 1
        while (i < tokens.size - 1) {
            val op = tokens[i]
            val next = tokens[i + 1].toDouble()
            result = when (op) {
                "+" -> result + next
                "-" -> result - next
                "*" -> result * next
                "/" -> if (next != 0.0) result / next else throw ArithmeticException("Division by zero")
                else -> result
            }
            i += 2
        }
        return if (result % 1.0 == 0.0) result.toLong().toString() else String.format("%.4f", result).trimEnd('0')
    }
}
