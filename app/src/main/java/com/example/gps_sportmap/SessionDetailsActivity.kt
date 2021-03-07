package com.example.gps_sportmap


import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.android.volley.AuthFailureError
import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.VolleyLog
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.gps_sportmap.database.dto.Location
import com.example.gps_sportmap.database.dto.Session
import com.example.gps_sportmap.database.repositories.LocationRepository
import com.example.gps_sportmap.database.repositories.SessionRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.session_details.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.text.SimpleDateFormat
import java.util.*


@Suppress("NAME_SHADOWING")
class SessionDetailsActivity : AppCompatActivity(), OnMapReadyCallback {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    private lateinit var mMap: GoogleMap
    private var marker: Marker? = null

    private lateinit var locations: List<Location>
    private lateinit var offlineLocations: List<Location>
    private lateinit var session: Session

    private lateinit var locationRepository: LocationRepository
    private lateinit var sessionRepository: SessionRepository

    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
    private val representableDateFormat = SimpleDateFormat(
            "dd-MM-yyyy' 'HH:mm",
            Locale.getDefault()
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.session_details)

        locationRepository = LocationRepository(this).open()
        sessionRepository = SessionRepository(this).open()
        val sessionId = intent.getStringExtra("SessionId")!!.toInt()
        session = sessionRepository.getById(sessionId)
        locations = locationRepository.getAllBySessionId(session.sessionId)
        offlineLocations = locationRepository.getPassedAllNotPassedBySessionId(session.sessionId)
        locationRepository.close()
        sessionRepository.close()

        textViewSessionDetailsSession.text = session.sessionName
        textViewSessionDetailsDescription.text = session.description
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            textViewSessionDetailsRecordedAt.text = representableDateFormat.format(dbDateFormat.parse(session.recordedAt)!!)
        } else textViewSessionDetailsRecordedAt.text = session.recordedAt
        textViewSessionDetailsDistance.text = Helpers.getDistanceString(session.distance)
        textViewSessionDetailsDuration.text = Helpers.getTimeString(session.duration.toLong())


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

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
        val startingLocation = LatLng(
                locations.first().latitude.toDouble(),
                locations.first().longitude.toDouble()
        )

        mMap.moveCamera(CameraUpdateFactory.zoomTo(17.0f))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(startingLocation))

        val helper = Helpers
        for (location in locations) {
            if (location.locationTypeId == C.REST_LOCATIONID_LOC) {
                helper.addToMapPolylineOptions(
                        location.latitude.toDouble(),
                        location.longitude.toDouble()
                )
            }
            if (location.locationTypeId == C.REST_LOCATIONID_CP) {
                val markOptions = MarkerOptions()
                        .position(LatLng(location.latitude.toDouble(), location.longitude.toDouble()))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.checkpoint))
                marker = mMap.addMarker(markOptions)
                helper.addToMapCheckPointMarkerOptions(markOptions)
            }

        }
        helper.getMapCheckPointMarkerOptions().forEach { markerOptions ->
            marker = mMap.addMarker(
                    markerOptions
            )
        }
        mMap.addPolyline(helper.getMapPolylineOptions())


    }


    fun onClickExport(view: View) {

        val fileName = "${session.sessionName}(${session.recordedAt})"
        var file = File(this.getExternalFilesDir(null)!!.absolutePath, "$fileName.gpx")
        var fileCreated = file.createNewFile()
        var i = 0
        while (!fileCreated) {
            file = File(this.getExternalFilesDir(null)!!.absolutePath, "$fileName$i.gpx")
            fileCreated = file.createNewFile()
            i++
        }
        generateGpx(
                file,
                session.description,
                "https://sportmap.akaver.com/GpsSessions",
                "Running",
                locations
        )
        val uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file)
        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri)
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        emailIntent.type = "*/*"
        startActivity(emailIntent)
    }

    fun onClickUploadToBackend(view: View){
        // send locations to backend
        val result = JSONArray()
        for (location in offlineLocations) {
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
                    Method.POST, C.REST_BASE_URL + "GpsLocations/bulkupload/${session.sessionToken}",
                    Response.Listener {
                        response -> Log.i("LOG_RESPONSE", response!!)
                        locationRepository.setPassedAllNotPassedBySessionId(session!!.sessionId)
                    },
                    Response.ErrorListener {
                        error -> Log.e("LOG_RESPONSE", error.toString())
                    }) {
                override fun getBodyContentType(): String { return "application/json; charset=utf-8" }

                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    for ((key, value) in super.getHeaders()) { headers[key] = value }
                    headers["Authorization"] = "Bearer " + C.REST_TOKEN!!
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


    fun generateGpx(
            file: File?,
            cmt: String,
            pageUrl: String,
            type: String,
            locations: List<Location>
    ) {
        var cmt = cmt
        var pageUrl = pageUrl
        var type = type
        val header =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n" +
                        "<gpx version=\"1.1\" creator=\"Endomondo.com\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.garmin.com/xmlschemas/GpxExtensions/v3 http://www.garmin.com/xmlschemas/GpxExtensionsv3.xsd http://www.garmin.com/xmlschemas/TrackPointExtension/v1 http://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\" xmlns:gpxx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
        cmt = "<cmt>$cmt</cmt>\n"
        pageUrl = "<pageUrl>$pageUrl</pageUrl>\n"
        type = "<type>$type =</type><trkseg>\n"
        var trackPoints = ""
        var checkpoints = ""
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        for (location in locations) {
            if (location.locationTypeId != C.REST_LOCATIONID_WP) {
                val date = dbDateFormat.parse(location.recordedAt)
                if (location.locationTypeId == C.REST_LOCATIONID_LOC) {
                    trackPoints += """
    <trkpt lat="${location.latitude}" lon="${location.longitude}">
        <ele>${location.altitude}</ele>
        <time>${df.format(date!!.time)}</time>
    </trkpt>"""
                } else {
                    checkpoints += """
    <wpt lat="${location.latitude}" lon="${location.longitude}">
        <ele>${location.altitude}</ele>
    </wpt>"""
                }
            }
        }
        val footer = "</trkseg></trk></gpx>"
        try {
            val writer = FileWriter(file, false)
            writer.append(header)
            writer.append(checkpoints)
            writer.append("<metadata>\n")
            writer.append("<author>\n")
            writer.append("<email>${C.REST_EMAIL}</email>\n")
            writer.append("</author>\n")
            writer.append("</metadata>\n")
            writer.append("<trk>\n")
            writer.append(cmt)
            writer.append(pageUrl)
            writer.append(type)
            writer.append(trackPoints)
            writer.append(footer)
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            Log.e("generateGfx", "Error Writting Path", e)
        }
    }


    // ============================================== LIFECYCLE CALLBACKS =============================================
    override fun onStart() {
        Log.d(TAG, "onStart")
        super.onStart()
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()

    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()

    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        locationRepository.close()
        sessionRepository.close()
    }

    override fun onRestart() {
        Log.d(TAG, "onRestart")
        super.onRestart()
    }


}
