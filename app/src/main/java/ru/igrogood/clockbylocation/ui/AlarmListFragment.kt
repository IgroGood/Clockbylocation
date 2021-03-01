package ru.igrogood.clockbylocation.ui

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import ru.igrogood.clockbylocation.AlarmClock
import ru.igrogood.clockbylocation.ListClockAdapter
import ru.igrogood.clockbylocation.R


class AlarmListFragment : Fragment(), ListClockAdapter.INotesAdapterCallback {
    var cloks: ArrayList<AlarmClock> = ArrayList()
    var appDB: SQLiteDatabase? = null
    private var listClockAdapter: ListClockAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_alarm_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val lvMain: ListView = view?.findViewById(R.id.listClock) as ListView
        appDB = requireContext().openOrCreateDatabase(
            "app.db",
            AppCompatActivity.MODE_PRIVATE,
            null
        )
        appDB?.execSQL(
            """
            CREATE TABLE IF NOT EXISTS clocks(
                id_clock INTEGER PRIMARY KEY AUTOINCREMENT,
                name_clock VARCHAR(200),
                descr_clock VARCHAR(400),
                latitude_clock REAL,
                longitude_clock REAL,
                radius_clock INT,
                is_active_clock BIT)
        """.trimIndent()
        )
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
        listClockAdapter = ListClockAdapter(requireContext(), cloks)
        listClockAdapter?.callback = this
        lvMain.adapter = listClockAdapter
    }

    override fun update() {
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