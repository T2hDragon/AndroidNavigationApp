package com.example.gps_sportmap.database.dto

class User {
    var userId: Int = 0
    var email: String = ""
    var password: String = ""
    var loggedIn: Boolean = false

    constructor(email: String, password: String, loggedIn: Boolean = false) : this(0, email, password, loggedIn)


    constructor(userId: Int, email: String, password: String, loggedIn: Boolean = false) {
        this.userId = userId
        this.email = email
        this.password = password
        this.loggedIn = loggedIn
    }
}