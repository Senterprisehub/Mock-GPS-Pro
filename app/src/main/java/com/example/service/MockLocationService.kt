package com.example.service

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
import com.example.MainActivity
import java.util.Locale

class MockLocationService : Service() {

    companion object {
        const val CHANNEL_ID = "mock_gps_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.mockgps.ACTION_START"
        const val ACTION_STOP = "com.example.mockgps.ACTION_STOP"
        const val ACTION_UPDATE = "com.example.mockgps.ACTION_UPDATE"

        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"
        const val EXTRA_SPEED = "extra_speed"

        private var isRunning = false
        private var serviceStartTimeMs: Long = 0L

        fun startService(context: Context, lat: Double, lng: Double, speed: Float) {
            try {
                if (serviceStartTimeMs == 0L) {
                    serviceStartTimeMs = System.currentTimeMillis()
                }
                val intent = Intent(context, MockLocationService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_LAT, lat)
                    putExtra(EXTRA_LNG, lng)
                    putExtra(EXTRA_SPEED, speed)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Ignore foreground service start restrictions
            }
        }

        fun updateNotification(context: Context, lat: Double, lng: Double, speed: Float) {
            try {
                val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (notifManager != null && isRunning) {
                    val notif = createNotification(context, lat, lng, speed)
                    notifManager.notify(NOTIFICATION_ID, notif)
                }
            } catch (e: Exception) {
                // Ignore background notification exceptions
            }
        }

        fun stopService(context: Context) {
            try {
                serviceStartTimeMs = 0L
                val intent = Intent(context, MockLocationService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                // Ignore
            }
        }

        private fun createNotification(context: Context, lat: Double, lng: Double, speed: Float): Notification {
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val stopIntent = Intent(context, MockLocationService::class.java).apply {
                action = ACTION_STOP
            }
            val stopPendingIntent = PendingIntent.getService(
                context,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val contentText = String.format(
                Locale.US,
                "Mocking Lat: %.6f, Lng: %.6f | %.0f km/h",
                lat, lng, speed
            )

            val startTime = if (serviceStartTimeMs > 0L) serviceStartTimeMs else System.currentTimeMillis()

            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Mock GPS Pro Active")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setWhen(startTime)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(android.R.drawable.ic_media_pause, "Stop Mocking", stopPendingIntent)
                .build()
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        try {
            createNotificationChannel()
            val initialNotification = createNotification(this, 0.0, 0.0, 0f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(
                        NOTIFICATION_ID,
                        initialNotification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                } catch (e: Exception) {
                    startForeground(NOTIFICATION_ID, initialNotification)
                }
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP) {
            isRunning = false
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (e: Exception) {
                // Ignore
            }
            stopSelf()
            return START_NOT_STICKY
        }

        val lat = intent?.getDoubleExtra(EXTRA_LAT, 0.0) ?: 0.0
        val lng = intent?.getDoubleExtra(EXTRA_LNG, 0.0) ?: 0.0
        val speed = intent?.getFloatExtra(EXTRA_SPEED, 0f) ?: 0f

        try {
            val notification = createNotification(this, lat, lng, speed)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Ignore
        }

        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mock Location Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live mock location injection status"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}

