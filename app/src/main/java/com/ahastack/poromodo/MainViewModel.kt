package com.ahastack.poromodo

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ahastack.poromodo.model.AppDatabase
import com.ahastack.poromodo.model.PomodoroPhase
import com.ahastack.poromodo.model.Session
import com.ahastack.poromodo.model.Task
import com.ahastack.poromodo.preferences.DataStoreManager
import com.ahastack.poromodo.preferences.Settings
import com.ahastack.poromodo.preferences.TERMINAL_FOCUS_TASK_ID
import com.ahastack.poromodo.preferences.TimerInstance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import java.time.ZonedDateTime


class MainViewModel(application: Application) :
    AndroidViewModel(application) {
    private val taskDao by lazy { AppDatabase.getDatabase(application).taskDao() }
    private val sessionDao by lazy { AppDatabase.getDatabase(application).sessionDao() }

    private val _expandedStates = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val expandedStates: StateFlow<Map<Long, Boolean>> = _expandedStates

    private val context = application


    val timeRemaining: StateFlow<Int> = DataStoreManager.timerState


    private val settings: StateFlow<Settings> = DataStoreManager.getSettings(application).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
    )
    val timerInstance: StateFlow<TimerInstance> =
        DataStoreManager.getTimerInstance(application).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TimerInstance()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val focusedTask: StateFlow<Task?> =
        DataStoreManager.getFocusedTaskId(application).transformLatest<Long, Task> {
            taskDao.getTaskById(it)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )


    @OptIn(ExperimentalCoroutinesApi::class)
    val progress = timerInstance.transformLatest<TimerInstance, Int> {
        (1 - it.timeLeft.toDouble() / it.timeTotal.toDouble()) * 100
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val isRunning: StateFlow<Boolean> = timerInstance.transformLatest<TimerInstance, Boolean> {
        it.isRunning
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )


    private val _taskToIncreasePodoCount = combine(focusedTask, timerInstance) { t, ti ->
        if (ti.timeLeft == 0 && !ti.isRunning && t != null && t.updatedAt.plusSeconds(59)
                .isBefore(ZonedDateTime.now())
        ) {
            t
        } else null
    }.filterNotNull()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _podoSetComplete = timerInstance.transformLatest<TimerInstance, Boolean> {
        it.timeLeft == 0 && it.currentPhase == PomodoroPhase.PODOMORO
    }

    init {
        viewModelScope.launch {
            // TASK_COMPLETE
            launch {
                _taskToIncreasePodoCount.collectLatest {
                    if (it.numOfPodomoroSpend < it.numOfPodomoroToComplete) {
                        val timeAdded = settings.value.podomoroDuration
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
                    val timeSpent = settings.value.podomoroDuration
                    val timeBreak = settings.value.breakTime
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

    fun unFocusTask() {
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
        if (timerInstance.value.currentPhase != phase) {
            viewModelScope.launch {
                DataStoreManager.switchToNextDesiredPhase(getApplication(), phase)
            }
        }
    }

    fun startTimer() {
        viewModelScope.launch {
            val intent = Intent(context, PomodoroService::class.java).apply {
                action = PomodoroService.ACTION_START
            }
            DataStoreManager.startTheClock(getApplication())
            context.startForegroundService(intent)
        }
    }

    fun pauseTimer() {
        viewModelScope.launch {
            DataStoreManager.stopTheClock(getApplication())
        }
    }

}