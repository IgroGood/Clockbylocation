package ru.igrogood.clockbylocation

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*


class ListClockAdapter(context: Context?, cloks: ArrayList<Clock>?) : BaseAdapter() {
    var ctx: Context? = context
    var lInflater: LayoutInflater? = null
    var objects: ArrayList<Clock>? = cloks
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
            view = lInflater?.inflate(R.layout.item_clock, parent, false)
        }
        val p: Clock = getClock(position)

        if (view != null) {
            (view.findViewById(R.id.name) as TextView).setText(p.name)
            (view.findViewById(R.id.descr) as TextView).setText(p.descr)
            //(view.findViewById(R.id.isActive) as CheckBox).isChecked = (p.isActive)
            val deleteBtn = view.findViewById(R.id.deleteBtn) as Button
            val cbBuy = view.findViewById<CheckBox>(R.id.isActive)
            cbBuy.isChecked = p.isActive
            cbBuy.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { compoundButton, b ->
                Log.i("ssssssssssss", b.toString())
                getClock(compoundButton.getTag() as Int).isActive = b
            })
            cbBuy.tag = position
            cbBuy.isChecked = p.isActive

            deleteBtn.setOnClickListener {
                objects?.removeAt(position)
                adapter.notifyDataSetChanged()
            }
        }
        return view
    }


    fun getClock(position: Int): Clock {
        return getItem(position) as Clock
    }

    fun getClocks(): ArrayList<Clock>? {
        return objects
    }

    fun setDataUpdateListener(l: View.OnClickListener?) {
        throw RuntimeException("Stub!")
    }
}