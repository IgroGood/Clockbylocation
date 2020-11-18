package ru.igrogood.clockbylocation

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*


class ListClockAdapter(context: Context?, cloks: ArrayList<AlarmClock>?) : BaseAdapter() {
    interface INotesAdapterCallback {
        fun update()
    }

    var callback: INotesAdapterCallback? = null
    var ctx: Context? = context
    var lInflater: LayoutInflater? = null
    var objects: ArrayList<AlarmClock>? = cloks
    var adapter: ListClockAdapter = this

    init {
        lInflater = ctx
            ?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
    }

    override fun getCount(): Int {
        return objects!!.size
    }

    override fun getItem(position: Int): Any? {
        return objects?.get(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        // используем созданные, но не используемые view
        var view: View? = convertView
        if (view == null) {
            view = lInflater?.inflate(R.layout.item_alarm_clock, parent, false)
        }
        val p: AlarmClock = getClock(position)

        if (view != null) {
            (view.findViewById(R.id.name) as TextView).text = if(p.name.isNotEmpty()) p.name else "Будильник"
            (view.findViewById(R.id.descr) as TextView).text = if(p.descr.isNotEmpty()) p.descr else "Описание"
            (view.findViewById(R.id.latitude) as TextView).text = p.latitude.toString()
            (view.findViewById(R.id.longitude) as TextView).text = p.longitude.toString()
            val deleteBtn = view.findViewById(R.id.deleteBtn) as Button
            val cbBuy = view.findViewById<CheckBox>(R.id.isActive)
            cbBuy.isChecked = p.isActive
            cbBuy.setOnCheckedChangeListener{ compoundButton, b ->
                getClock(compoundButton.tag as Int).isActive = b
                callback?.update()
            }
            cbBuy.tag = position
            cbBuy.isChecked = p.isActive

            deleteBtn.setOnClickListener {
                objects?.removeAt(position)
                adapter.notifyDataSetChanged()
                callback?.update()
            }
        }
        return view
    }

    fun getClock(position: Int): AlarmClock {
        return getItem(position) as AlarmClock
    }

    fun getClocks(): ArrayList<AlarmClock>? {
        return objects
    }
}