package com.example.reminderalarmmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlertReceiver:BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        val notificationManager = NotificationManagerCompat.from(context!!)

        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Title")
                .setContentText("22:10, Title Continues..")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

        notificationManager.notify(200,notification)
    }
}