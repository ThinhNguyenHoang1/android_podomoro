package com.example.poromodo.ui.stats

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.poromodo.databinding.FragmentStatBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatFragment : Fragment() {
    private var _binding: FragmentStatBinding? = null
    private val binding get() = _binding!!

    private val vm: StatViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatBinding.inflate(inflater, container, false)

        return binding.root
    }

    fun calPercent(p: Int, m: Int): Int {
        return (p.toDouble() / m.toDouble() * 100).toInt()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.weeklyUsage.collectLatest { listUsageByDow ->
                        try {
                            val maxUsage =
                                listUsageByDow.maxBy { it -> it.totalMinutes }.totalMinutes
                            binding.pbMon.progress =
                                calPercent(
                                    vm.weeklyUsage.value.getOrNull(0)?.totalMinutes ?: 0,
                                    maxUsage
                                )
                            binding.pbTues.progress =
                                calPercent(
                                    vm.weeklyUsage.value.getOrNull(1)?.totalMinutes ?: 0,
                                    maxUsage
                                )
                            binding.pbWed.progress =
                                calPercent(
                                    vm.weeklyUsage.value.getOrNull(2)?.totalMinutes ?: 0,
                                    maxUsage
                                )
                            binding.pbThurs.progress =
                                calPercent(
                                    vm.weeklyUsage.value.getOrNull(3)?.totalMinutes ?: 0,
                                    maxUsage
                                )
                            binding.pbFri.progress =
                                calPercent(
                                    vm.weeklyUsage.value.getOrNull(4)?.totalMinutes ?: 0,
                                    maxUsage
                                )
                            binding.pbSat.progress =
                                calPercent(
                                    vm.weeklyUsage.value.getOrNull(5)?.totalMinutes ?: 0,
                                    maxUsage
                                )
                            binding.pbSun.progress =
                                calPercent(
                                    vm.weeklyUsage.value.getOrNull(6)?.totalMinutes ?: 0,
                                    maxUsage
                                )
                        } catch (e: NoSuchElementException) {
                            Log.e("HUHUHU", "$e")
                        }
                    }
                }
                launch {
                    vm.averageDailyUsage.collectLatest {
                        binding.tvAvgWeekRep.text = it.toInt().toString()
                    }
                }
                launch {
                    vm.averageDailyVisits.collectLatest {
                        binding.tvAvgWeekTime.text = it.toInt().toString()
                    }
                }
                launch {
                    vm.todayUsage.collectLatest {
                        binding.tvTimeTotayToday.text = it.toInt().toString()
                    }
                }
                launch {
                    vm.weeklyUsageTotal.collectLatest {
                        binding.tvTimeTotalWeek.text = it.toInt().toString()
                    }
                }

                launch {
                    vm.monthlyUsageTotal.collectLatest {
                        binding.tvTimeTotalMonth.text = it.toInt().toString()
                    }
                }

            }
        }
    }
}