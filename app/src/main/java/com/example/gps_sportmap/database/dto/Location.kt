package com.example.gps_sportmap.database.dto

class Location {
    var locationId: Int = 0
    var sessionId: Int = 0
    var recordedAt: String = ""
    var latitude: String = ""
    var longitude: String = ""
    var accuracy: String = ""
    var altitude: String = ""
    var verticalAccuracy: String = ""
    var locationTypeId: String = ""
    var passed: Int = 0

    constructor(sessionId: Int, recordedAt: String, latitude: String,
                longitude: String, accuracy: String, altitude: String, verticalAccuracy: String,
                locationTypeId: String, passed : Int) : this(0, sessionId, recordedAt, latitude,
            longitude, accuracy, altitude, verticalAccuracy, locationTypeId, passed)

    constructor(locationId: Int, sessionId: Int, recordedAt: String, latitude: String,
                longitude: String, accuracy: String, altitude: String, verticalAccuracy: String,
                locationTypeId: String, passed: Int) {
        this.locationId = locationId
        this.sessionId = sessionId
        this.recordedAt = recordedAt
        this.latitude = latitude
        this.longitude = longitude
        this.accuracy = accuracy
        this.altitude = altitude
        this.verticalAccuracy = verticalAccuracy
        this.locationTypeId = locationTypeId
        this.passed = passed
    }

    fun notPassed() : Location {
        this.passed = 0
        return this
    }
}