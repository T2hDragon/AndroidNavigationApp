package com.example.gps_sportmap


import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.gps_sportmap.database.repositories.UserRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.ask_session_details.view.*
import kotlinx.android.synthetic.main.confirm.view.*
import kotlinx.android.synthetic.main.map_actions.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    private lateinit var mMap: GoogleMap
    private var marker: Marker? = null

    private var mapPolyline: Polyline? = null

    private val broadcastReceiver = InnerBroadcastReceiver()
    private val broadcastReceiverIntentFilter: IntentFilter = IntentFilter()
    private var locationServiceActive = false
    private var compassOn = false
    private var centerToggle = true
    private var northUp = false


    // ============================================== COMPASS VALUES =============================================
    private lateinit var sensorManager: SensorManager
    private lateinit var compassImage: ImageView
    private lateinit var accelerometer: Sensor
    private lateinit var magnetometer: Sensor

    private var currentDegree = 0.0f
    private var lastAccelerometer = FloatArray(3)
    private var lastMagnetometer = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometerSet = false


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // History View setup
        history.setOnClickListener {
            val intent = Intent(applicationContext, SessionHistoryActivity::class.java)
            startActivity(intent)
            val userRepository = UserRepository(this).open()
            if (userRepository.getUserLoggedIn() == null) {
                userRepository.close()
                finish()
            }
            Log.d("User logged in", "User: " + userRepository.getUserLoggedIn())
            userRepository.close()
        }


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // safe to call every time
        createNotificationChannel()

        if (!checkPermissions()) {
            requestPermissions()
        }

        broadcastReceiverIntentFilter.addAction(C.LOCATION_UPDATE_ACTION)

        imageButtonStartStop.setOnClickListener {
            Log.d(TAG, "buttonStartStopOnClick. locationServiceActive: $locationServiceActive")
            if (locationServiceActive) {
                val dialog = BottomSheetDialog(this)
                val view = layoutInflater.inflate(R.layout.confirm, null)
                view.confirmation_text.text = "End session?"
                val decline = view.findViewById<ImageView>(R.id.confirm_decline)
                val accept = view.findViewById<ImageView>(R.id.confirm_accept)
                decline.setOnClickListener { dialog.dismiss() }
                // stopping the service
                accept.setOnClickListener {
                    stopService(Intent(this, LocationService::class.java))
                    imageButtonStartStop.setImageDrawable(
                            ContextCompat.getDrawable(
                                    this,
                                    R.drawable.baseline_play_arrow_24
                            )

                    )
                    history.backgroundTintList = ColorStateList.valueOf(Color.parseColor(C.NOT_IN_USE_COLOR))
                    locationServiceActive = !locationServiceActive
                    dialog.dismiss()
                }
                dialog.setContentView(view)
                dialog.show()

            } else {
                // clear the track on map
                Helpers.clearMapPolylineOptions()
                Helpers.clearMapWayPointMarkerOptions()
                Helpers.clearMapCheckPointMarkerOptions()
                val dialog = BottomSheetDialog(this)
                val view = layoutInflater.inflate(R.layout.ask_session_details, null)
                val accept = view.findViewById<ImageView>(R.id.confirm_details)
                // stopping the service
                accept.setOnClickListener {
                    C.sessionName = view.editTextSessionName.text.toString()
                    C.sessionDescription = view.editTextSessionDescription.text.toString()
                    if (Build.VERSION.SDK_INT >= 26) {
                        // starting the FOREGROUND service
                        // service has to display non-dismissible notification within 5 secs
                        startForegroundService(Intent(this, LocationService::class.java))
                    } else {
                        startService(Intent(this, LocationService::class.java))
                    }
                    imageButtonStartStop.setImageDrawable(
                            ContextCompat.getDrawable(
                                    this,
                                    R.drawable.baseline_stop_24
                            )
                    )

                    history.backgroundTintList = ColorStateList.valueOf(Color.parseColor(C.NOT_AVAILABLE_COLOR))
                    locationServiceActive = !locationServiceActive

                    dialog.dismiss()
                }
                dialog.setContentView(view)
                dialog.show()


            }
        }

        imageButtonCP.setOnClickListener {
            Log.d(TAG, "imageButtonCP")
            sendBroadcast(Intent(C.NOTIFICATION_ACTION_CP))
            C.cpPoint = true
        }
        imageButtonWP.setOnClickListener {
            Log.d(TAG, "imageButtonWP")
            sendBroadcast(Intent(C.NOTIFICATION_ACTION_WP))
            C.wpPoint = true
        }

        centerButton.setOnClickListener {
            centerToggle = !centerToggle
            centerButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (centerToggle) C.IN_USE_COLOR else C.NOT_IN_USE_COLOR))
        }

        compassImage = findViewById(R.id.imageViewCompass)
        compassImage.imageAlpha = 0
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        centerButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (centerToggle) C.IN_USE_COLOR else C.NOT_IN_USE_COLOR))
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isCompassEnabled = false
        val startingLocation = LatLng(59.39541850010906, 24.664189584607847) //Tallinn

        mMap.moveCamera(CameraUpdateFactory.zoomTo(17.0f))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(startingLocation))

    }


    private fun updateMap(lat: Double, lon: Double) {
        mMap.clear()
        val center = LatLng(lat, lon)
        if (marker != null) {
            marker!!.remove()
        }

        if (mapPolyline != null) {
            mapPolyline!!.remove()
        }

        //====== Update Markers =======
        marker = mMap
                .addMarker(
                        MarkerOptions()
                                .position(center)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.user))
                )

        //CheckPoint Markers
        val markerLocation = if (Helpers.getLastLocation() == null) center else Helpers.getLastLocation()!!
        if (C.cpPoint) {
            val markOptions = MarkerOptions()
                    .position(markerLocation)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.checkpoint))
            marker = mMap.addMarker(markOptions)
            Helpers.addToMapCheckPointMarkerOptions(markOptions)
            Helpers.clearMapWayPointMarkerOptions()
            C.cpPoint = false
        }

        //WayPoint Markers
        if (C.wpPoint) {
            val markOptions = MarkerOptions()
                    .position(markerLocation)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.waypoint))
            marker = mMap.addMarker(markOptions)
            Helpers.setMapWayPointMarkerOptions(markOptions)
            C.wpPoint = false
        }

        Helpers.getMapCheckPointMarkerOptions().forEach { markerOptions -> marker = mMap.addMarker(markerOptions) }
        if (Helpers.getWayPointMarkerOptions() != null) marker = mMap.addMarker(Helpers.getWayPointMarkerOptions()!!)
        //Map polyline
        mapPolyline = mMap.addPolyline(Helpers.getMapPolylineOptions())


        if (centerToggle) mMap.moveCamera(CameraUpdateFactory.newLatLng(center))
        if (northUp && mMap.cameraPosition.bearing != 0F) bearingNorth(null)

        Helpers.setLastLocation(center)
    }

    // ============================================== LIFECYCLE CALLBACKS =============================================
    override fun onStart() {
        Log.d(TAG, "onStart")
        super.onStart()
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadcastReceiver, broadcastReceiverIntentFilter)


        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()

        sensorManager.unregisterListener(this, accelerometer)
        sensorManager.unregisterListener(this, magnetometer)
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }

    override fun onRestart() {
        Log.d(TAG, "onRestart")
        super.onRestart()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("centerToggle", centerToggle)
        outState.putBoolean("northUp", northUp)
        outState.putBoolean("compassOn", compassOn)
        outState.putBoolean("locationServiceActive", locationServiceActive)

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        centerToggle = savedInstanceState.getBoolean("centerToggle")
        northUp = savedInstanceState.getBoolean("northUp")
        compassOn = savedInstanceState.getBoolean("compassOn")
        locationServiceActive = savedInstanceState.getBoolean("locationServiceActive")


        if (locationServiceActive) {
            imageButtonStartStop.setImageDrawable(
                    ContextCompat.getDrawable(
                            this,
                            R.drawable.baseline_stop_24
                    )
            )
        }
        if (compassOn) {
            Log.d("compassRestore", compassImage.toString())
            compassImage.imageAlpha = 255
        }
        compassButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (compassOn) C.IN_USE_COLOR else C.NOT_IN_USE_COLOR))
        northButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (northUp) C.IN_USE_COLOR else C.NOT_IN_USE_COLOR))
        centerButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (centerToggle) C.IN_USE_COLOR else C.NOT_IN_USE_COLOR))
        history.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (locationServiceActive) C.NOT_AVAILABLE_COLOR else C.NOT_IN_USE_COLOR))
    }


    // ============================================== NOTIFICATION CHANNEL CREATION =============================================
    private fun createNotificationChannel() {
        // when on 8 Oreo or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                    C.NOTIFICATION_CHANNEL,
                    "Default channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setSound(null, null)


            channel.description = "Default channel"

            val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // ============================================== PERMISSION HANDLING =============================================
    // Returns the current state of the permissions needed.
    private fun checkPermissions(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun requestPermissions() {
        val shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(
                    TAG,
                    "Displaying permission rationale to provide additional context."
            )
            Snackbar.make(
                    findViewById(R.id.map),
                    "Hey, i really need to access GPS!",
                    Snackbar.LENGTH_INDEFINITE
            )
                    .setAction("OK") {
                        // Request permission
                        ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                C.REQUEST_PERMISSIONS_REQUEST_CODE
                        )
                    }
                    .show()
        } else {
            Log.i(TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    C.REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == C.REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.count() <= 0) { // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.")
                Toast.makeText(this, "User interaction was cancelled.", Toast.LENGTH_SHORT).show()
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {// Permission was granted.
                Log.i(TAG, "Permission was granted")
                Toast.makeText(this, "Permission was granted", Toast.LENGTH_SHORT).show()
            } else { // Permission denied.
                Snackbar.make(
                        findViewById(R.id.map),
                        "You denied GPS! What can I do?",
                        Snackbar.LENGTH_INDEFINITE
                )
                        .setAction("Settings") {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri: Uri = Uri.fromParts(
                                    "package",
                                    BuildConfig.APPLICATION_ID, null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .show()
            }
        }


    }

    // ============================================== BROADCAST RECEIVER =============================================
    private inner class InnerBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, intent?.action!!)
            when (intent.action) {
                C.LOCATION_UPDATE_ACTION -> {
                    textViewOverallTotal.text = Helpers.getDistanceString(
                            intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_OVERALL_TOTAL, 0.0f).toInt()
                                    .toString())

                    var duration = intent.getLongExtra(C.LOCATION_UPDATE_ACTION_OVERALL_TIME, 0)
                    textViewOverallDuration.text = Helpers.getTimeString(duration)
                    textViewOverallSpeed.text = Helpers.getPace(
                            duration,
                            intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_OVERALL_TOTAL, 0.0f)
                    )

                    textViewCPTotal.text = Helpers.getDistanceString(
                            intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_CP_TOTAL, 0.0f).toInt()
                                    .toString())

                    duration = intent.getLongExtra(C.LOCATION_UPDATE_ACTION_CP_TIME, 0)
                    textViewCPDuration.text = Helpers.getTimeString(duration)
                    textViewCPSpeed.text = Helpers.getPace(
                            duration,
                            intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_CP_TOTAL, 0.0f)
                    )

                    textViewWPTotal.text = Helpers.getDistanceString(
                            intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_WP_TOTAL, 0.0f).toInt()
                                    .toString())

                    duration = intent.getLongExtra(C.LOCATION_UPDATE_ACTION_WP_TIME, 0)
                    textViewWPDuration.text = Helpers.getTimeString(duration)
                    textViewWPSpeed.text = Helpers.getPace(
                            duration,
                            intent.getFloatExtra(C.LOCATION_UPDATE_ACTION_WP_TOTAL, 0.0f)
                    )

                    updateMap(
                            intent.getDoubleExtra(C.LOCATION_UPDATE_ACTION_LAT, 0.0),
                            intent.getDoubleExtra(C.LOCATION_UPDATE_ACTION_LON, 0.0)
                    )
                }
                C.LOCATION_UPDATE_STOP -> {
                }
            }
        }
    }

    // ============================================== COMPASS UPDATE =============================================
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor === accelerometer) {
            lowPass(event.values, lastAccelerometer)
            lastAccelerometerSet = true
        } else if (event.sensor === magnetometer) {
            lowPass(event.values, lastMagnetometer)
            lastMagnetometerSet = true
        }

        if (lastAccelerometerSet && lastMagnetometerSet) {
            val r = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, null, lastAccelerometer, lastMagnetometer)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)
                val degree = (Math.toDegrees(orientation[0].toDouble()) + 360).toFloat() % 360

                val rotateAnimation = RotateAnimation(
                        currentDegree,
                        -degree,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f
                )
                rotateAnimation.duration = 1000
                rotateAnimation.fillAfter = true

                compassImage.startAnimation(rotateAnimation)
                currentDegree = -degree
            }
        }
    }

    private fun lowPass(input: FloatArray, output: FloatArray) {
        val alpha = 0.05f

        for (i in input.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}


    fun hideOrShowCompass(view: View?) {
        compassImage.imageAlpha = if (compassImage.imageAlpha == 255) 0 else 255
        compassOn = !compassOn
        compassButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (compassOn) C.IN_USE_COLOR else C.NOT_IN_USE_COLOR));

    }

    fun bearingNorth(view: View?) {
        val bearingNorth = CameraPosition(mMap.cameraPosition.target, mMap.cameraPosition.zoom, mMap.cameraPosition.tilt, 0F)
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(bearingNorth))
        if (view != null) {
            northUp = !northUp
            northButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (northUp) C.IN_USE_COLOR else C.NOT_IN_USE_COLOR))
        }
    }

}
