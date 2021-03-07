package com.example.gps_sportmap.database.dto

class Session {
    var sessionId: Int = 0
    var userId: Int = 0
    var sessionToken: String = ""
    var sessionName: String = ""
    var description: String = ""
    var recordedAt: String = ""
    var duration: String = ""
    var distance: String = ""

    constructor(userId: Int, sessionToken: String, sessionName: String, description: String,
                recordedAt: String, duration: String, distance: String) : this(0, userId, sessionToken, sessionName, description, recordedAt, duration, distance)


    constructor(sessionId: Int, userId: Int, sessionToken: String, sessionName: String, description: String, recordedAt: String, duration: String, distance: String) {
        this.sessionId = sessionId
        this.userId = userId
        this.sessionToken = sessionToken
        this.sessionName = sessionName
        this.description = description
        this.recordedAt = recordedAt
        this.duration = duration
        this.distance = distance
    }
}