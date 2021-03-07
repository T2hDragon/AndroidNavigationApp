package com.example.gps_sportmap

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.gps_sportmap.database.dto.Session
import com.example.gps_sportmap.database.repositories.LocationRepository
import com.example.gps_sportmap.database.repositories.SessionRepository
import com.google.android.gms.location.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.text.SimpleDateFormat
import java.util.*


class LocationService : Service() {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    // The desired intervals for location updates. Inexact. Updates may be more or less frequent.
    private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 2000
    private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2

    private val broadcastReceiver = InnerBroadcastReceiver()
    private val broadcastReceiverIntentFilter: IntentFilter = IntentFilter()

    private val mLocationRequest: LocationRequest = LocationRequest()
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mLocationCallback: LocationCallback? = null

    // last received location
    private var currentLocation: Location? = null

    private var distanceOverallDirect = 0f
    private var distanceOverallTotal = 0f
    private var locationStart: Location? = null
    private var distanceOverallTime: Long = 0L
    private var synced: Boolean = true


    private var distanceCPDirect = 0f
    private var distanceCPTotal = 0f
    private var locationCP: Location? = null
    private var distanceCPTime: Long = 0L

    private var distanceWPDirect = 0f
    private var distanceWPTotal = 0f
    private var distanceWPTime: Long = 0L

    private var locationWP: Location? = null

    private var session: Session? = null
    private var jwt: String? = null
    private var trackingSessionId: String? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())

    private lateinit var locationRepository: LocationRepository
    private lateinit var sessionRepository: SessionRepository


    override fun onCreate() {
        Log.d(TAG, "onCreate")
        super.onCreate()
        // Repositories
        locationRepository = LocationRepository(this).open()
        sessionRepository = SessionRepository(this).open()

        broadcastReceiverIntentFilter.addAction(C.NOTIFICATION_ACTION_CP)
        broadcastReceiverIntentFilter.addAction(C.NOTIFICATION_ACTION_WP)
        broadcastReceiverIntentFilter.addAction(C.LOCATION_UPDATE_ACTION)

        registerReceiver(broadcastReceiver, broadcastReceiverIntentFilter)


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                onNewLocation(locationResult.lastLocation)
            }
        }


        getRestToken()

