package com.mysafe.escale_demo.tools

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat

/***
 * Android11以上启用前端服务通用notification提供者
 */
object ForegroundProvider {

    fun startForeground(
        contextService: Service,
        channelId: String,
        channelName: String,
        notificationId: Int
    ) {
        val chId = createNotificationChannel(contextService, channelId, channelName)
        val builder = NotificationCompat.Builder(contextService, chId)
        val notification = builder.setOngoing(true).setSmallIcon(android.R.mipmap.sym_def_app_icon)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        contextService.startForeground(notificationId, notification)
    }

    private fun createNotificationChannel(
        contextService: Service,
        channelId: String,
        channelName: String
    ): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
            chan.lightColor = Color.GREEN
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val service =
                contextService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(chan)
            channelId
        } else {
            ""
        }
    }

}