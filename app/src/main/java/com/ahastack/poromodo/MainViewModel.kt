package com.ahastack.poromodo

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ahastack.poromodo.model.AppDatabase
import com.ahastack.poromodo.model.PomodoroPhase
import com.ahastack.poromodo.model.Session
import com.ahastack.poromodo.model.Task
import com.ahastack.poromodo.preferences.BREAK_DEFAULT_DURATION_MIN
import com.ahastack.poromodo.preferences.DataStoreManager
import com.ahastack.poromodo.preferences.LONG_BREAK_DEFAULT_DURATION_MIN
import com.ahastack.poromodo.preferences.PODOMORO_DEFAULT_DURATION_MIN
import com.ahastack.poromodo.preferences.POMODORO_CYCLE
import com.ahastack.poromodo.preferences.TERMINAL_FOCUS_TASK_ID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import androidx.core.net.toUri


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
    private val sessionDao by lazy { AppDatabase.getDatabase(application).sessionDao() }

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

    private val _podoSetComplete = combine(_timeRemaining, _phase) { t, p ->
        if (t == 0 && p == PomodoroPhase.PODOMORO) {
            true
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
                    _notificationSoundUri.value = settings.notiSoundTrack.toUri()
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

            // TASK_COMPLETE
            launch {
                _taskToIncreasePodoCount.collectLatest {
                    if (it.numOfPodomoroSpend < it.numOfPodomoroToComplete) {
                        val timeAdded = _pomodoroDuration.value
                        val timeSpent = it.totalTimeSpent + timeAdded
                        updateTask(
                            it.copy(
                                numOfPodomoroSpend = it.numOfPodomoroSpend + 1,
                                totalTimeSpent = timeSpent
                            )
                        )
                    }
                }
            }


            // PODO_COMPLETE
            launch {
                _podoSetComplete.collectLatest {
                    val timeSpent = _pomodoroDuration.value
                    val timeBreak = _pomodoroDuration.value
                    val now = ZonedDateTime.now()
                    val startFrom = now.plusSeconds(-timeSpent.toLong())
                    val s = Session(
                        timeFocus = timeSpent,
                        timeBreak = timeBreak,
                        startAt = startFrom,
                        endAt = now
                    )
                    sessionDao.upsertSession(s)
                }
            }

            // A Phase complete
            launch {
               _timeRemaining.collectLatest {
                   if (it <= 0) {
                       playRingtone()
                       DataStoreManager.advanceCycle(application)
                   }
               }
            }

        }
    }
    fun playRingtone() {
        try {
            val mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(
                    getApplication(),
                    _notificationSoundUri.value ?: android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
                )
                isLooping = false
                prepare()
                start()
                setOnCompletionListener { it.release() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
                    delay(1000)
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