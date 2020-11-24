package com.example.reminderalarmmanager

import android.app.AlarmManager

const val CHANNEL_ID: String = "ReminderChannelId"
const val CHANNEL_NAME: String = "ReminderChannelName"

const val ALARM_STORE_KEY: String = "AlarmIds"

val REPEAT_TYPES = arrayOf("Every Minutes", "Every 15 Minutes", "Every 30 Minutes", "Hourly")

fun getFrequencyType(repeatType: Int) = when (repeatType) {
    0 -> 1 * 60 * 1000
    1 -> AlarmManager.INTERVAL_FIFTEEN_MINUTES
    2 -> AlarmManager.INTERVAL_HALF_HOUR
    3 -> AlarmManager.INTERVAL_HOUR
    else -> throw RuntimeException("Couldn't find frequency type")
}
