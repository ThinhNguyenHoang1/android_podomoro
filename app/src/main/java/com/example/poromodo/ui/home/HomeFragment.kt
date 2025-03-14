package com.example.poromodo.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.poromodo.MainViewModel
import com.example.poromodo.PoromodoPhase
import com.example.poromodo.R
import com.example.poromodo.databinding.FragmentHomeBinding
import com.example.poromodo.model.AppDatabase
import com.example.poromodo.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(), AddTaskDialogFragment.OnTaskAddedListener, UpdateTaskDialogFragment.OnTaskUpdatedListener {

    private val vm: MainViewModel by activityViewModels()
    private var _binding: FragmentHomeBinding? = null
    private lateinit var taskAdapter: TaskRecyclerViewAdapter

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

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressBar: ProgressBar = binding.statusProgressBar
        val button: Button = binding.podomoroButton
        val timeLeftTv: TextView = binding.tvTimeLeft
        val phaseToggleGroup = binding.phaseToggleGroup
        val taskAddBtn: Button = binding.btnTaskAdd
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

        taskAdapter = TaskRecyclerViewAdapter { task, action ->
            Log.d("Menu Action", "onTaskHandle: $task $action")
            when (action) {
                TaskRecyclerViewAdapter.MenuAction.EDIT -> {
                    val dialog = UpdateTaskDialogFragment(task)
                    dialog.setOnTaskUpdatedListener(this)
                    dialog.show(childFragmentManager, "EditTaskDialog")
                }
                TaskRecyclerViewAdapter.MenuAction.DELETE -> {
                    val index = taskList.indexOfFirst { it.taskId == task.taskId }
                    if (index != -1) {
                        taskList.removeAt(index)
                        taskAdapter.submitList(taskList.toList())
                        lifecycleScope.launch {
                            taskDao.deleteTask(task.taskId)
                        }
                    }
                }
                TaskRecyclerViewAdapter.MenuAction.MARK_COMPLETE -> {
                    val updatedTask = task.copy(numOfPomodorosCompleted = task.numOfPomodorosToComplete)
                    val index = taskList.indexOfFirst { it.taskId == task.taskId }
                    if (index != -1) {
                        taskList[index] = updatedTask
                        taskAdapter.submitList(taskList.toList())
                        lifecycleScope.launch {
                            taskDao.insertTask(updatedTask)
                        }
                    }
                }
            }
        }
        binding.rclvTaskList.layoutManager = LinearLayoutManager(requireContext())
        binding.rclvTaskList.adapter = taskAdapter

        // Add Task Button
        taskAddBtn.setOnClickListener {
            val dialog = AddTaskDialogFragment()
            Log.d("CHUNGUS", " DIALOG CREATE")
            dialog.setOnTaskAddedListener(this)
            dialog.show(childFragmentManager, "AddTaskDialog")
        }

        button.setOnClickListener {
            if (!vm.isRunning.value) {
                vm.startTimer()
            } else {
                vm.pauseTimer()
            }
        }

        // Load tasks from Room database
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.timeRemaining.collectLatest {
                        progressBar.progress = it
                        timeLeftTv.text = formatTime(it)
                        Log.d("DELUXE", "onCreateView: TIMEREMANINGIS: $it")
                    }
                }
                launch {
                    vm.tasks().collect {
                        taskAdapter.submitList(it)
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
                            PoromodoPhase.PODOMORO -> {
                                phaseToggleGroup.check(R.id.phase_pomodoro)
                            }

                            PoromodoPhase.BREAK -> {
                                phaseToggleGroup.check(R.id.phase_short_break)
                            }

                            PoromodoPhase.LONG_BREAK -> {
                                phaseToggleGroup.check(R.id.phase_long_break)
                            }
                        }
                    }
                }

                launch {
                    vm.timeRemaining.collectLatest {
                        progressBar.progress = it
                        timeLeftTv.text = formatTime(it)
                    }
                }

            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onTaskAdded(task: Task) {
        Log.d("CHUNGUS", "onTaskAdded: $task")
        CoroutineScope(Dispatchers.IO).launch {
            val taskId = AppDatabase.getDatabase(requireContext()).taskDao().upsertTask(task)
        }
    }

    override fun onTaskUpdated(task: Task) {
        CoroutineScope(Dispatchers.IO).launch {
            val taskId = AppDatabase.getDatabase(requireContext()).taskDao().upsertTask(task)
        }
    }
}