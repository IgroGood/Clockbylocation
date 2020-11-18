package ru.igrogood.clockbylocation

import android.app.*
import android.app.Notification.FLAG_INSISTENT
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.location.Location
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.huawei.hms.location.*
import ru.igrogood.clockbylocation.WakeUpCallActivity.Companion.ID_ALARM_CLOCK
import java.util.concurrent.TimeUnit


class ServiceClock : Service() {
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var currentDeviceLocation: Location? = null
    private var notificationManager: NotificationManager? = null
    var cloks: ArrayList<AlarmClock> = ArrayList()
    val LOG_TAG = "myLogs"

    companion object {
        const val NOTIFICATION_ID = 234
        const val CHANNEL_ID = "alarm_clock_01"
        val NAME: CharSequence = "alarm_clock"
        const val DESCRIPTION = "clock by location"
    }

    private fun someTask() {
        Thread {
            while (true){
                cloks = ArrayList()
                try {
                    val appDB = openOrCreateDatabase("app.db", MODE_PRIVATE, null)
                    appDB.execSQL("CREATE TABLE IF NOT EXISTS clocks(id_clock INTEGER PRIMARY KEY AUTOINCREMENT, name_clock VARCHAR(200), descr_clock VARCHAR(400), latitude_clock REAL, longitude_clock REAL, radius_clock INT, is_active_clock BIT)")
                    val myCursor: Cursor? = appDB?.rawQuery("SELECT * FROM clocks", null)
                    if (myCursor != null)
                        while (myCursor.moveToNext()) {
                            val id = myCursor.getInt(0)
                            val name = myCursor.getString(1)
                            val descr = myCursor.getString(2)
                            val latitude = myCursor.getDouble(3)
                            val longitude = myCursor.getDouble(4)
                            val radius = myCursor.getInt(5)
                            val is_active = myCursor.getInt(6) == 1
                            cloks.add(AlarmClock(id, name, descr, latitude, longitude, radius, is_active))
                        }
                    myCursor?.close()

                    var isAlarmClockActive = false
                    if (currentDeviceLocation != null) {
                        for (clock in cloks) {
                            if(clock.isActive){
                                if (checkTheEntryLocation(currentDeviceLocation!!, clock)) {
                                    showNotification(clock.id)
                                    isAlarmClockActive = true
                                }
                            } else {
                                if (!checkTheEntryLocation(currentDeviceLocation!!, clock)) {
                                    showNotification(clock.id)
                                    isAlarmClockActive = true
                                }
                            }
                        }
                    }

                    if(!isAlarmClockActive){
                        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        mNotificationManager.cancelAll()
                    }

                    TimeUnit.SECONDS.sleep(60)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            stopSelf()
        }.start()
    }

    private fun checkTheEntryLocation(d: Location, ac: AlarmClock): Boolean {
        // (d.x - ac.x)^2 + (d.y - ac.y)^2 ?= R^2
        if ((d.latitude-ac.latitude)*(d.latitude-ac.latitude)
                +(d.longitude-ac.longitude)*(d.longitude-ac.longitude)
                <= ac.radius * ac.radius)
            return true
        return false
    }

    private fun createChanelNotification(){
        notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(CHANNEL_ID, NAME, importance)
            mChannel.description = DESCRIPTION
            mChannel.enableLights(true)
            mChannel.lightColor = Color.RED
            mChannel.enableVibration(true)
            mChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            mChannel.setShowBadge(true)
            notificationManager?.createNotificationChannel(mChannel)
        }
    }

    private fun showNotification(IdAlarmClock: Int){
        val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setDefaults(FLAG_INSISTENT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.warning))
            .setContentText(getString(R.string.notification_when_the_alarm_is_activated))
            .setSound(alarmSound)
            .setUsesChronometer(true)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(false)
        //val resultIntent = Intent(this, AppActivity::class.java)
        val resultIntent = Intent(this, WakeUpCallActivity::class.java).putExtra(ID_ALARM_CLOCK, IdAlarmClock)
        val stackBuilder: TaskStackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addParentStack(AppActivity::class.java)
        stackBuilder.addNextIntent(resultIntent)
        val resultPendingIntent: PendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(resultPendingIntent)
        val notification: Notification = builder.build()
        notification.flags += FLAG_INSISTENT
        notificationManager?.notify(NOTIFICATION_ID, builder.build())
    }

    private fun initializeGeolocation(){
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val mLocationRequest = LocationRequest()
        mLocationRequest.interval = 60000 // ms
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val mLocationCallback: LocationCallback
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                currentDeviceLocation = locationResult.lastLocation
                Log.i(LOG_TAG, currentDeviceLocation.toString())
            }
        }
        fusedLocationProviderClient!!
            .requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper())
    }

    override fun onCreate() {
        super.onCreate()
        createChanelNotification()
        initializeGeolocation()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        someTask()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}