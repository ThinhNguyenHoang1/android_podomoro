package com.ahastack.poromodo.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.ahastack.poromodo.R
import com.ahastack.poromodo.databinding.FragmentUpdateTaskDialogBinding

import com.ahastack.poromodo.model.Task
import java.time.ZonedDateTime

class UpdateTaskDialogFragment(task: Task) : DialogFragment() {
    private var _binding: FragmentUpdateTaskDialogBinding? = null
    private val binding get() = _binding!!
    private var listener: OnTaskUpdatedListener? = null
    private val oldTask: Task = task

    interface OnTaskUpdatedListener {
        fun onTaskUpdated(task: Task)
    }

    fun setOnTaskUpdatedListener(listener: OnTaskUpdatedListener) {
        this.listener = listener
    }


    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            val attributes = window.attributes
            attributes.dimAmount = 0.5f
            window.attributes = attributes
            dialog?.findViewById<View>(R.id.fmdialog_add_task)?.setPadding(32, 32, 32, 32)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdateTaskDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tilTaTitle.editText?.setText(oldTask.title)
        binding.tilTaDescription.editText?.setText(oldTask.description)
//        binding.tilTaNumberOfPodoReps.editText?.setText(oldTask.numOfPodomoroToComplete)
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            val title = binding.tilTaTitle.editText?.text.toString()
            val description = binding.tilTaDescription.editText?.text.toString()
            val pomodorosText = binding.tilTaNumberOfPodoReps.editText?.text.toString()

            val pomodoros = pomodorosText.toIntOrNull()

            when {
                title.isBlank() -> {
                    binding.tilTaTitle.error = "Title cannot be empty"
                    return@setOnClickListener
                }

                pomodoros == null || pomodoros <= 0 -> {
                    binding.tilTaNumberOfPodoReps.error = "Enter a valid number of Pomodoros (> 0)"
                    return@setOnClickListener
                }

                else -> {
                    binding.tilTaTitle.error = null
                    binding.tilTaNumberOfPodoReps.error = null

                    val task = oldTask.copy(
                        title = title,
                        description = description,
                        numOfPodomoroToComplete = pomodoros,
                        totalTimeSpent = 0,
                        createdAt = ZonedDateTime.now(),
                    )
                    listener?.onTaskUpdated(task)
                    dismiss()
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}