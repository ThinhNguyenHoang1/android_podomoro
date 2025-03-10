package com.example.poromodo.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property to create a DataStore instance
val Context.dataStore by preferencesDataStore(name = "pomodoro_settings")
const val PODOMORO_DEFAULT_DURATION_MIN = 25
const val BREAK_DEFAULT_DURATION_MIN = 5
const val LONG_BREAK_DEFAULT_DURATION_MIN = 15

data class Settings(
    val podomoroDuration: Int = PODOMORO_DEFAULT_DURATION_MIN,
    val breakTime: Int = BREAK_DEFAULT_DURATION_MIN,
    val longBreakTime: Int = LONG_BREAK_DEFAULT_DURATION_MIN,
    val notiSoundTrack: String
)

object DataStoreManager {
    // Define preference keys
    private val POMODORO_DURATION_KEY = intPreferencesKey("pomodoro_duration") // In minutes
    private val BREAK_TIME_KEY = intPreferencesKey("break_time") // In minutes
    private val BREAK_LONG_TIME_KEY = intPreferencesKey("long_break_time") // In minutes
    private val NOTIFICATION_SOUND_KEY =
        stringPreferencesKey("notification_sound") // Sound file name

    fun getSettings(context: Context): Flow<Settings> {
        return context.dataStore.data.map { preferences ->
            Settings(
                podomoroDuration = preferences[POMODORO_DURATION_KEY]
                    ?: PODOMORO_DEFAULT_DURATION_MIN,
                breakTime = preferences[BREAK_TIME_KEY] ?: BREAK_DEFAULT_DURATION_MIN,
                longBreakTime = preferences[BREAK_LONG_TIME_KEY] ?: LONG_BREAK_DEFAULT_DURATION_MIN,
                notiSoundTrack = preferences[NOTIFICATION_SOUND_KEY] ?: ""
            )
        }
    }

    fun getPomodoroDuration(context: Context): Flow<Int> {
        return context.dataStore.data
            .map { preferences ->
                preferences[POMODORO_DURATION_KEY]
                    ?: PODOMORO_DEFAULT_DURATION_MIN // Default to 25 minutes
            }
    }

    fun getBreakTime(context: Context): Flow<Int> {
        return context.dataStore.data
            .map { preferences ->
                preferences[BREAK_TIME_KEY] ?: BREAK_DEFAULT_DURATION_MIN // Default to 5 minutes
            }
    }

    fun getLongBreakTime(context: Context): Flow<Int> {
        return context.dataStore.data
            .map { preferences ->
                preferences[BREAK_LONG_TIME_KEY]
                    ?: LONG_BREAK_DEFAULT_DURATION_MIN // Default to 5 minutes
            }
    }

    // Get notification sound as Flow
    fun getNotificationSound(context: Context): Flow<String> {
        return context.dataStore.data
            .map { preferences ->
                preferences[NOTIFICATION_SOUND_KEY] ?: "default_sound.mp3" // Default sound
            }
    }

    // Save Pomodoro duration
    suspend fun savePomodoroDuration(context: Context, duration: Int) {
        context.dataStore.edit { preferences ->
            preferences[POMODORO_DURATION_KEY] = duration
        }
    }

    // Save break time
    suspend fun saveBreakTime(context: Context, breakTime: Int) {
        context.dataStore.edit { preferences ->
            preferences[BREAK_TIME_KEY] = breakTime
        }
    }

    suspend fun saveLongBreakTime(context: Context, breakTime: Int) {
        context.dataStore.edit { preferences ->
            preferences[BREAK_LONG_TIME_KEY] = breakTime
        }
    }

    // Save notification sound
    suspend fun saveNotificationSound(context: Context, sound: String) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_SOUND_KEY] = sound
        }
    }
}