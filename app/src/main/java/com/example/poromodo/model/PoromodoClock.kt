package com.example.poromodo.model

enum class Status {
    INIT, PAUSED, RUNNING, COMPLETED
}

data class PoromodoClock(
    var timeLeft: Int = 0,
    var timeTotal: Int = 60,
    var state: Status = Status.INIT,
)