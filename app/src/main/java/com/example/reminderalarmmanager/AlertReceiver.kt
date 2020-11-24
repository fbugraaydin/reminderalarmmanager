package com.example.reminderalarmmanager

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlertReceiver:BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        val notificationManager = NotificationManagerCompat.from(context!!)
        val id = intent?.extras?.getInt("id")
        val detail = intent?.extras?.getString("detail")
        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle(detail)
                .setContentText("$id - $detail")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                //.setVibrate(longArrayOf( 1000, 1000, 1000, 1000, 1000))
                //.setLights(Color.RED, 3000, 3000)
                .setDefaults(Notification.DEFAULT_ALL)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .build()

        notificationManager.notify(200,notification)
    }
}