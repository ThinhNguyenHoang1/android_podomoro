package com.example.poromodo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.example.poromodo.preferences.DataStoreManager

class PomodoroService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var selectedRingtoneUri: Uri? = null
    private var countdownTimer: CountDownTimer? = null
    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    companion object {
        const val CHANNEL_ID = "PomodoroChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_RINGTONE_URI = "EXTRA_RINGTONE_URI"
        const val EXTRA_DURATION = "EXTRA_DURATION"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d("KKKK", "onCreate: ")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("KKKK", "onStart: ${intent.toString()}")
        when (intent?.action) {
            ACTION_START -> {
                val duration = intent.getLongExtra(EXTRA_DURATION, 25 * 60 * 1000) // Default 25 minutes
                selectedRingtoneUri = intent.getStringExtra(EXTRA_RINGTONE_URI)?.toUri()
                startPomodoro(duration)
            }
            ACTION_STOP -> stopPomodoro()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pomodoro Timer",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun startPomodoro(duration: Long) {
        Log.d("KKKK", "SERVICE:startPodomoro: $duration")
        countdownTimer?.cancel()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        countdownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 60000
                val seconds = (millisUntilFinished % 60000) / 1000
                val timeLeft = String.format("%02d:%02d", minutes, seconds)

                val notification = NotificationCompat.Builder(this@PomodoroService, CHANNEL_ID)
                    .setContentTitle("Pomodoro Running")
                    .setContentText("Time remaining: $timeLeft")
                    .setSmallIcon(R.drawable.ic_clock) // Add your timer icon
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build()

                startForeground(NOTIFICATION_ID, notification)
            }

            override fun onFinish() {
                playRingtone()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }.start()
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
                    selectedRingtoneUri ?: android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
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

    private fun stopPomodoro() {
        countdownTimer?.cancel()
        mediaPlayer?.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        countdownTimer?.cancel()
        mediaPlayer?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}