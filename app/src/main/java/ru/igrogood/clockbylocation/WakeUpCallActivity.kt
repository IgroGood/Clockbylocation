package ru.igrogood.clockbylocation

import android.app.NotificationManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WakeUpCallActivity : AppCompatActivity() {
    var idAlarmClock: Int? = null
    var alarmClock: AlarmClock? = null
    var appDB: SQLiteDatabase? = null
    val LOG_TAG = "myLogs"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wake_up_call)
        idAlarmClock = intent.getIntExtra(ID_ALARM_CLOCK, -1)
        if(idAlarmClock != null){
            try {
                appDB = openOrCreateDatabase("app.db", MODE_PRIVATE, null)
                appDB?.execSQL("CREATE TABLE IF NOT EXISTS clocks(id_clock INTEGER PRIMARY KEY AUTOINCREMENT, name_clock VARCHAR(200), descr_clock VARCHAR(400), latitude_clock REAL, longitude_clock REAL, radius_clock INT, is_active_clock BIT);")
                val myCursor: Cursor? = appDB?.rawQuery("SELECT * FROM clocks WHERE(id_clock = $idAlarmClock)", null)
                myCursor?.moveToNext()
                if (myCursor != null) {
                    alarmClock = AlarmClock(myCursor.getInt(0),
                        myCursor.getString(1),
                        myCursor.getString(2),
                        myCursor.getDouble(3),
                        myCursor.getDouble(4),
                        myCursor.getInt(5),
                        myCursor.getInt(6) == 1)
                    myCursor.close()
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        if (alarmClock!!.name.isNotEmpty())
            findViewById<TextView>(R.id.nameAlarmClockEdit).text = alarmClock?.name
        else
            findViewById<TextView>(R.id.nameAlarmClockEdit).text = "Будильник"
        findViewById<Button>(R.id.offBtn).setOnClickListener{
            try {
                //appDB?.execSQL("UPDATE clocks SET is_active_clock = false WHERE id_clock = ${alarmClock?.id}")
                appDB?.execSQL("UPDATE clocks SET is_active_clock = 0 WHERE id_clock = ${alarmClock?.id}")
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(ServiceClock.NOTIFICATION_ID)
                this.finish()
            } catch (e: InterruptedException){
                e.printStackTrace()
            }
        }
    }

    companion object {
        val ID_ALARM_CLOCK = "mame_alarm_clock"
    }
}