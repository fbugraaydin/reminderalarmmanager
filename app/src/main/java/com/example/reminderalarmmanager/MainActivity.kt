package com.example.reminderalarmmanager

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private var selectedRepeatType: Int = 0
    private lateinit var time: TextView
    private lateinit var datePickerButton: Button
    private lateinit var cancelAlarmButton: Button
    private lateinit var repeatTimeSelectButton: Button
    private lateinit var coordinatorLayout:CoordinatorLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        time = findViewById(R.id.timeTextView)
        datePickerButton = findViewById(R.id.datePickerButton)
        cancelAlarmButton = findViewById(R.id.cancelAlarmButton)
        repeatTimeSelectButton = findViewById(R.id.repeatingTimeButton)
        coordinatorLayout = findViewById(R.id.coordinatorLayout)

        createNotificationChannel()
        listeners()
    }

    private fun listeners() {
        datePickerButton.setOnClickListener {
            val materialTimePicker: MaterialTimePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H).build()
            materialTimePicker.show(supportFragmentManager, "time_picker_fragment")

            materialTimePicker.addOnPositiveButtonClickListener {
                val hour: Int = materialTimePicker.hour
                val minute: Int = materialTimePicker.minute
                onTimeSet(hour, minute)
            }
        }

        repeatTimeSelectButton.setOnClickListener {

            MaterialAlertDialogBuilder(this)
                .setTitle("Select Frequency Time")
                .setItems(REPEAT_TYPES) { dialog, which ->
                    this.selectedRepeatType = which
                    startAlarm(null, true)
                }
                .show()
        }

        cancelAlarmButton.setOnClickListener {
            val availableAlarmObjectList = getArrayList(ALARM_STORE_KEY)!!
            val availableAlarmList =
                availableAlarmObjectList.map { "${it.id} - ${it.detail}" }.toTypedArray()

            MaterialAlertDialogBuilder(this)
                .setTitle("Select Alarm to cancel.")
                .setItems(availableAlarmList) { dialog, which ->
                    val selectedAlarm = availableAlarmObjectList[which]
                    cancelAlarm(selectedAlarm)
                }
                .show()
        }
    }

    private fun onTimeSet(hour: Int, minute: Int) {
        val cal = Calendar.getInstance()
        cal[Calendar.HOUR_OF_DAY] = hour
        cal[Calendar.MINUTE] = minute
        cal[Calendar.SECOND] = 0

        startAlarm(cal,false)
    }

    private fun startAlarm(cal: Calendar?,isRepeatable: Boolean) {
        val alarmList = this.getArrayList(ALARM_STORE_KEY)
        val alarmId = if(alarmList.isNullOrEmpty()) 0 else alarmList.last().id + 1
        val detail:String?

        val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, AlertReceiver::class.java)

        if(isRepeatable){
            var baseTime = 0L
            if(selectedRepeatType == 0 || selectedRepeatType == 1 || selectedRepeatType == 2){
                baseTime = SystemClock.elapsedRealtime()
            }else if(selectedRepeatType == 3){
                val curCal = Calendar.getInstance()
                curCal[Calendar.MINUTE] = 0
                curCal[Calendar.SECOND] = 0
                baseTime =  curCal.timeInMillis
            }

            val frequency = this.getFrequencyType()
            detail = REPEAT_TYPES[selectedRepeatType]
            intent.putExtra("id",alarmId)
            intent.putExtra("detail",detail)
            val pendingIntent = PendingIntent.getBroadcast(this, alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                baseTime + frequency,
                frequency,
                pendingIntent
            )

        }else{
            if (cal!!.before(Calendar.getInstance())) {
                cal.add(Calendar.DATE, 1)
            }
            detail = DateFormat.getTimeInstance(DateFormat.SHORT).format(cal.time)

            intent.putExtra("id",alarmId)
            intent.putExtra("detail",detail)
            val pendingIntent = PendingIntent.getBroadcast(this, alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
        }

        saveAlarm(Alarm(id = alarmId,detail = detail!!))
    }

    private fun cancelAlarm(alarm:Alarm) {
        val alarmManager =
            getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlertReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, alarm.id, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        alarmManager.cancel(pendingIntent)

        var alarmList = getArrayList(ALARM_STORE_KEY)
        if(alarmList.isNullOrEmpty()){
            alarmList = ArrayList()
        }
        alarmList.remove(alarm)
        saveArrayList(alarmList, ALARM_STORE_KEY)

        time.text = getString(R.string.amount_of_alarms,alarmList.size)
        Snackbar.make(coordinatorLayout, getString(R.string.cancel_alarm_info,alarm.detail), Snackbar.LENGTH_LONG).show()
    }

    private fun getFrequencyType():Long{
        return when(selectedRepeatType){
            0 -> 1 * 1000
            1 -> AlarmManager.INTERVAL_FIFTEEN_MINUTES
            2 -> AlarmManager.INTERVAL_HALF_HOUR
            3 -> AlarmManager.INTERVAL_HOUR
            else -> throw RuntimeException("Couldn't find frequency type")
        }
    }

    private fun createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notificationChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH)

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun saveAlarm(alarm:Alarm){
        var alarmList = getArrayList(ALARM_STORE_KEY)
        if(alarmList.isNullOrEmpty()){
            alarmList = ArrayList()
        }
        alarmList.add(alarm)
        saveArrayList(alarmList, ALARM_STORE_KEY)

        Snackbar.make(coordinatorLayout, getString(R.string.set_alarm_info,alarm.detail), Snackbar.LENGTH_LONG).show()
        time.text = getString(R.string.amount_of_alarms,alarmList.size)
    }

    private fun saveArrayList(list: List<Alarm>?, key: String?) {
        val prefs: SharedPreferences = getPreferences(Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val json: String = Gson().toJson(list)
        editor.putString(key, json)
        editor.apply()
    }

    private fun getArrayList(key: String?): MutableList<Alarm>? {
        val prefs: SharedPreferences = getPreferences(Context.MODE_PRIVATE)
        val json: String? = prefs.getString(key, null)
        val type: Type = object : TypeToken<MutableList<Alarm>?>() {}.type
        return Gson().fromJson(json, type)
    }

    data class Alarm(val id:Int, val detail:String)

}