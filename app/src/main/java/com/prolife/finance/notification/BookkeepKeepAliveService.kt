package com.prolife.finance.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.prolife.finance.R
import com.prolife.finance.data.SessionStore

/**
 * 前台保活服务 — 让进程保持"前台"优先级，
 * 防止国产 ROM 冻结后台进程导致 onNotificationPosted 回调延迟。
 */
class BookkeepKeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bookkeep)
            .setContentText("\u81EA\u52A8\u8BB0\u8D26\u76D1\u542C\u4E2D")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "\u81EA\u52A8\u8BB0\u8D26\u4FDD\u6D3B",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                description = "\u4FDD\u6301\u8BB0\u8D26\u76D1\u542C\u8FD0\u884C"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "bookkeep_keep_alive"
        private const val NOTIFICATION_ID = 2001
        private const val ACTION_STOP = "com.prolife.finance.ACTION_STOP_KEEP_ALIVE"

        fun start(context: Context) {
            val intent = Intent(context, BookkeepKeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BookkeepKeepAliveService::class.java)
                .setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
