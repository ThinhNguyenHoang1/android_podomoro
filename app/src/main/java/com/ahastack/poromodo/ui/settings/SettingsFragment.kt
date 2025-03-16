package com.ahastack.poromodo.ui.settings

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.ahastack.poromodo.databinding.FragmentSettingsBinding
import com.ahastack.poromodo.preferences.DataStoreManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private var selectedRingtoneTitle: String? = null
    private var selectedRingtoneUri: Uri? = null

    // Register Activity Result Launcher for Ringtone Picker
    private var ringtonePickerLauncher: ActivityResultLauncher<Intent>? = null

    private fun getSoundTrackTitleFromUri(uri: Uri): String {
        val ringtone = RingtoneManager.getRingtone(requireContext(), uri)
        return ringtone.getTitle(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ringtonePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == android.app.Activity.RESULT_OK) {
                    val uri: Uri? =
                        result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                    if (uri != null) {
                        selectedRingtoneUri = uri
                        binding.notificationSoundInput.text = getSoundTrackTitleFromUri(uri)
                    } else {
                        selectedRingtoneUri = null
                        selectedRingtoneTitle = null
                        binding.notificationSoundInput.text = "Select a ringtone"
                    }
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load preferences
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(state = Lifecycle.State.STARTED) {
                launch {
                    DataStoreManager.getPomodoroDuration(requireContext()).collectLatest {
                        binding.pomodoroDurationInput.setText(it.toString())
                    }
                }

                launch {
                    DataStoreManager.getBreakTime(requireContext()).collectLatest {
                        binding.breakTimeInput.setText(it.toString())
                    }
                }

                launch {
                    DataStoreManager.getLongBreakTime(requireContext()).collectLatest {
                        binding.breakLongTimeInput.setText(it.toString())
                    }
                }

                launch {
                    DataStoreManager.getNotificationSound(requireContext()).collectLatest {
                        val uri = it.toUri()
                        selectedRingtoneUri = uri
                        val title = getSoundTrackTitleFromUri(uri)
                        Log.d("KAPPA", "onViewCreated: TITLE: $title")
                        binding.notificationSoundInput.text = title
                    }
                }
            }
        }
        // Listener
        val soundSpinner = binding.notificationSoundInput
        val defaultRingtoneUri =
            RingtoneManager.getActualDefaultRingtoneUri(activity, RingtoneManager.TYPE_RINGTONE)
        val defaultRingtone = RingtoneManager.getRingtone(activity, defaultRingtoneUri)
        val defaultRingtoneName = defaultRingtone.getTitle(activity)
        binding.notificationSoundChangeBtn.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Sound")
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(
                    RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                )
                // If there’s a previously selected URI, pass it to pre-select in the picker
                selectedRingtoneUri?.let {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it)
                }
            }
            ringtonePickerLauncher?.launch(intent)
        }
        binding.saveButton.setOnClickListener {
            val podomoroDuration = binding.pomodoroDurationInput.text.toString().toInt()
            val breakDuration = binding.breakTimeInput.text.toString().toInt()
            val longBreakDuration = binding.breakLongTimeInput.text.toString().toInt()
            // Validation
            when {
                podomoroDuration == 0 -> {
                    binding.pomodoroDurationLayout.error = "Enter valid duration"
                    return@setOnClickListener
                }

                breakDuration == 0 -> {
                    binding.breakTimeLayout.error = "Enter valid duration"
                    return@setOnClickListener
                }

                longBreakDuration == 0 -> {
                    binding.breakLongTimeLayout.error = "Enter valid duration"
                    return@setOnClickListener
                }

                else -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        launch {
                            DataStoreManager.savePomodoroDuration(
                                requireContext(),
                                podomoroDuration
                            )
                        }

                        launch {
                            DataStoreManager.saveBreakTime(requireContext(), breakDuration)
                        }

                        launch {
                            DataStoreManager.saveLongBreakTime(requireContext(), longBreakDuration)
                        }

                        launch {
                            DataStoreManager.saveNotificationSound(
                                requireContext(),
                                selectedRingtoneUri.toString()
                            )
                        }

                        withContext(Dispatchers.Main) {
                            Snackbar.make(view, "Setting Saved!", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            // Save

        }
    }
}