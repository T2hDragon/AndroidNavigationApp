package com.example.gps_sportmap

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import java.util.concurrent.TimeUnit

class Helpers {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName

        private var mapPolylineOptions: PolylineOptions? = null
        private var mapCheckPointMarkerOptions: MutableList<MarkerOptions> = ArrayList()
        private var mapWayPointMarkerOptions: MarkerOptions? = null
        private var lastLocation: LatLng? = null

        @Synchronized
        fun getMapPolylineOptions(): PolylineOptions {
            if (mapPolylineOptions == null) {
                mapPolylineOptions = PolylineOptions()
            }
            return mapPolylineOptions!!
        }

        @Synchronized
        fun getMapCheckPointMarkerOptions(): MutableList<MarkerOptions> {
            return mapCheckPointMarkerOptions
        }

        @Synchronized
        fun getWayPointMarkerOptions(): MarkerOptions? {
            return mapWayPointMarkerOptions
        }

        fun clearMapPolylineOptions() {
            mapPolylineOptions = PolylineOptions()
        }

        fun clearMapWayPointMarkerOptions() {
            mapWayPointMarkerOptions = null
        }

        fun clearMapCheckPointMarkerOptions() {
            mapCheckPointMarkerOptions = ArrayList()
        }


        fun addToMapPolylineOptions(lat: Double, lon: Double) {
            getMapPolylineOptions().add(LatLng(lat, lon))
        }

        fun addToMapCheckPointMarkerOptions(markerOptions: MarkerOptions) {
            getMapCheckPointMarkerOptions().add(markerOptions)
        }

        fun getLastLocation(): LatLng? {
            return lastLocation
        }

        fun setMapWayPointMarkerOptions(markerOptions: MarkerOptions) {
            mapWayPointMarkerOptions = markerOptions
        }

        fun setLastLocation(latLng: LatLng) {
            lastLocation = latLng
        }


        fun getTimeString(millis: Long): String {
            return String.format(
                    "%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(
                            TimeUnit.MILLISECONDS.toHours(millis)
                    ),
                    TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(
                            TimeUnit.MILLISECONDS.toMinutes(millis)
                    )
            )
        }


        fun getPace(millis: Long, distance: Float): String {
            Log.d(TAG, "$millis-$distance")
            val speed = millis / 60.0 / distance
            if (speed > 99) return "--:--"
            val minutes = (speed).toInt()
            val seconds = ((speed - minutes) * 60).toInt()

            return minutes.toString() + ":" + (if (seconds < 10) "0" else "") + seconds.toString() + "min/km"

        }

        fun getDistanceString(distance: String): CharSequence {
            return "${String.format("%.3f", distance.toDouble() / 1000)}km"
        }


    }
}