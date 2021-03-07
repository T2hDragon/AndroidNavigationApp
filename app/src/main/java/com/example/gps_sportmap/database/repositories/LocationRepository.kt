package com.example.gps_sportmap.database.repositories

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.gps_sportmap.database.DbHelper
import com.example.gps_sportmap.database.dto.Location


class LocationRepository(val context: Context) {

    private lateinit var dbHelper: DbHelper
    private lateinit var db: SQLiteDatabase

    fun open(): LocationRepository {
        dbHelper = DbHelper(context)
        db = dbHelper.writableDatabase

        return this
    }

    fun close() {
        dbHelper.close()
    }

    fun addLocation(location: Location) {
        val contentValues = ContentValues()
        contentValues.put(DbHelper.SESSION_ID, location.sessionId)
        contentValues.put(DbHelper.RECORDED_AT, location.recordedAt)
        contentValues.put(DbHelper.LATITUDE, location.latitude)
        contentValues.put(DbHelper.LONGITUDE, location.longitude)
        contentValues.put(DbHelper.ACCURACY, location.accuracy)
        contentValues.put(DbHelper.ALTITUDE, location.altitude)
        contentValues.put(DbHelper.VERTICAL_ACCURACY, location.verticalAccuracy)
        contentValues.put(DbHelper.LOCATION_TYPE_ID, location.locationTypeId)
        contentValues.put(DbHelper.PASSED, location.passed)
        location.locationId = db.insert(DbHelper.LOCATION_TABLE_NAME, null, contentValues).toInt()
    }


