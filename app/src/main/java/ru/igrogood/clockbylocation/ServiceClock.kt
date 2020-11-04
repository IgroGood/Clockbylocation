package ru.igrogood.clockbylocation

import android.R
import android.app.*
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
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.huawei.hms.location.*
import java.util.concurrent.TimeUnit


class ServiceClock : Service() {
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var currentDeviceLocation: Location? = null
    var cloks: ArrayList<Clock> = ArrayList()
    val LOG_TAG = "myLogs"
    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "onStartCommand")
        someTask()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(LOG_TAG, "onBind")
        return null
    }

    fun someTask() {
        Thread {
            //for (i in 1..1000) {
            while (true){
                //Log.d(LOG_TAG, "i = $i")
                cloks = ArrayList()
                initializeGeolocation()
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
                            cloks.add(Clock(id, name, descr, latitude, longitude, radius, is_active))
                        }
                    myCursor?.close()


                    Log.i("myLogs", "поиск...")
                    if (currentDeviceLocation != null) {
                        Log.i("myLogs", "поиск")
                        for (clock in cloks) {
                            if(clock.isActive){
                                Log.i("myLogs", "${clock.latitude} ${clock.longitude}")
                                var f = (currentDeviceLocation!!.latitude - clock.latitude) * (currentDeviceLocation!!.latitude - clock.latitude) + (currentDeviceLocation!!.longitude - clock.longitude) * (currentDeviceLocation!!.longitude - clock.longitude)
                                Log.i("myLogs", "${currentDeviceLocation!!.latitude} ${currentDeviceLocation!!.latitude} = $f")
                                val result = (currentDeviceLocation!!.latitude - clock.latitude) * (currentDeviceLocation!!.latitude - clock.latitude) + (currentDeviceLocation!!.longitude - clock.longitude) * (currentDeviceLocation!!.longitude - clock.longitude) <= clock.radius * clock.radius
                                Log.i("myLogs", result.toString())
                                if ((currentDeviceLocation!!.latitude - clock.latitude) * (currentDeviceLocation!!.latitude - clock.latitude) + (currentDeviceLocation!!.longitude - clock.longitude) * (currentDeviceLocation!!.longitude - clock.longitude) <= clock.radius * clock.radius) {
                                    showNotification()
                                }
                            }
                        }
                    }
                    TimeUnit.SECONDS.sleep(10)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            stopSelf()
        }.start()
    }

    fun showNotification(){
        val NOTIFICATION_ID = 234
        val CHANNEL_ID = "alarm_clock_01"
        val name: CharSequence = "alarm_clock"
        val Description = "clock by location"
        val notificationManager = this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = Description
            mChannel.enableLights(true)
            mChannel.lightColor = Color.RED
            mChannel.enableVibration(true)
            mChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            mChannel.setShowBadge(false)
            notificationManager.createNotificationChannel(mChannel)
        }

        val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(ru.igrogood.clockbylocation.R.mipmap.ic_launcher)
                .setContentTitle("Внимание")
                .setContentText("Вы вошли в зону будильника")
                .setSound(alarmSound)

        val resultIntent = Intent(this, MainActivity::class.java)
        val stackBuilder: TaskStackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(resultIntent)
        val resultPendingIntent: PendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        builder.setContentIntent(resultPendingIntent)
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun initializeGeolocation(){
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val mLocationRequest = LocationRequest()
        mLocationRequest.interval = 10000
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val mLocationCallback: LocationCallback
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                currentDeviceLocation = locationResult.lastLocation
            }
        }
        fusedLocationProviderClient!!
                .requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper())
    }
}