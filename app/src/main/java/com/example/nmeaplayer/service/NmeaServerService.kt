package com.example.nmeaplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.nmeaplayer.MainActivity
import com.example.nmeaplayer.R
import com.example.nmeaplayer.engine.PlaybackState
import com.example.nmeaplayer.server.NmeaServerRepository
import com.example.nmeaplayer.server.ServerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Foreground Service hosting and managing [NmeaServerRepository] playback and TCP server.
 * Ensures streaming continues uninterrupted when the app is backgrounded or minimized,
 * and presents an ongoing status notification with control actions (Pause/Resume, Exit).
 */
class NmeaServerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundWithNotification()
        observeAndPublishStatus()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> NmeaServerRepository.playbackEngine.pause()
            ACTION_PLAY -> NmeaServerRepository.playbackEngine.play()
            ACTION_EXIT -> {
                NmeaServerRepository.triggerAppExit()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_STOP -> {
                NmeaServerRepository.playbackEngine.stop()
                NmeaServerRepository.tcpServerManager.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        startForegroundWithNotification()
        return START_STICKY
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun observeAndPublishStatus() {
        val repo = NmeaServerRepository
        serviceScope.launch {
            combine(
                repo.tcpServerManager.serverState,
                repo.tcpServerManager.activeClientCount,
                repo.tcpServerManager.port,
                repo.playbackEngine.playbackState,
                repo.playbackEngine.currentTimeString
            ) { serverState, activeClients, port, playbackState, timeStr ->
                StatusData(serverState, activeClients, port, playbackState, timeStr)
            }.collect { status ->
                val notification = buildNotification(status)
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun startForegroundWithNotification() {
        try {
            val repo = NmeaServerRepository
            val status = StatusData(
                serverState = repo.tcpServerManager.serverState.value,
                activeClients = repo.tcpServerManager.activeClientCount.value,
                port = repo.tcpServerManager.port.value,
                playbackState = repo.playbackEngine.playbackState.value,
                timeStr = repo.playbackEngine.currentTimeString.value
            )
            val notification = buildNotification(status)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildNotification(status: StatusData): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (status.serverState) {
            ServerState.RUNNING -> getString(R.string.notification_title_running)
            ServerState.STARTING -> getString(R.string.notification_title_starting)
            ServerState.ERROR -> getString(R.string.notification_title_error)
            ServerState.STOPPED -> getString(R.string.notification_title_stopped)
        }

        val playbackText = when (status.playbackState) {
            PlaybackState.PLAYING -> getString(R.string.playback_playing)
            PlaybackState.PAUSED -> getString(R.string.playback_paused)
            PlaybackState.STOPPED -> getString(R.string.playback_stopped)
        }

        val text = getString(
            R.string.notification_content_format,
            status.port,
            status.activeClients,
            playbackText,
            status.timeStr
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)

        // Add Pause / Play Action
        if (status.playbackState == PlaybackState.PLAYING) {
            val pauseIntent = Intent(this, NmeaServerService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getService(
                this,
                1,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.action_pause),
                pausePendingIntent
            )
        } else {
            val playIntent = Intent(this, NmeaServerService::class.java).apply {
                action = ACTION_PLAY
            }
            val playPendingIntent = PendingIntent.getService(
                this,
                2,
                playIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_media_play,
                getString(R.string.action_resume),
                playPendingIntent
            )
        }

        // Add Exit Action
        val exitIntent = Intent(this, NmeaServerService::class.java).apply {
            action = ACTION_EXIT
        }
        val exitPendingIntent = PendingIntent.getService(
            this,
            3,
            exitIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            getString(R.string.exit_app),
            exitPendingIntent
        )

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private data class StatusData(
        val serverState: ServerState,
        val activeClients: Int,
        val port: Int,
        val playbackState: PlaybackState,
        val timeStr: String
    )

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "nmea_server_channel"

        const val ACTION_START = "com.example.nmeaplayer.ACTION_START"
        const val ACTION_PAUSE = "com.example.nmeaplayer.ACTION_PAUSE"
        const val ACTION_PLAY = "com.example.nmeaplayer.ACTION_PLAY"
        const val ACTION_STOP = "com.example.nmeaplayer.ACTION_STOP"
        const val ACTION_EXIT = "com.example.nmeaplayer.ACTION_EXIT"

        fun startService(context: Context) {
            try {
                val intent = Intent(context, NmeaServerService::class.java).apply {
                    action = ACTION_START
                }
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, NmeaServerService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
