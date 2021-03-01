package ru.igrogood.clockbylocation.ui

import android.content.ContentValues
import android.content.Context.MODE_PRIVATE
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.material.slider.Slider
import com.huawei.hms.location.*
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.OnMapReadyCallback
import com.huawei.hms.maps.model.CameraPosition
import com.huawei.hms.maps.model.Circle
import com.huawei.hms.maps.model.CircleOptions
import com.huawei.hms.maps.model.LatLng
import kotlinx.android.synthetic.main.fragment_map.*
import ru.igrogood.clockbylocation.InputFilterMinMax
import ru.igrogood.clockbylocation.R


class MapFragment : Fragment(), OnMapReadyCallback {
    val MY_LOG = "myLogs"

    private val TAG = "MapViewDemoActivity"
    private var mMapView: MapView? = null
    private val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var currentDeviceLocation: Location? = null
    private var hMap: HuaweiMap? = null
    private var alarmArea: Circle? = null
    private var radiusClock = 0.0
    private var radiusCenter: LatLng? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mMapView = view.findViewById(R.id.mapView)
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null)
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY)
        radiusClock = getString(R.string.default_radius).toDouble()
        mMapView!!.onCreate(mapViewBundle)
        mMapView!!.getMapAsync(this)

        initializeGeolocation()
        val nameClockEdit = view.findViewById<EditText>(R.id.nameClockEdit)
        val descrClockEdit = view.findViewById<EditText>(R.id.descrClockEdit)
        val radiusSlider = view.findViewById<Slider>(R.id.radiusSlider)
        val radiusClockEdit = view.findViewById<EditText>(R.id.radiusClockEdit)
        val createClockBtn = view.findViewById<Button>(R.id.createClockBtn)

        radiusClockEdit.addTextChangedListener {
            if (it.toString().isNotEmpty())
                radiusClock = it.toString().toDouble()
            else
                radiusClock = 1.0
            drawOblatsAlarmClockActions()
            if(!it.isNullOrEmpty())
                radiusSlider.value = radiusClockEdit.text.toString().toFloat()
        }

        radiusSlider.addOnChangeListener { slider, value, fromUser ->
            radiusClock = value.toDouble()
            radiusClockEdit.setText(value.toInt().toString())
            drawOblatsAlarmClockActions()
        }

        radiusClockEdit.filters = arrayOf(
            InputFilterMinMax(
                "0",
                getString(R.string.default_max_radius)))

        createClockBtn.setOnClickListener {
            val appDB = requireContext().openOrCreateDatabase("app.db", MODE_PRIVATE, null)
            appDB?.execSQL("CREATE TABLE IF NOT EXISTS clocks(id_clock INTEGER PRIMARY KEY AUTOINCREMENT, name_clock VARCHAR(200), descr_clock VARCHAR(400), latitude_clock REAL, longitude_clock REAL, radius_clock INT, is_active_clock BIT)")

            if (alarmArea == null) {
                Toast.makeText(requireContext(), "Выберите область на карте", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newClock = ContentValues()
            newClock.put("name_clock", nameClockEdit.text.toString())
            newClock.put("descr_clock", descrClockEdit.text.toString())
            newClock.put("latitude_clock", alarmArea!!.center.latitude)
            newClock.put("longitude_clock", alarmArea!!.center.longitude)
            //newClock.put("latitude_clock", 0)
            //newClock.put("longitude_clock", 0)
            newClock.put("radius_clock", radiusClock)
            newClock.put("is_active_clock", true)
            appDB?.insert("clocks", null, newClock)
            Toast.makeText(requireContext(), "Будильник создан", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeGeolocation(){
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
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

    private fun drawOblatsAlarmClockActions(){
        alarmArea?.remove()
        if (radiusCenter != null)
            alarmArea = hMap?.addCircle(
                CircleOptions()
                    .center(radiusCenter)
                    .radius(radiusClock)
                    .fillColor(R.color.purple_200))
    }

    override fun onMapReady(map: HuaweiMap) {
        hMap = map
        hMap?.isMyLocationEnabled = true
        val build = CameraPosition.Builder().target(LatLng(55.5807481, 36.8251304)).zoom(5f).build()
        val cameraUpdate = CameraUpdateFactory.newCameraPosition(build)
        hMap?.animateCamera(cameraUpdate)
        hMap?.setOnMapClickListener {
            radiusCenter = it
            drawOblatsAlarmClockActions()
        }
    }

    override fun onStart() {
        super.onStart()
        mMapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mMapView?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mMapView?.onDestroy()
    }

    override fun onPause() {
        mMapView?.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mMapView?.onResume()
    }
}