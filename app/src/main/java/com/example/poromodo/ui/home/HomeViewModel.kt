package com.example.poromodo.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.poromodo.model.PoromodoClock
import com.example.poromodo.model.Status
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

class HomeViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    // Define ViewModel factory in a companion object
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                HomeViewModel(
                    savedStateHandle = savedStateHandle
                )
            }
        }
    }

    private var timer = Timer()

    private val _clockData = MutableLiveData<PoromodoClock>().apply {
        val p: PoromodoClock = PoromodoClock(60, 60, Status.INIT)
        value = p
    }
    val clockData: LiveData<PoromodoClock> = _clockData
    fun windNewTimer() {
        timer.cancel()
        timer.purge()
        val nTimer = Timer()
        nTimer.scheduleAtFixedRate(0L, 1000L) {
            trollTick()
        }
        this.timer = nTimer
    }
    fun start() {
        _clockData.value = _clockData.value.also {
            if (it != null) {
                it.state = Status.RUNNING
                windNewTimer()
            }
        }
    }

    fun resume() {
        _clockData.value = _clockData.value.also {
            if (it != null) {
                windNewTimer()
                it.state = Status.RUNNING
            }
        }
    }

    fun reset() {
        _clockData.value = _clockData.value.also {
            if (it != null) {
                it.state = Status.RUNNING
                it.timeLeft = it.timeTotal
            }
        }
        start()
    }

    fun stop() {
        timer.cancel()
        _clockData.value = _clockData.value.also {
            if (it != null) {
                it.state = Status.PAUSED
            }
        }
    }

    fun complete() {
        _clockData.postValue(
            _clockData.value.also {
                if (it != null) {
                    it.timeLeft = 0
                    it.state = Status.COMPLETED
                }
            }
        )
    }

    fun trollTick() {
        Log.d("DEBUG", "trollTick: ${this._clockData.value}")
        _clockData.postValue(
            _clockData.value.also {
                if (it != null) {
                    it.timeLeft -= 1
                    if (it.timeLeft <= 0) {
                        complete()
                    }
                }
            }
        )
    }
}
