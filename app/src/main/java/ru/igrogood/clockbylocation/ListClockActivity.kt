package ru.igrogood.clockbylocation

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity


open class ListClockActivity : AppCompatActivity() {
    var cloks: ArrayList<Clock> = ArrayList<Clock>()
    var appDB: SQLiteDatabase? = null
    private var listClockAdapter: ListClockAdapter? = null
    var deleteMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_clock)

        val lvMain: ListView = findViewById<ListView>(R.id.listClock) as ListView
        appDB = openOrCreateDatabase("app.db", MODE_PRIVATE, null)
        appDB?.execSQL("CREATE TABLE IF NOT EXISTS clocks(id_clock INTEGER PRIMARY KEY AUTOINCREMENT, name_clock VARCHAR(200), descr_clock VARCHAR(400), latitude_clock REAL, longitude_clock REAL, radius_clock INT, is_active_clock BIT)")
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
        listClockAdapter = ListClockAdapter(this, cloks)
        lvMain.setAdapter(listClockAdapter)

        val deleteModeBtn = findViewById<Button>(R.id.saveBtn)
        deleteModeBtn.setOnClickListener {
            val listClock = listClockAdapter?.getClocks()
            appDB?.delete("clocks", null, null)
            if (listClock != null)
                for (clock in listClock){
                    val newClock = ContentValues()
                    newClock.put("name_clock", clock.name)
                    newClock.put("descr_clock", clock.descr)
                    newClock.put("latitude_clock", clock.latitude)
                    newClock.put("longitude_clock", clock.longitude)
                    newClock.put("radius_clock", clock.radius)
                    newClock.put("is_active_clock", clock.isActive)
                    appDB?.insert("clocks", null, newClock)
                }
        }
    }
}