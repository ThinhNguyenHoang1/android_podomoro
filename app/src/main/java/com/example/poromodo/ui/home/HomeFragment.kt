package com.example.poromodo.ui.home

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.poromodo.MainViewModel
import com.example.poromodo.PoromodoPhase
import com.example.poromodo.R
import com.example.poromodo.databinding.FragmentHomeBinding
import com.example.poromodo.model.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private val vm: MainViewModel by activityViewModels()
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    fun formatTime(seconds: Int): String {
        val minutes = seconds / 60 // Integer division to get minutes
        val remainingSeconds = seconds % 60 // Modulo to get remaining seconds
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val progressBar: ProgressBar = binding.statusProgressBar
        val button: Button = binding.podomoroButton
        val timeLeftTv: TextView = binding.tvTimeLeft
        val phaseToggleGroup = binding.phaseToggleGroup
        phaseToggleGroup.check(R.id.phase_toggle_group)
        phaseToggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.phase_pomodoro -> vm.setPhase(PoromodoPhase.PODOMORO)
                    R.id.phase_short_break -> vm.setPhase(PoromodoPhase.BREAK)
                    R.id.phase_long_break -> vm.setPhase(PoromodoPhase.LONG_BREAK)
                }
            }
        }


        CoroutineScope(Dispatchers.Main).launch {
            launch {
                vm.timeRemaining.collectLatest {
                    progressBar.progress = it
                    timeLeftTv.text = formatTime(it)
                    Log.d("DELUXE", "onCreateView: TIMEREMANINGIS: ${it.toString()}")
                }
            }

            launch {
                vm.isRunning.collectLatest {
                    button.text = if (it) "PAUSE" else "START"
                }
            }

            launch {
                vm.phase.collectLatest {
                    when (it) {
                        PoromodoPhase.PODOMORO -> phaseToggleGroup.check(R.id.phase_pomodoro)
                        PoromodoPhase.BREAK -> phaseToggleGroup.check(R.id.phase_short_break)
                        PoromodoPhase.LONG_BREAK -> phaseToggleGroup.check(R.id.phase_long_break)
                    }
                    val color = when (it) {
                        PoromodoPhase.PODOMORO -> Color.RED
                        PoromodoPhase.BREAK -> Color.YELLOW
                        PoromodoPhase.LONG_BREAK -> Color.GREEN
                    }
                    binding.fmHome.setBackgroundColor(color)
                }
            }

            launch {
                vm.pomodoroDuration.collectLatest {
                    progressBar.max = it
                    if (!vm.isRunning.value) {
                        progressBar.progress = it
                        timeLeftTv.text = formatTime(it)
                    }
                }
            }
            launch {
                vm.breakTime.collectLatest {
                    progressBar.max = it
                    if (!vm.isRunning.value) {
                        progressBar.progress = it
                        timeLeftTv.text = formatTime(it)
                    }
                }
            }

            launch {
                vm.longBreakTime.collectLatest {
                    progressBar.max = it
                    if (!vm.isRunning.value) {
                        progressBar.progress = it
                        timeLeftTv.text = formatTime(it)
                    }
                }
            }
        }

        button.setOnClickListener {
            Log.d("CHUNGUS", "onCreateView: ${vm.isRunning.value}")
            if (!vm.isRunning.value) {
                vm.startTimer()
            } else {
                vm.pauseTimer()
            }
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}