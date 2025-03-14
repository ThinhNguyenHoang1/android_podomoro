package com.example.poromodo.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.poromodo.databinding.ItemUserTaskBinding
import com.example.poromodo.model.Task

class TaskRecyclerViewAdapter(private val onTaskClicked: (Task) -> Unit) :
    ListAdapter<Task, TaskRecyclerViewAdapter.TaskViewHolder>(DiffCallback) {

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Task>() {
            override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
                return oldItem.taskId == newItem.taskId
            }

            override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding =
            ItemUserTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TaskViewHolder(binding: ItemUserTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        private val titleTextView: TextView = binding.tvTaskTitle
        private val descriptionTextView: TextView = binding.tvTaskDescription
        private val progressIcon: ImageView = binding.ivProgressIcon
        private val progressTv: TextView = binding.tvTaskProgress
        private val editTaskBtn = binding.btnTaskEdit

        fun bind(task: Task) {
            titleTextView.text = task.title
            descriptionTextView.text = task.description
            progressTv.text = "Completed: ${task.totalTimeSpent} / ${task.numOfPodomoroToComplete}"
        }
    }
}