    fun getByLocationId(locationId: Int): Location {
        val columns =
                arrayOf(DbHelper.LOCATION_ID, DbHelper.SESSION_ID, DbHelper.RECORDED_AT, DbHelper.LATITUDE, DbHelper.LONGITUDE,
                        DbHelper.ACCURACY, DbHelper.ALTITUDE, DbHelper.VERTICAL_ACCURACY, DbHelper.LOCATION_TYPE_ID, DbHelper.PASSED)

        val cursor = db.query(DbHelper.LOCATION_TABLE_NAME, columns, "${DbHelper.LOCATION_ID} = ?", arrayOf("$locationId"), null, null, DbHelper.LOCATION_ID)
        if (cursor.moveToNext()) {
            return Location(
                    cursor.getString(cursor.getColumnIndex(DbHelper.LOCATION_ID)).toInt(),
                    cursor.getString(cursor.getColumnIndex(DbHelper.SESSION_ID)).toInt(),
                    cursor.getString(cursor.getColumnIndex(DbHelper.RECORDED_AT)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.LATITUDE)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.LONGITUDE)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.ACCURACY)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.ALTITUDE)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.VERTICAL_ACCURACY)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.LOCATION_TYPE_ID)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.PASSED)).toInt()
            )
        }
        throw Exception("No such location")
    }

    fun getAllBySessionId(sessionId: Int): List<Location> {
        val locations = ArrayList<Location>()

        val columns =
                arrayOf(DbHelper.LOCATION_ID, DbHelper.SESSION_ID, DbHelper.RECORDED_AT, DbHelper.LATITUDE, DbHelper.LONGITUDE,
                        DbHelper.ACCURACY, DbHelper.ALTITUDE, DbHelper.VERTICAL_ACCURACY, DbHelper.LOCATION_TYPE_ID, DbHelper.PASSED)

        val cursor = db.query(DbHelper.LOCATION_TABLE_NAME, columns, "${DbHelper.SESSION_ID} = ?", arrayOf("$sessionId"), null, null, DbHelper.LOCATION_ID)
        while (cursor.moveToNext()) {
            locations.add(Location(
                    cursor.getString(cursor.getColumnIndex(DbHelper.LOCATION_ID)).toInt(),
                    cursor.getString(cursor.getColumnIndex(DbHelper.SESSION_ID)).toInt(),
                    cursor.getString(cursor.getColumnIndex(DbHelper.RECORDED_AT)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.LATITUDE)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.LONGITUDE)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.ACCURACY)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.ALTITUDE)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.VERTICAL_ACCURACY)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.LOCATION_TYPE_ID)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.PASSED)).toInt()

            ))
        }
        return locations
    }

    fun setPassedAllNotPassedBySessionId(sessionId: Int){
        val locations = ArrayList<Location>()

        val columns =
                arrayOf(DbHelper.LOCATION_ID, DbHelper.SESSION_ID, DbHelper.RECORDED_AT, DbHelper.LATITUDE, DbHelper.LONGITUDE,
                        DbHelper.ACCURACY, DbHelper.ALTITUDE, DbHelper.VERTICAL_ACCURACY, DbHelper.LOCATION_TYPE_ID, DbHelper.PASSED)

        val select = "UPDATE ${DbHelper.LOCATION_TABLE_NAME} SET ${DbHelper.PASSED} = 1 WHERE ${DbHelper.SESSION_ID} = $sessionId AND ${DbHelper.PASSED} = 0"

        val cursor = db.rawQuery(select, null)
        cursor.moveToFirst()
        cursor.close()
    }

    fun getPassedAllNotPassedBySessionId(sessionId: Int): List<Location> {
        val locations = ArrayList<Location>()

        val columns =
                arrayOf(DbHelper.LOCATION_ID, DbHelper.SESSION_ID, DbHelper.RECORDED_AT, DbHelper.LATITUDE, DbHelper.LONGITUDE,
                        DbHelper.ACCURACY, DbHelper.ALTITUDE, DbHelper.VERTICAL_ACCURACY, DbHelper.LOCATION_TYPE_ID, DbHelper.PASSED)

        val select = "SELECT * FROM ${DbHelper.LOCATION_TABLE_NAME} WHERE ${DbHelper.SESSION_ID} = $sessionId AND ${DbHelper.PASSED} = 0"

        val cursor = db.rawQuery(select, null)
        while (cursor.moveToNext()) {
            locations.add(Location(
                    cursor.getString(cursor.getColumnIndex(DbHelper.LOCATION_ID)).toInt(),
                    cursor.getString(cursor.getColumnIndex(DbHelper.SESSION_ID)).toInt(),
                    cursor.getString(cursor.getColumnIndex(DbHelper.RECORDED_AT)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.LATITUDE)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.LONGITUDE)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.ACCURACY)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.ALTITUDE)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.VERTICAL_ACCURACY)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.LOCATION_TYPE_ID)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.PASSED)).toInt()

            ))
        }
        return locations
    }



    fun updateLocation(location: Location) {

        val contentValues = ContentValues()
        contentValues.put(DbHelper.SESSION_ID, location.sessionId)
        contentValues.put(DbHelper.RECORDED_AT, location.recordedAt)
        contentValues.put(DbHelper.LATITUDE, location.latitude)
        contentValues.put(DbHelper.LONGITUDE, location.longitude)
        contentValues.put(DbHelper.ACCURACY, location.accuracy)
        contentValues.put(DbHelper.ALTITUDE, location.altitude)
        contentValues.put(DbHelper.VERTICAL_ACCURACY, location.verticalAccuracy)
        contentValues.put(DbHelper.LOCATION_TYPE_ID, location.locationTypeId)
        contentValues.put(DbHelper.PASSED, location.passed)

        db.update(DbHelper.LOCATION_TABLE_NAME, contentValues, "${DbHelper.LOCATION_ID} = ?", arrayOf("${location.locationId}"))
    }

    fun removeById(locationId: Int) {
        db.execSQL("DELETE FROM ${DbHelper.LOCATION_TABLE_NAME} WHERE ${DbHelper.LOCATION_TABLE_NAME}.${DbHelper.LOCATION_ID}=$locationId;")

    }

}