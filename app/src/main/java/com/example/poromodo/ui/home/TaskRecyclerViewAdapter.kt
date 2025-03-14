package com.example.poromodo.ui.home

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.poromodo.MainViewModel
import com.example.poromodo.databinding.ItemUserTaskBinding
import com.example.poromodo.model.Task

class TaskRecyclerViewAdapter(
    private val viewModel: MainViewModel,
    private val onMenuAction: (Task, MenuAction) -> Unit,
) :
    ListAdapter<Task, TaskRecyclerViewAdapter.TaskViewHolder>(DiffCallback) {

    enum class MenuAction {
        EDIT, DELETE, MARK_COMPLETE
    }

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

        return TaskViewHolder(binding, onMenuAction)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(
        binding: ItemUserTaskBinding,
        menuCallback: (Task, MenuAction) -> Unit
    ) :
        RecyclerView.ViewHolder(binding.root) {
        private val titleTextView: TextView = binding.tvTaskTitle
        private val descriptionTextView: TextView = binding.tvTaskDescription
        private val progressIcon: ImageView = binding.ivProgressIcon
        private val progressTv: TextView = binding.tvTaskProgress
        private val taskMenuBtn = binding.btnTaskMenu
        private val itemCallback = menuCallback
        private val expandIcon = binding.expandIcon

        fun bind(task: Task) {
            titleTextView.text = task.title
            descriptionTextView.text = task.description
            progressTv.text = "${task.numOfPodomoroSpend} / ${task.numOfPodomoroToComplete}"
            // Set initial expanded state from ViewModel
            val isExpanded = viewModel.expandedStates.value[task.taskId] == true
            descriptionTextView.visibility = if (isExpanded) View.VISIBLE else View.GONE
            expandIcon.rotation = if (isExpanded) 180f else 0f

            // Toggle expansion on click
            expandIcon.setOnClickListener {
                val newState = !isExpanded
                viewModel.setExpandedState(task.taskId, newState)
                notifyItemChanged(adapterPosition)
            }
            // Set up the menu button
            taskMenuBtn.setOnClickListener { view ->
                val popupMenu = PopupMenu(view.context, view)
                popupMenu.menu.add(0, 0, 0, "Edit")
                popupMenu.menu.add(0, 1, 1, "Delete")
                popupMenu.menu.add(0, 2, 2, "Mark Complete")
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        0 -> itemCallback(task, MenuAction.EDIT)
                        1 -> itemCallback(task, MenuAction.DELETE)
                        2 -> itemCallback(task, MenuAction.MARK_COMPLETE)
                    }
                    true
                }
                popupMenu.show()
            }
        }
    }
}