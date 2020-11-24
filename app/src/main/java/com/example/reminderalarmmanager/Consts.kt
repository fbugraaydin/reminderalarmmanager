package com.example.reminderalarmmanager

import android.app.AlarmManager

const val CHANNEL_ID: String = "ReminderChannelId"
const val CHANNEL_NAME: String = "ReminderChannelName"

const val ALARM_STORE_KEY: String = "AlarmIds"

enum class RepeatType(val id:Int, val value:String,val intervalMilis:Long){
    EVERY_MINUTE(0,"Every Minute",1 * 60 * 1000),
    EVERY_FIFTEEN_MINUTES(1,"Every 15 Minutes",AlarmManager.INTERVAL_FIFTEEN_MINUTES),
    EVERY_THIRTY_MINUTES(2,"Every 30 Minutes",AlarmManager.INTERVAL_HALF_HOUR),
    HOURLY(3,"Hourly",AlarmManager.INTERVAL_HOUR);

    companion object {
        fun getById(id: Int?): RepeatType? = values().find { it.id == id }
        fun labels():List<String> = values().map { it.value }
    }
}