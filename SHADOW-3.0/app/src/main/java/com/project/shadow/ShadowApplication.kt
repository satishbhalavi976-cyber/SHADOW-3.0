package com.project.shadow

import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager

/**
 * SHADOW 3.0 Application Class
 * Initializes global app state: theme, preferences, logging
 */
class ShadowApplication : Application() {

    companion object {
        const val VERSION = "3.0"
        const val PREF_THEME = "theme_mode"
        const val PREF_WAKE_WORD = "wake_word_enabled"
        const val PREF_TTS_SPEED = "tts_speed"
        const val PREF_TTS_PITCH = "tts_pitch"
        const val PREF_FIRST_LAUNCH = "first_launch"
        const val THEME_DARK = "dark"
        const val THEME_LIGHT = "light"
        const val THEME_SYSTEM = "system"

        lateinit var instance: ShadowApplication
            private set
    }

    lateinit var prefs: SharedPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        applyTheme()
    }

    private fun applyTheme() {
        when (prefs.getString(PREF_THEME, THEME_DARK)) {
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun setTheme(theme: String) {
        prefs.edit().putString(PREF_THEME, theme).apply()
        applyTheme()
    }
}
