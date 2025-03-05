package com.example.poromodo.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.poromodo.databinding.FragmentHomeBinding
import com.example.poromodo.model.Status

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val progressBar: ProgressBar = binding.statusProgressBar
        val button: Button = binding.podomoroButton
        val timeLeftTv: TextView = binding.tvTimeLeft
        val timeTotalTv: TextView = binding.tvTimeTotal

        progressBar.setOnClickListener {
            Toast.makeText(this.context, "Tik Tok", Toast.LENGTH_LONG).show()
            homeViewModel.trollTick()
        }
        homeViewModel.clockData.observe(viewLifecycleOwner) {
            timeLeftTv.text = it.timeLeft.toString() + "__"  + it.state.toString()
            timeTotalTv.text = it.timeTotal.toString()
            progressBar.progress = (it.timeLeft / it.timeTotal) * 100
            button.text = when (it.state) {
                Status.INIT -> "Start"
                Status.PAUSED -> "Resume"
                Status.RUNNING -> "Stop"
                Status.COMPLETED -> "Restart"
            }
        }

        button.setOnClickListener {
            val clock = homeViewModel.clockData.value
            if (clock == null) {
                Toast.makeText(this.context, "Chungus Null Clock", Toast.LENGTH_LONG).show()
            } else {
                when (clock.state) {
                    Status.INIT -> {
                        homeViewModel.start()
                    }

                    Status.PAUSED -> {
                        homeViewModel.resume()
                    }

                    Status.RUNNING -> {
                        homeViewModel.stop()
                    }

                    Status.COMPLETED -> {
                        homeViewModel.reset()
                    }
                }
            }
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}