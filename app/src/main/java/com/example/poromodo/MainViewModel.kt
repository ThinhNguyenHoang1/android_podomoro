package com.example.poromodo

import android.app.Application
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poromodo.preferences.BREAK_DEFAULT_DURATION_MIN
import com.example.poromodo.preferences.DataStoreManager
import com.example.poromodo.preferences.LONG_BREAK_DEFAULT_DURATION_MIN
import com.example.poromodo.preferences.PODOMORO_DEFAULT_DURATION_MIN
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class PoromodoPhase {PODOMORO, BREAK, LONG_BREAK}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _phase = MutableStateFlow(PoromodoPhase.PODOMORO)
    val phase: StateFlow<PoromodoPhase> = _phase

    private val _pomodoroDuration = MutableStateFlow(PODOMORO_DEFAULT_DURATION_MIN * 60) // Default in seconds
    val pomodoroDuration: StateFlow<Int> = _pomodoroDuration

    private val _breakTime = MutableStateFlow(BREAK_DEFAULT_DURATION_MIN * 60) // Default in seconds
    val breakTime: StateFlow<Int> = _breakTime

    private val _longBreakTime = MutableStateFlow(LONG_BREAK_DEFAULT_DURATION_MIN * 60) // Default in seconds
    val longBreakTime: StateFlow<Int> = _longBreakTime

    private val _notificationSoundUri = MutableStateFlow(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
    val notificationSoundUri: StateFlow<Uri> = _notificationSoundUri

    private val _timeRemaining = MutableStateFlow(PODOMORO_DEFAULT_DURATION_MIN) // Default to Pomodoro duration in seconds
    val timeRemaining: StateFlow<Int> = _timeRemaining

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning


    // Job to manage the timer coroutine
    private var timerJob: Job? = null

    init {
        // Load initial values from DataStore
        val context = application.applicationContext
        viewModelScope.launch {
            Log.d("CHUNGUS", "HIHIHI: ${isRunning.value} ${timeRemaining.value}")
            DataStoreManager.getSettings(context).collectLatest { settings ->
                _pomodoroDuration.value = settings.podomoroDuration * 60
                _breakTime.value = settings.breakTime * 60
                _longBreakTime.value = settings.longBreakTime * 60
                _notificationSoundUri.value = Uri.parse(settings.notiSoundTrack)
                setPhase(PoromodoPhase.PODOMORO)
            }
        }
    }

    fun setPhase(phase: PoromodoPhase) {
        if (_phase.value != phase) {
            _phase.value = phase
            _isRunning.value = false
            cancelTickingJob()
            when (phase) {
                PoromodoPhase.PODOMORO -> _timeRemaining.value = _pomodoroDuration.value
                PoromodoPhase.BREAK -> _timeRemaining.value = _breakTime.value
                PoromodoPhase.LONG_BREAK -> _timeRemaining.value = _longBreakTime.value
            }
        }
    }

    fun startTimer() {
        if (!_isRunning.value) {
            _isRunning.value = true
            cancelTickingJob()
            timerJob = viewModelScope.launch {
                while (_timeRemaining.value > 0 && _isRunning.value) {
                    kotlinx.coroutines.delay(1000)
                    _timeRemaining.value -= 1
                }
                _isRunning.value = false
                when (_phase.value) {
                    PoromodoPhase.PODOMORO -> _timeRemaining.value = _pomodoroDuration.value
                    PoromodoPhase.BREAK -> _timeRemaining.value = _breakTime.value
                    PoromodoPhase.LONG_BREAK -> _timeRemaining.value = _longBreakTime.value
                }
            }
        }
    }

    fun pauseTimer() {
        cancelTickingJob()
        _isRunning.value = false
    }

    fun resetTimer() {
        _isRunning.value = false
        _timeRemaining.value = _pomodoroDuration.value
    }

    private fun cancelTickingJob() {
        timerJob?.cancel()
        timerJob = null // Reset the job reference
    }

    override fun onCleared() {
        super.onCleared()
        cancelTickingJob()
        // Cleanup if needed
    }

}