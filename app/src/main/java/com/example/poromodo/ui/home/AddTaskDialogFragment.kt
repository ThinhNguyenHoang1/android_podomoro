package com.example.poromodo.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.poromodo.R
import com.example.poromodo.databinding.FragmentAddTaskDialogBinding

class AddTaskDialogFragment : Fragment() {
    private var _binding: FragmentAddTaskDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTaskDialogBinding.inflate(inflater, container, false)
        val root:View = binding.root
        val taskTitleLayout = binding.tilTaTitle
        val taskDescLayout = binding.tilTaDescription
        val taskNumPoroLayout = binding.tilTaNumberOfPodoReps

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}