package com.example.gps_sportmap.database;

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


class DbHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "app.db"
        const val DATABASE_VERSION = 3

        const val USER_TABLE_NAME = "USER"
        const val SESSION_TABLE_NAME = "SESSION"
        const val LOCATION_TABLE_NAME = "LOCATION"

        const val USER_ID = "USER_ID"
        const val PASSWORD = "PASSWORD"
        const val EMAIL = "EMAIL"
        const val LOGGED_IN = "LOGGED_IN"

        const val SESSION_ID = "session_id"
        const val SESSION_TOKEN = "session_token"
        const val SESSION_NAME = "session_name"
        const val DESCRIPTION = "description"
        const val RECORDED_AT = "recorded_at"
        const val DURATION = "duration"
        const val DISTANCE = "distance"

        const val LOCATION_ID = "location_id"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val ACCURACY = "accuracy"
        const val ALTITUDE = "altitude"
        const val VERTICAL_ACCURACY = "vertical_accuracy"
        const val LOCATION_TYPE_ID = "location_type_id"
        const val PASSED = "passed"


        const val SQL_USER_CREATE_TABLE =
                "create table $USER_TABLE_NAME (" +
                        "$USER_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "$EMAIL TEXT NOT NULL, " +
                        "$PASSWORD TEXT NOT NULL, " +
                        "$LOGGED_IN INT NOT NULL " +
                        ");"


        const val SQL_SESSION_CREATE_TABLE =
                "create table $SESSION_TABLE_NAME (" +
                        "$SESSION_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "$USER_ID TEXT NULL, " +
                        "$SESSION_TOKEN TEXT NULL, " +
                        "$SESSION_NAME TEXT NOT NULL, " +
                        "$DESCRIPTION TEXT NOT NULL, " +
                        "$RECORDED_AT TEXT NOT NULL, " +
                        "$DURATION TEXT NOT NULL, " +
                        "$DISTANCE TEXT NOT NULL, " +
                        "CONSTRAINT fk_user_session FOREIGN KEY($USER_ID) REFERENCES $USER_TABLE_NAME($USER_ID) ON DELETE CASCADE);"

        const val SQL_LOCATION_CREATE_TABLE =
                "create table $LOCATION_TABLE_NAME (" +
                        "$LOCATION_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "$SESSION_ID INTEGER NOT NULL, " +
                        "$RECORDED_AT TEXT NOT NULL, " +
                        "$LATITUDE TEXT NOT NULL, " +
                        "$LONGITUDE TEXT NOT NULL, " +
                        "$ACCURACY TEXT NOT NULL, " +
                        "$VERTICAL_ACCURACY TEXT NOT NULL, " +
                        "$ALTITUDE TEXT NOT NULL, " +
                        "$LOCATION_TYPE_ID TEXT NOT NULL, " +
                        "$PASSED INT NOT NULL, " +
                        "CONSTRAINT fk_session_location FOREIGN KEY($SESSION_ID) REFERENCES $SESSION_TABLE_NAME($SESSION_ID) ON DELETE CASCADE);"

        const val SQL_DELETE_USER_TABLES = "DROP TABLE IF EXISTS $USER_TABLE_NAME;"
        const val SQL_DELETE_SESSION_TABLES = "DROP TABLE IF EXISTS $SESSION_TABLE_NAME;"
        const val SQL_DELETE_LOCATION_TABLES = "DROP TABLE IF EXISTS $LOCATION_TABLE_NAME;"
    }


    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_USER_CREATE_TABLE)
        db?.execSQL(SQL_SESSION_CREATE_TABLE)
        db?.execSQL(SQL_LOCATION_CREATE_TABLE)
    }


    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(SQL_DELETE_LOCATION_TABLES)
        db?.execSQL(SQL_DELETE_SESSION_TABLES)
        db?.execSQL(SQL_DELETE_USER_TABLES)
        onCreate(db)
    }

    fun reset(db: SQLiteDatabase?) {
        db?.execSQL(SQL_DELETE_LOCATION_TABLES)
        db?.execSQL(SQL_DELETE_SESSION_TABLES)
        db?.execSQL(SQL_DELETE_USER_TABLES)
        onCreate(db)
    }

}