/*
        getLastLocation()
*/

        createLocationRequest()
        requestLocationUpdates()

    }


    private fun getRestToken() {
        val handler = WebApiSingletonHandler.getInstance(applicationContext)

        val requestJsonParameters = JSONObject()
        requestJsonParameters.put("email", C.REST_EMAIL)
        requestJsonParameters.put("password", C.REST_PASSWORD)

        val httpRequest = JsonObjectRequest(
                Request.Method.POST,
                C.REST_BASE_URL + "account/login",
                requestJsonParameters,
                { response ->
                    Log.d(TAG, response.toString())
                    jwt = response.getString("token")
                    startRestTrackingSession()

                },
                { error ->
                    Log.d(TAG, error.toString())
                    jwt = "Missing"
                    startRestTrackingSession()

                }
        )

        handler.addToRequestQueue(httpRequest)

    }


    private fun startRestTrackingSession() {
        //Entry in backend
        var handler = WebApiSingletonHandler.getInstance(applicationContext)
        val requestJsonParameters = JSONObject()
        requestJsonParameters.put("name", C.sessionName)
        requestJsonParameters.put("description", C.sessionDescription)
        requestJsonParameters.put("paceMin", 6*60)
        requestJsonParameters.put("paceMax", 18*60)


        var httpRequest = object : JsonObjectRequest(
                Request.Method.POST,
                C.REST_BASE_URL + "GpsSessions",
                requestJsonParameters,
                Response.Listener { response ->
                    Log.d(TAG, response.toString())
                    trackingSessionId = response.getString("id")

                    //Entry in local Db
                    session = sessionRepository.addSession(
                            Session(
                                    C.USER_ID, response.getString("id"), C.sessionName, C.sessionDescription,
                                    dateFormat.format(Calendar.getInstance().time), "0", "0"
                            )
                    )
                },
                Response.ErrorListener { error ->
                    Log.d(TAG, error.toString())
                    //Entry in local Db
                    session = sessionRepository.addSession(
                            Session(
                                    C.USER_ID, "missing", C.sessionName, C.sessionDescription,
                                    dateFormat.format(Calendar.getInstance().time), "0", "0"
                            )
                    )
                }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                for ((key, value) in super.getHeaders()) {
                    headers[key] = value
                }
                headers["Authorization"] = "Bearer " + jwt!!
                return headers
            }
        }


        handler.addToRequestQueue(httpRequest)

    }

    private fun saveLocation(location: Location, location_type: String) {

        if (session != null) {
            val locationDto: com.example.gps_sportmap.database.dto.Location =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        com.example.gps_sportmap.database.dto.Location(
                                session!!.sessionId,
                                dateFormat.format(Date(location.time)),
                                location.latitude.toString(),
                                location.longitude.toString(),
                                location.accuracy.toString(),
                                location.altitude.toString(),
                                location.verticalAccuracyMeters.toString(),
                                location_type,
                                1
                        )
                    } else TODO("VERSION.SDK_INT < O")

            var handler = WebApiSingletonHandler.getInstance(applicationContext)
            val requestJsonParameters = JSONObject()

            requestJsonParameters.put("recordedAt", dateFormat.format(Date(location.time)))

            requestJsonParameters.put("latitude", location.latitude)
            requestJsonParameters.put("longitude", location.longitude)
            requestJsonParameters.put("accuracy", location.accuracy)
            requestJsonParameters.put("altitude", location.altitude)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requestJsonParameters.put("verticalAccuracy", location.verticalAccuracyMeters)
            }
            requestJsonParameters.put("gpsSessionId", trackingSessionId)
            requestJsonParameters.put("gpsLocationTypeId", location_type)


            var httpRequest = object : JsonObjectRequest(
                    Request.Method.POST,
                    C.REST_BASE_URL + "GpsLocations",
                    requestJsonParameters,
                    Response.Listener { response ->
                        Log.d(TAG, response.toString())
                        Log.d("Sent session succeeded", requestJsonParameters.toString())
                        locationRepository.addLocation(locationDto)
                    },
                    Response.ErrorListener { error ->
                        Log.d(TAG, error.toString())
                         if (!isOnline(this)) {
                             synced = false
                             locationRepository.addLocation(locationDto.notPassed())
                             Log.d("Sent session failed", requestJsonParameters.toString())
                         }
                    }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    for ((key, value) in super.getHeaders()) {
                        headers[key] = value
                    }
                    headers["Authorization"] = "Bearer " + jwt!!
                    return headers
                }
            }
            handler.addToRequestQueue(httpRequest)
            if (location_type == C.REST_LOCATIONID_CP) {
                C.cpPoint = true
            }
            if (location_type == C.REST_LOCATIONID_WP) {
                C.wpPoint = true
            }

        }
    }

    private fun requestLocationUpdates() {


        Log.i(TAG, "Requesting location updates")

        try {
            mFusedLocationClient.requestLocationUpdates(
                    mLocationRequest,
                    mLocationCallback, Looper.myLooper()
            )
        } catch (unlikely: SecurityException) {
            Log.e(
                    TAG,
                    "Lost location permission. Could not request updates. $unlikely"
            )
        }
    }

    private fun onNewLocation(location: Location) {
        Log.d(TAG, "New location: ${location.longitude}, ${location.latitude}")
        if (location.accuracy > 100) {
            return
        }
        if (currentLocation == null) {
            locationStart = location
            locationCP = location
            locationWP = location
        } else {
            distanceOverallDirect = location.distanceTo(locationStart)
            distanceOverallTotal += location.distanceTo(currentLocation)
            distanceOverallTime += (location.time - currentLocation!!.time)

            distanceCPDirect = location.distanceTo(locationCP)
            distanceCPTotal += location.distanceTo(currentLocation)
            distanceCPTime += (location.time - currentLocation!!.time)

            distanceWPDirect = location.distanceTo(locationWP)
            distanceWPTotal += location.distanceTo(currentLocation)
            distanceWPTime += (location.time - currentLocation!!.time)
        }
        // save the location for calculations
        currentLocation = location

        showNotification()

        // save the data to mapPolyLine singleton
        Helpers.addToMapPolylineOptions(location.latitude, location.longitude)


        saveLocation(location, C.REST_LOCATIONID_LOC)

        // broadcast new location to UI
        val intent = Intent(C.LOCATION_UPDATE_ACTION)

        intent.putExtra(C.LOCATION_UPDATE_ACTION_LAT, location.latitude)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_LON, location.longitude)

        intent.putExtra(C.LOCATION_UPDATE_ACTION_OVERALL_DIRECT, distanceOverallDirect)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_OVERALL_TOTAL, distanceOverallTotal)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_OVERALL_TIME, distanceOverallTime)

        intent.putExtra(C.LOCATION_UPDATE_ACTION_CP_DIRECT, distanceCPDirect)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_CP_TOTAL, distanceCPTotal)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_CP_TIME, distanceCPTime)

        intent.putExtra(C.LOCATION_UPDATE_ACTION_WP_DIRECT, distanceWPDirect)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_WP_TOTAL, distanceWPTotal)
        intent.putExtra(C.LOCATION_UPDATE_ACTION_WP_TIME, distanceWPTime)

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

        if (isOnline(this) && !synced){

            // send locations to backend
            val result = JSONArray()
            val locations = locationRepository.getPassedAllNotPassedBySessionId(session!!.sessionId)
            for (location in locations) {
                val requestJsonParameters = JSONObject()

                requestJsonParameters.put("recordedAt", location.recordedAt)
                requestJsonParameters.put("latitude", location.latitude.toDouble())
                requestJsonParameters.put("longitude", location.longitude.toDouble())
                requestJsonParameters.put("accuracy", location.accuracy.toDouble())
                requestJsonParameters.put("altitude", location.altitude.toDouble())
                requestJsonParameters.put("verticalAccuracy", location.verticalAccuracy.toDouble())
                requestJsonParameters.put("gpsLocationTypeId", location.locationTypeId)
                result.put(requestJsonParameters)
            }

            try {
                val requestQueue = Volley.newRequestQueue(this)
                val mRequestBody = result.toString()
                val stringRequest: StringRequest = object : StringRequest(
                        Method.POST, C.REST_BASE_URL + "GpsLocations/bulkupload/$trackingSessionId",
                        Response.Listener {
                            response -> Log.i("LOG_RESPONSE", response!!)
                            synced = true
                            locationRepository.setPassedAllNotPassedBySessionId(session!!.sessionId)
                            Log.d("Sent sessions succeeded", result.toString())
                          },
                        Response.ErrorListener {
                            error -> Log.e("LOG_RESPONSE", error.toString())
                            Log.d("Sent sessions failed", result.toString())
                        }) {
                    override fun getBodyContentType(): String { return "application/json; charset=utf-8" }

                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>()
                        for ((key, value) in super.getHeaders()) { headers[key] = value }
                        headers["Authorization"] = "Bearer " + jwt!!
                        return headers
                    }

                    @Throws(AuthFailureError::class)
                    override fun getBody(): ByteArray? {
                        return try { mRequestBody.toByteArray(charset("utf-8")) } catch (uee: UnsupportedEncodingException) {
                            VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", mRequestBody, "utf-8")
                            return null
                        }
                    }

                    override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
                        var responseString = ""
                        responseString = response.statusCode.toString()
                        return Response.success(
                                responseString,
                                HttpHeaderParser.parseCacheHeaders(response)
                        )
                    }
                }
                requestQueue.add(stringRequest)
            } catch (e: JSONException) { e.printStackTrace() }
        }
    }

    private fun createLocationRequest() {
        mLocationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.maxWaitTime = UPDATE_INTERVAL_IN_MILLISECONDS
    }


    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()


        //stop location updates
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)

        // remove notifications
        NotificationManagerCompat.from(this).cancelAll()


        // don't forget to unregister broadcast receiver!!!!
        unregisterReceiver(broadcastReceiver)


        // broadcast stop to UI
        val intent = Intent(C.LOCATION_UPDATE_STOP)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
