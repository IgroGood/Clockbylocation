package ru.igrogood.clockbylocation

import android.Manifest
import android.app.ActivityManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.widget.addTextChangedListener
import com.huawei.hms.location.*
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.MapView
import com.huawei.hms.maps.OnMapReadyCallback
import com.huawei.hms.maps.model.CameraPosition
import com.huawei.hms.maps.model.Circle
import com.huawei.hms.maps.model.CircleOptions
import com.huawei.hms.maps.model.LatLng


class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    companion object {
        private val TAG = "MapViewDemoActivity"
        private const val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"
        private val REQUEST_CODE = 100
        private val RUNTIME_PERMISSIONS = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET
        )
    }

    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var currentDeviceLocation: Location? = null
    private var hmap: HuaweiMap? = null
    private var mMapView: MapView? = null
    private var alarmArea: Circle? = null
    private var radiusClock = 5000.0
    private var radiusCenter: LatLng? = null

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (hasPermissions(this, *RUNTIME_PERMISSIONS)) {
            ActivityCompat.requestPermissions(
                    this,
                    RUNTIME_PERMISSIONS,
                    REQUEST_CODE
            )
        }
        initializeGeolocation()
        mMapView = findViewById(R.id.mapView)
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY)
        }
        mMapView?.onCreate(mapViewBundle)
        mMapView?.getMapAsync(this)

        val nameClockEdit = findViewById<EditText>(R.id.nameClockEdit)
        val descrClockEdit = findViewById<EditText>(R.id.descrClockEdit)
        val radiusClockEdit = findViewById<EditText>(R.id.radiusClockEdit)
        val createClockBtn = findViewById<Button>(R.id.createClockBtn)

        radiusClockEdit.addTextChangedListener {
            if (it.toString().isNotEmpty())
                radiusClock = it.toString().toDouble()
            else
                radiusClock = 1.0
            drawOblatsAlarmClockActions()
        }

        val listClockBtn = findViewById<Button>(R.id.listClockBtn)
        listClockBtn.setOnClickListener {
            val intent = Intent(this@MainActivity, ListClockActivity::class.java)
            startActivity(intent)
        }
        createClockBtn.setOnClickListener {
            val appDB = openOrCreateDatabase("app.db", MODE_PRIVATE, null)
            appDB.execSQL("CREATE TABLE IF NOT EXISTS clocks(id_clock INTEGER PRIMARY KEY AUTOINCREMENT, name_clock VARCHAR(200), descr_clock VARCHAR(400), latitude_clock REAL, longitude_clock REAL, radius_clock INT, is_active_clock BIT)")
            //Log.i(TAG, "Location1: ${currentDeviceLocation?.latitude} ${currentDeviceLocation?.longitude}")


            if (alarmArea == null) {
                Toast.makeText(applicationContext, "Выберите область на карте", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newClock = ContentValues()
            newClock.put("name_clock", nameClockEdit.text.toString())
            newClock.put("descr_clock", descrClockEdit.text.toString())
            newClock.put("latitude_clock", alarmArea!!.center.latitude)
            newClock.put("longitude_clock", alarmArea!!.center.longitude)
            newClock.put("radius_clock", radiusClock)
            newClock.put("is_active_clock", true)
            appDB.insert("clocks", null, newClock)
            Toast.makeText(applicationContext, "Будильник создан", Toast.LENGTH_SHORT).show()
        }

        startService(Intent(this, ServiceClock::class.java))
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

    override fun onMapReady(map: HuaweiMap) {
        hmap = map
        hmap?.setMyLocationEnabled(true)
        val build = CameraPosition.Builder().target(LatLng(55.5807481, 36.8251304)).zoom(5f).build()
        val cameraUpdate = CameraUpdateFactory.newCameraPosition(build)
        hmap?.animateCamera(cameraUpdate)
        hmap?.setMaxZoomPreference(5f)
        hmap?.setMinZoomPreference(2f)
        hmap?.setOnMapClickListener {
            radiusCenter = it
            drawOblatsAlarmClockActions()
        }
    }

    private fun drawOblatsAlarmClockActions(){
        alarmArea?.remove()
        if (radiusCenter != null)
            alarmArea = hmap?.addCircle(
                    CircleOptions()
                            .center(radiusCenter)
                            .radius(radiusClock)
                            .fillColor(R.color.purple_200))
    }

    override fun onPause() {
        mMapView!!.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mMapView!!.onResume()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mMapView!!.onLowMemory()
    }

    private fun hasPermissions(context: Context, vararg permissions: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                                context,
                                permission
                        ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    override fun onStart() {
        super.onStart()
        mMapView!!.onStart()
    }

    override fun onStop() {
        super.onStop()
        mMapView!!.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mMapView!!.onDestroy()
    }
}