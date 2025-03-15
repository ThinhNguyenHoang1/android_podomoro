package com.example.poromodo

import android.app.Application
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poromodo.model.AppDatabase
import com.example.poromodo.model.PomodoroPhase
import com.example.poromodo.model.Task
import com.example.poromodo.preferences.BREAK_DEFAULT_DURATION_MIN
import com.example.poromodo.preferences.DataStoreManager
import com.example.poromodo.preferences.LONG_BREAK_DEFAULT_DURATION_MIN
import com.example.poromodo.preferences.PODOMORO_DEFAULT_DURATION_MIN
import com.example.poromodo.preferences.POMODORO_CYCLE
import com.example.poromodo.preferences.TERMINAL_FOCUS_TASK_ID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.time.ZonedDateTime


class MainViewModel(application: Application) :
    AndroidViewModel(application) {
    private val _phase = MutableStateFlow(PomodoroPhase.PODOMORO)
    val phase: StateFlow<PomodoroPhase> = _phase

    private val _expandedStates = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val expandedStates: StateFlow<Map<Long, Boolean>> = _expandedStates


    private val _pomodoroDuration =
        MutableStateFlow(PODOMORO_DEFAULT_DURATION_MIN * 60) // Default in seconds
    val pomodoroDuration: StateFlow<Int> = _pomodoroDuration

    private val _breakTime = MutableStateFlow(BREAK_DEFAULT_DURATION_MIN * 60) // Default in seconds
    val breakTime: StateFlow<Int> = _breakTime

    private val _longBreakTime =
        MutableStateFlow(LONG_BREAK_DEFAULT_DURATION_MIN * 60) // Default in seconds
    val longBreakTime: StateFlow<Int> = _longBreakTime

    private val _notificationSoundUri =
        MutableStateFlow(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
    val notificationSoundUri: StateFlow<Uri> = _notificationSoundUri

    private val _timeRemaining = MutableStateFlow(_pomodoroDuration.value)
    val timeRemaining: StateFlow<Int> = _timeRemaining

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning


    // Job to manage the timer coroutine
    private var timerJob: Job? = null

    private val taskDao by lazy { AppDatabase.getDatabase(application).taskDao() }

    val progress = combine(
        _timeRemaining,
        _pomodoroDuration,
        _breakTime,
        _longBreakTime,
        _phase
    ) { timeLeft, podoDur, breakDur, longBreakDur, currPhase ->
        val totalTime = when (currPhase) {
            PomodoroPhase.PODOMORO -> podoDur
            PomodoroPhase.BREAK -> breakDur
            PomodoroPhase.LONG_BREAK -> longBreakDur
        }
        (1 - timeLeft.toDouble() / totalTime.toDouble()) * 100
    }

    private val _focusedTask: MutableStateFlow<Task?> = MutableStateFlow(null)
    val focusedTask: StateFlow<Task?> = _focusedTask

    private val _taskToIncreasePodoCount =
        combine(_focusedTask, _timeRemaining, _phase) { task, time, phase ->
            if (time == 0 && task != null && phase == PomodoroPhase.PODOMORO && task.updatedAt.plusSeconds(
                    59
                ).isBefore(
                    ZonedDateTime.now()
                )
            ) {
                task
            } else null
        }.filterNotNull()

    init {
        // Load initial values from DataStore
        val context = application.applicationContext
        viewModelScope.launch {
            launch {
                combine(
                    DataStoreManager.getSettings(context),
                    DataStoreManager.getPomodoroCurrentCycleIndex(context)
                ) { s, p ->
                    Pair(s, p)
                }.collectLatest { pair ->
                    val settings = pair.first
                    val p = pair.second
                    _pomodoroDuration.emit(settings.podomoroDuration * 60)
                    _breakTime.emit(settings.breakTime * 60)
                    _longBreakTime.emit(settings.longBreakTime * 60)
                    _notificationSoundUri.value = Uri.parse(settings.notiSoundTrack)
                    val ph = POMODORO_CYCLE[p]
                    if (_phase.value != ph) {
                        _phase.emit(ph)
                        setPhase(ph)
                    }
                    when (ph) {
                        PomodoroPhase.PODOMORO -> _timeRemaining.emit(_pomodoroDuration.value)
                        PomodoroPhase.BREAK -> _timeRemaining.emit(_breakTime.value)
                        PomodoroPhase.LONG_BREAK -> _timeRemaining.emit(_longBreakTime.value)
                    }
                }
            }

            launch {
                DataStoreManager.getFocusedTaskId(context).collectLatest { tid ->
                    taskDao.getTaskById(tid).collectLatest { task ->
                        _focusedTask.emit(task)
                    }
                }
            }

            launch {
                _taskToIncreasePodoCount.collectLatest {
                    if (it.numOfPodomoroSpend < it.numOfPodomoroToComplete) {
                        updateTask(it.copy(numOfPodomoroSpend = it.numOfPodomoroSpend + 1))
                    }
                }
            }

        }
    }

    fun tasks(): Flow<List<Task>> = taskDao.getTasksOrderByCreatedDate()
    fun delTask(t: Task) {
        viewModelScope.launch {
            taskDao.deleteTask(t)
        }
    }

    fun upsertTask(t: Task) {
        viewModelScope.launch {
            taskDao.upsertTask(t)
        }
    }

    fun updateTask(t: Task) {
        viewModelScope.launch {
            Log.d("DELUXE", "NEW TASK: $t")
            taskDao.updateTask(t.copy(updatedAt = ZonedDateTime.now()))
        }
    }

    fun completeTask(t: Task) {
        val newTask = t.copy(numOfPodomoroSpend = t.numOfPodomoroToComplete)
        updateTask(newTask)
    }

    fun focusTask(t: Task) {
        viewModelScope.launch {
            DataStoreManager.saveFocusTaskId(getApplication(), t.taskId)
        }
    }

    fun unFocusTask(t: Task) {
        viewModelScope.launch {
            DataStoreManager.saveFocusTaskId(getApplication(), TERMINAL_FOCUS_TASK_ID)
        }
    }


    fun setExpandedState(taskId: Long, isExpanded: Boolean) {
        val currentStates = _expandedStates.value.toMutableMap()
        currentStates[taskId] = isExpanded
        _expandedStates.value = currentStates
    }

    fun resetExpandedStates(taskIds: List<Long>) {
        val newStates = taskIds.associateWith { false }
        _expandedStates.value = newStates
    }

    fun setPhase(phase: PomodoroPhase) {
        if (_phase.value != phase) {
            cancelTickingJob()
            _isRunning.value = false
            viewModelScope.launch {
                DataStoreManager.switchToNextDesiredPhase(getApplication(), phase)
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
                    _timeRemaining.emit(_timeRemaining.value - 1)
                }
                _isRunning.emit(false)
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