/*

        // send locations to backend
        val result = JSONArray()
        val locations = locationRepository.getAllBySessionId(session!!.sessionId)
        for (location in locations) {
            val requestJsonParameters = JSONObject()

            requestJsonParameters.put("recordedAt", location.recordedAt)
            requestJsonParameters.put("latitude", location.latitude.toDouble())
            requestJsonParameters.put("longitude", location.longitude.toDouble())
            requestJsonParameters.put("accuracy", location.accuracy.toDouble())
            requestJsonParameters.put("altitude", location.altitude.toDouble())
            requestJsonParameters.put("verticalAccuracy", location.verticalAccuracy.toDouble())
            requestJsonParameters.put("gpsLocationTypeId", location.locationTypeId)
            result.put(requestJsonParameters)
        }
        val handler = WebApiSingletonHandler.getInstance(applicationContext)
        Log.d("Information", "$result\n trackingId: $trackingSessionId")

        val httpRequest = object : JsonArrayRequest(
            Request.Method.POST,
            C.REST_BASE_URL + "GpsLocations/bulkupload/$trackingSessionId",
            result,
            Response.Listener { response ->
                Log.d(TAG, response.toString())
                Log.d("Information passed", "passed")
            },
            Response.ErrorListener { error ->
                Log.d(TAG, error.toString())
                Log.d("Information failed", "message :" + error.toString())
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                for ((key, value) in super.getHeaders()) {
                    headers[key] = value
                }
                headers["Authorization"] = "Bearer " + accountToken!!
                return headers
            }
        }
        handler.addToRequestQueue(httpRequest)

        try {
            val requestQueue = Volley.newRequestQueue(this)
            val mRequestBody = result.toString()
            val stringRequest: StringRequest = object : StringRequest(
                    Method.POST, C.REST_BASE_URL + "GpsLocations/bulkupload/$trackingSessionId",
                    Response.Listener { response -> Log.i("LOG_RESPONSE", response!!) },
                    Response.ErrorListener { error -> Log.e("LOG_RESPONSE", error.toString()) }) {
                override fun getBodyContentType(): String {
                    return "application/json; charset=utf-8"
                }

                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    for ((key, value) in super.getHeaders()) {
                        headers[key] = value
                    }
                    headers["Authorization"] = "Bearer " + accountToken!!
                    return headers
                }

                @Throws(AuthFailureError::class)
                override fun getBody(): ByteArray? {
                    return try {
                        mRequestBody.toByteArray(charset("utf-8"))
                    } catch (uee: UnsupportedEncodingException) {
                        VolleyLog.wtf(
                                "Unsupported Encoding while trying to get the bytes of %s using %s",
                                mRequestBody,
                                "utf-8"
                        )
                        return null
                    }
                }


                override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
                    var responseString = ""
                    responseString = response.statusCode.toString()
                    return Response.success(
                            responseString,
                            HttpHeaderParser.parseCacheHeaders(response)
                    )
                }
            }
            requestQueue.add(stringRequest)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
*/


        session?.duration = distanceOverallTime.toString()
        session?.distance = distanceOverallTotal.toString()

        sessionRepository.updateSession(session!!)

    }


    override fun onLowMemory() {
        Log.d(TAG, "onLowMemory")
        super.onLowMemory()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")

        // set counters and locations to 0/null
        currentLocation = null
        locationStart = null
        locationCP = null
        locationWP = null

        distanceOverallDirect = 0f
        distanceOverallTotal = 0f
        distanceCPDirect = 0f
        distanceCPTotal = 0f
        distanceWPDirect = 0f
        distanceWPTotal = 0f


        showNotification()

        return START_STICKY
        //return super.onStartCommand(intent, flags, startId)
    }


    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind")
        TODO("Return the communication channel to the service.")
    }

    override fun onRebind(intent: Intent?) {
        Log.d(TAG, "onRebind")
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        return super.onUnbind(intent)

    }

    fun showNotification() {
        val intentCp = Intent(C.NOTIFICATION_ACTION_CP)
        val intentWp = Intent(C.NOTIFICATION_ACTION_WP)

        val pendingIntentCp = PendingIntent.getBroadcast(this, 0, intentCp, 0)
        val pendingIntentWp = PendingIntent.getBroadcast(this, 0, intentWp, 0)

        val notifyview = RemoteViews(packageName, R.layout.map_actions)

        notifyview.setOnClickPendingIntent(R.id.imageButtonCP, pendingIntentCp)
        notifyview.setOnClickPendingIntent(R.id.imageButtonWP, pendingIntentWp)


        notifyview.setTextViewText(R.id.textViewOverallTotal, Helpers.getDistanceString(distanceOverallTotal.toString()))
        notifyview.setTextViewText(
                R.id.textViewOverallDuration, Helpers.getTimeString(
                distanceOverallTime
        )
        )
        notifyview.setTextViewText(
                R.id.textViewOverallSpeed, Helpers.getPace(
                distanceOverallTime,
                distanceOverallTotal
        )
        )

        notifyview.setTextViewText(R.id.textViewWPTotal, Helpers.getDistanceString(distanceWPTotal.toString()))
        notifyview.setTextViewText(R.id.textViewWPDuration, Helpers.getTimeString(distanceWPTime))
        notifyview.setTextViewText(
                R.id.textViewWPSpeed, Helpers.getPace(
                distanceWPTime,
                distanceWPTotal
        )
        )

        notifyview.setTextViewText(R.id.textViewCPTotal, Helpers.getDistanceString(distanceCPDirect.toString()))
        notifyview.setTextViewText(R.id.textViewCPDuration, Helpers.getTimeString(distanceCPTime))
        notifyview.setTextViewText(
                R.id.textViewCPSpeed, Helpers.getPace(
                distanceCPTime,
                distanceCPTotal
        )
        )


        // construct and show notification
        val builder = NotificationCompat.Builder(applicationContext, C.NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.baseline_location_searching_24)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        builder.setContent(notifyview)

        // Super important, start as foreground service - ie android considers this as an active app. Need visual reminder - notification.
        // must be called within 5 secs after service starts.
        startForeground(C.NOTIFICATION_ID, builder.build())

    }

    private inner class InnerBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, intent?.action!!)
            when (intent.action) {
                C.NOTIFICATION_ACTION_WP -> {
                    locationWP = currentLocation
                    distanceWPDirect = 0f
                    distanceWPTotal = 0f
                    distanceWPTime = 0
                    saveLocation(locationWP!!, C.REST_LOCATIONID_WP)
                    showNotification()
                }
                C.NOTIFICATION_ACTION_CP -> {
                    locationCP = currentLocation
                    distanceCPDirect = 0f
                    distanceCPTotal = 0f
                    distanceCPTime = 0

                    //reset WP also, since we know exactly where we are on the map
                    locationWP = currentLocation
                    distanceWPDirect = 0f
                    distanceWPTotal = 0f
                    distanceWPTime = 0

                    saveLocation(locationCP!!, C.REST_LOCATIONID_CP)
                    showNotification()
                }
            }
        }
    }


    fun isOnline(context: Context): Boolean {
        val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                } else TODO("VERSION.SDK_INT < M")
        if (capabilities != null) {
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }
}
