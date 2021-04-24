package com.example.reminderalarmmanager

import android.app.*
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private var selectedRepeatType: RepeatType? = null
    private lateinit var time: TextView
    private lateinit var datePickerButton: Button
    private lateinit var cancelAlarmButton: Button
    private lateinit var repeatTimeSelectButton: Button
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var alarmTitleEditText:TextInputEditText

    val OVERLAY_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(checkPermission()){
            setContentView(R.layout.activity_main)
            initializeComponents()
            createNotificationChannel()
            listeners()
        }else{
            grantPermission()
        }
    }

    private fun initializeComponents() {
        time = findViewById(R.id.timeTextView)
        datePickerButton = findViewById(R.id.datePickerButton)
        cancelAlarmButton = findViewById(R.id.cancelAlarmButton)
        repeatTimeSelectButton = findViewById(R.id.repeatingTimeButton)
        coordinatorLayout = findViewById(R.id.coordinatorLayout)
        alarmTitleEditText = findViewById(R.id.alarmDescriptionEditText)

        val alarmList = getArrayList(ALARM_STORE_KEY)
        showAmountOfAlarms(alarmList?.size?:0)
    }

    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                return false
            }
            return true
        }
        return true
    }

    private fun grantPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                if ("xiaomi" == Build.MANUFACTURER.toLowerCase(Locale.ROOT)) {
                    val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
                    intent.setClassName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.permissions.PermissionsEditorActivity"
                    )
                    intent.putExtra("extra_pkgname", packageName)
                    AlertDialog.Builder(this)
                        .setTitle("Please Enable the additional permissions")
                        .setMessage("You will not receive notifications while the app is in background if you disable these permissions")
                        .setPositiveButton("Go to Settings",
                            DialogInterface.OnClickListener { dialog, which -> startActivity(intent) })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setCancelable(false)
                        .show()
                } else {
                    val overlaySettings = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivityForResult(overlaySettings, OVERLAY_REQUEST_CODE)
                }
            }
        }
    }

    private fun listeners() {
        datePickerButton.setOnClickListener {
            val materialTimePicker: MaterialTimePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H).build()
            materialTimePicker.show(supportFragmentManager, "time_picker_fragment")

            materialTimePicker.addOnPositiveButtonClickListener {
                val hour: Int = materialTimePicker.hour
                val minute: Int = materialTimePicker.minute
                onTimeSet(hour, minute)
            }
        }

        repeatTimeSelectButton.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.repeat_time_dialog_title)
                .setItems(RepeatType.labels().toTypedArray()) { dialog, which ->
                    this.selectedRepeatType = RepeatType.getById(which)
                    startRepeatAlarm()
                }
                .show()
        }

        cancelAlarmButton.setOnClickListener {
            val availableAlarmObjectList = getArrayList(ALARM_STORE_KEY)
            if(!availableAlarmObjectList.isNullOrEmpty()){

                val availableAlarmList =
                    availableAlarmObjectList.map {
                        if(it.title.isBlank()){
                            "${it.id} - ${it.detail}"
                        }else{
                            "${it.id} - ${it.title} - ${it.detail}"
                        }
                    }.toTypedArray()

                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.cancel_alarm_dialog_title)
                    .setItems(availableAlarmList) { dialog, which ->
                        val selectedAlarm = availableAlarmObjectList[which]
                        cancelAlarm(selectedAlarm)
                    }
                    .show()
            }
        }
    }

    private fun onTimeSet(hour: Int, minute: Int) {
        val cal = Calendar.getInstance()
        cal[Calendar.HOUR_OF_DAY] = hour
        cal[Calendar.MINUTE] = minute
        cal[Calendar.SECOND] = 0

        startExactAlarm(cal)
    }

    /**
     * Sets an alarm for exact time.
     */
    private fun startExactAlarm(cal: Calendar?) {
        val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmId = getNextAlarmId()

        if (cal!!.before(Calendar.getInstance())) {
            cal.add(Calendar.DATE, 1)
        }
        val detail = DateFormat.getTimeInstance(DateFormat.SHORT).format(cal.time)

        val pendingIntent = createIntent(alarmId, detail)

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)

        saveAlarm(Alarm(id = alarmId, title = alarmTitleEditText.text.toString(), detail = detail))
        alarmTitleEditText.text = null
    }

    /**
     * Sets an alarm for triggering periodically
     */
    private fun startRepeatAlarm() {
        val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmId = getNextAlarmId()

        val repeatTime = selectedRepeatType!!.intervalMilis
        val cal = Calendar.getInstance()
        if (selectedRepeatType == RepeatType.HOURLY) { // to trigger at o'clock
            cal[Calendar.MINUTE] = 0
            cal[Calendar.SECOND] = 0
        }
        val baseTime = cal.timeInMillis

        val startTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(baseTime)
        val detail = selectedRepeatType!!.value + " - Start Time:$startTime"

        val pendingIntent = createIntent(alarmId, detail)

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            baseTime + repeatTime,
            repeatTime,
            pendingIntent
        )

        saveAlarm(Alarm(id = alarmId, title = alarmTitleEditText.text.toString(), detail = detail))
        alarmTitleEditText.text = null
    }

    private fun createIntent(alarmId: Int, detail: String?): PendingIntent? {
        val intent = Intent(this, AlertReceiver::class.java)
        intent.putExtra("id", alarmId)
        intent.putExtra("title", alarmTitleEditText.text.toString())
        intent.putExtra("detail", detail)
        return PendingIntent.getBroadcast(this, alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getNextAlarmId(): Int {
        val alarmList = this.getArrayList(ALARM_STORE_KEY)
        return if (alarmList.isNullOrEmpty()) 0 else alarmList.last().id + 1
    }

    private fun cancelAlarm(alarm: Alarm) {
        val alarmManager =
            getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlertReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(this, alarm.id, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        alarmManager.cancel(pendingIntent)

        removeAlarm(alarm)
    }

    private fun removeAlarm(alarm: Alarm) {
        var alarmList = getArrayList(ALARM_STORE_KEY)
        if (alarmList.isNullOrEmpty()) {
            alarmList = ArrayList()
        }
        alarmList.remove(alarm)
        saveArrayList(alarmList, ALARM_STORE_KEY)

        showAmountOfAlarms(alarmList.size)
        Snackbar.make(
            coordinatorLayout,
            getString(R.string.cancel_alarm_info, alarm.title),
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun saveAlarm(alarm: Alarm) {
        var alarmList = getArrayList(ALARM_STORE_KEY)
        if (alarmList.isNullOrEmpty()) {
            alarmList = ArrayList()
        }
        alarmList.add(alarm)

        saveArrayList(alarmList, ALARM_STORE_KEY)

        showAmountOfAlarms(alarmList.size)
        Snackbar.make(
            coordinatorLayout,
            getString(R.string.set_alarm_info, alarm.detail),
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun showAmountOfAlarms(amount:Int) {
        time.text = getString(R.string.amount_of_alarms, amount)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            val att = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            notificationChannel.apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                enableLights(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),att)
                //vibrationPattern = longArrayOf( 1000, 1000, 1000, 1000, 1000)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }

    }

    /**
     * Saves alarm list to the device memory
     */
    private fun saveArrayList(list: List<Alarm>?, key: String?) {
        val prefs: SharedPreferences = getPreferences(Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val json: String = Gson().toJson(list)
        editor.putString(key, json)
        editor.apply()
    }

    /**
     * List alarm list from the device memory
     */
    private fun getArrayList(key: String?): MutableList<Alarm>? {
        val prefs: SharedPreferences = getPreferences(Context.MODE_PRIVATE)
        val json: String? = prefs.getString(key, null)
        val type: Type = object : TypeToken<MutableList<Alarm>?>() {}.type
        return Gson().fromJson(json, type)
    }

    data class Alarm(val id: Int, val title:String, val detail: String)

}