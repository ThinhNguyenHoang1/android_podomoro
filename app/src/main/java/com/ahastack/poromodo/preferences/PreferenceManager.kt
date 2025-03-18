package com.ahastack.poromodo.preferences

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ahastack.poromodo.model.PomodoroPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

// Extension property to create a DataStore instance
val Context.dataStore by preferencesDataStore(name = "pomodoro_settings")
const val PODOMORO_DEFAULT_DURATION_MIN = 25
const val BREAK_DEFAULT_DURATION_MIN = 5
const val LONG_BREAK_DEFAULT_DURATION_MIN = 15

const val TERMINAL_FOCUS_TASK_ID = -1L

var POMODORO_CYCLE =
    arrayOf<PomodoroPhase>(
        PomodoroPhase.PODOMORO,
        PomodoroPhase.BREAK,
        PomodoroPhase.PODOMORO,
        PomodoroPhase.BREAK,
        PomodoroPhase.PODOMORO,
        PomodoroPhase.BREAK,
        PomodoroPhase.PODOMORO,
        PomodoroPhase.BREAK,
        PomodoroPhase.LONG_BREAK
    )

data class Settings(
    val podomoroDuration: Int = PODOMORO_DEFAULT_DURATION_MIN,
    val breakTime: Int = BREAK_DEFAULT_DURATION_MIN,
    val longBreakTime: Int = LONG_BREAK_DEFAULT_DURATION_MIN,
    val notiSoundTrack: String
)

object DataStoreManager {
    // Settings preference keys
    private val POMODORO_DURATION_KEY = intPreferencesKey("pomodoro_duration") // In minutes
    private val BREAK_TIME_KEY = intPreferencesKey("break_time") // In minutes
    private val BREAK_LONG_TIME_KEY = intPreferencesKey("long_break_time") // In minutes
    private val NOTIFICATION_SOUND_KEY =
        stringPreferencesKey("notification_sound") // Sound file name

    // Runtime Datastore keys
    private val CURRENT_PODOMORO_CYCLE_INDEX =
        intPreferencesKey("POMODORO_CYCLE_INDEX")
    private val CURRENT_FOCUSED_TASK_ID = longPreferencesKey("FOCUSED_TASK")
    private val TIME_REMAINING = intPreferencesKey("TIME_REMAINING")
    private val IS_RUNNING = booleanPreferencesKey("IS_RUNNING")


    private var timerJob: Job? = null
    private val _timeRemaining = MutableStateFlow<Int>(0)
    val timerState: StateFlow<Int> = _timeRemaining.asStateFlow()

    private fun cancelTickingJob() {
        timerJob?.cancel()
        timerJob = null // Reset the job reference
    }

    // TODO: Maybe bug in concurreny. As the timerJob maybe set before the edit update the values
    suspend fun startTheClock(context: Context) {
        cancelTickingJob()
        context.dataStore.edit { p ->
            p[IS_RUNNING] = true
            _timeRemaining.value = p[TIME_REMAINING] ?: 0
        }

        timerJob = CoroutineScope(Dispatchers.Default).launch {
            flow {
                while (_timeRemaining.value > 0) {
                    delay(1000)
                    emit(Unit)
                }
            }.collect {
                if (_timeRemaining.value > 0)
                    _timeRemaining.value = _timeRemaining.value - 1
            }
        }
    }

    suspend fun stopTheClock(context: Context) {
        cancelTickingJob()
        context.dataStore.edit { p ->
            p[IS_RUNNING] = false
        }
        saveTimeRemaining(context, _timeRemaining.value)
    }

    suspend fun saveTimeRemaining(context: Context, v: Int) {
        context.dataStore.edit { preferences ->
            preferences[TIME_REMAINING] = v
        }
    }

    fun getIsRunning(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { p ->
            p[IS_RUNNING] == true
        }
    }

    fun getCurrentCyclePhase(context:Context): Flow<PomodoroPhase> {
        return context.dataStore.data.map { p ->
            val cIdx = p[CURRENT_PODOMORO_CYCLE_INDEX] ?: 0
            val ph = POMODORO_CYCLE[cIdx]
            ph
        }
    }

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

    fun getPomodoroCurrentCycleIndex(context: Context): Flow<Int> {
        return context.dataStore.data
            .map { preferences ->
                val ph = preferences[CURRENT_PODOMORO_CYCLE_INDEX] ?: 0
                if (ph > POMODORO_CYCLE.size) 0 else ph
            }
    }

    fun getFocusedTaskId(context: Context): Flow<Long> {
        return context.dataStore.data
            .map { preferences ->
                preferences[CURRENT_FOCUSED_TASK_ID] ?: TERMINAL_FOCUS_TASK_ID
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

    suspend fun savePomodoroCycleIndex(context: Context, index: Int) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_PODOMORO_CYCLE_INDEX] = index
            val s = getDurationFromPomodoroIndex(preferences, index)
            preferences[TIME_REMAINING] = s
        }
    }

    suspend fun advanceCycle(context: Context) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_PODOMORO_CYCLE_INDEX] =
                ((preferences[CURRENT_PODOMORO_CYCLE_INDEX] ?: 0) + 1) % POMODORO_CYCLE.size
        }
    }

    fun getDurationFromPomodoroIndex(p: MutablePreferences, idx: Int?): Int {
        val cIdx = idx ?: 0
        val ph = POMODORO_CYCLE[cIdx]
        val d = when (ph) {
            PomodoroPhase.PODOMORO -> p[POMODORO_DURATION_KEY]
            PomodoroPhase.BREAK -> p[BREAK_TIME_KEY]
            PomodoroPhase.LONG_BREAK -> p[BREAK_LONG_TIME_KEY]
        }
        val s = if (d == null) 0 else d * 60
        _timeRemaining.value = s
        return s
    }



    suspend fun switchToNextDesiredPhase(context: Context, phase: PomodoroPhase) {
        context.dataStore.edit { preferences ->
            val curr = preferences[CURRENT_PODOMORO_CYCLE_INDEX] ?: 0
            val s = POMODORO_CYCLE.size
            // This loop may not find a podomoro phase if we are currently after the last short break
            for (idx in (curr + 1) % s..s - 1) {
                if (POMODORO_CYCLE[idx] == phase) {
                    preferences[CURRENT_PODOMORO_CYCLE_INDEX] = idx
                    val s = getDurationFromPomodoroIndex(preferences, idx)
                    preferences[TIME_REMAINING] = s
                    return@edit
                }
            }

            // Therefore: We must Wrap around
            for (idx in 0..s - 1) {
                if (POMODORO_CYCLE[idx] == phase) {
                    preferences[CURRENT_PODOMORO_CYCLE_INDEX] = idx
                    val s = getDurationFromPomodoroIndex(preferences, idx)
                    preferences[TIME_REMAINING] = s
                    return@edit
                }
            }
        }
    }

    suspend fun saveFocusTaskId(context: Context, taskId: Long) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_FOCUSED_TASK_ID] = taskId
        }
    }
}