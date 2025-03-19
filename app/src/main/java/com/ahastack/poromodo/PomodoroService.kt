package com.ahastack.poromodo

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ahastack.poromodo.model.PomodoroPhase
import com.ahastack.poromodo.preferences.DataStoreManager
import com.ahastack.poromodo.preferences.POMODORO_CYCLE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch

class PomodoroService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
    private var isStarted = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "PomodoroChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_SKIP = "ACTION_SKIP"
        const val EXTRA_RINGTONE_URI = "EXTRA_RINGTONE_URI"
        const val EXTRA_DURATION = "EXTRA_DURATION"
    }

    override fun onCreate() {
        super.onCreate()
        createServiceNotificationChannel()
        Log.d("KKKK", "onCreate: ")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("KKKK", "onStart: ${intent}")
        if (!isStarted) {
            isStarted = true
        }
        makeForeground()

        when (intent?.action) {
            ACTION_START -> {
            }

            ACTION_SKIP -> {

            }
        }
        return START_STICKY
    }

    private fun createServiceNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pomodoro Timer",
            NotificationManager.IMPORTANCE_HIGH

        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        notificationManager.createNotificationChannel(channel)
    }


    private fun makeForeground() {
        Log.d("KKKK", "LAUNCHING NOTI")
        scope.launch {
            val timeLeft = DataStoreManager.timerState.value
            val timeTotal = DataStoreManager.getCurrentClockMaxTime(this@PomodoroService).last()
            val notification: Notification =
                NotificationCompat.Builder(this@PomodoroService, CHANNEL_ID)
                    .setContentTitle("Pomodoro Timer")
                    .setContentText("$timeLeft / $timeTotal")
                    .setSmallIcon(R.drawable.ic_pomo_foreground)
                    .build()
            startForeground(NOTIFICATION_ID, notification)
        }
    }


    private fun playRingtone() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(
                    this@PomodoroService,
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
                )
                isLooping = false
                prepare()
                start()
                setOnCompletionListener { it.release() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}