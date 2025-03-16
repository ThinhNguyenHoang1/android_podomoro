package com.example.poromodo.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poromodo.model.AppDatabase
import com.example.poromodo.model.WeeklyUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatViewModel(application: Application) : AndroidViewModel(application) {
    private val taskDao by lazy { AppDatabase.getDatabase(application).taskDao() }
    private val sessionDao by lazy { AppDatabase.getDatabase(application).sessionDao() }

    private val _weeklyUsage = MutableStateFlow<List<WeeklyUsage>>(emptyList())
    val weeklyUsage: StateFlow<List<WeeklyUsage>> = _weeklyUsage.asStateFlow()

    private val _averageDailyUsage = MutableStateFlow(0f)
    val averageDailyUsage: StateFlow<Float> = _averageDailyUsage.asStateFlow()

    private val _averageDailyVisits = MutableStateFlow(0f)
    val averageDailyVisits: StateFlow<Float> = _averageDailyVisits.asStateFlow()

    private val _todayVisits = MutableStateFlow(0)
    val todayVisits: StateFlow<Int> = _todayVisits.asStateFlow()

    private val _todayUsage = MutableStateFlow(0)
    val todayUsage: StateFlow<Int> = _todayUsage.asStateFlow()

    private val _weeklyUsageTotal = MutableStateFlow(0)
    val weeklyUsageTotal: StateFlow<Int> = _weeklyUsageTotal.asStateFlow()

    private val _monthlyUsageTotal = MutableStateFlow(0)
    val monthlyUsageTotal: StateFlow<Int> = _monthlyUsageTotal.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                _weeklyUsage.value = sessionDao.getWeeklyUsage()
                _averageDailyUsage.value = sessionDao.getAverageDailyUsage()
                _averageDailyVisits.value = sessionDao.getAverageDailyVisits()
                _todayVisits.value = sessionDao.getTodayVisits()
                _todayUsage.value = sessionDao.getTodayUsage()
                _weeklyUsageTotal.value = sessionDao.getWeeklyUsageTotal()
                _monthlyUsageTotal.value = sessionDao.getMonthlyUsageTotal()
            }
        }
    }
}