package com.example.gps_sportmap.database.repositories

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.gps_sportmap.database.DbHelper
import com.example.gps_sportmap.database.dto.Session

class SessionRepository(val context: Context) {

    private lateinit var dbHelper: DbHelper
    private lateinit var db: SQLiteDatabase

    fun open(): SessionRepository {
        dbHelper = DbHelper(context)
        db = dbHelper.writableDatabase

        return this
    }

    fun close() {
        dbHelper.close()
    }

    fun addSession(session: Session): Session {
        val contentValues = ContentValues()
        contentValues.put(DbHelper.USER_ID, session.userId)
        contentValues.put(DbHelper.SESSION_TOKEN, session.sessionToken)
        contentValues.put(DbHelper.SESSION_NAME, session.sessionName)
        contentValues.put(DbHelper.DESCRIPTION, session.description)
        contentValues.put(DbHelper.RECORDED_AT, session.recordedAt)
        contentValues.put(DbHelper.DURATION, session.duration)
        contentValues.put(DbHelper.DISTANCE, session.distance)
        session.sessionId = db.insert(DbHelper.SESSION_TABLE_NAME, null, contentValues).toInt()
        return session

    }


    fun getById(sessionId: Int): Session {
        val columns =
                arrayOf(DbHelper.SESSION_ID, DbHelper.USER_ID, DbHelper.SESSION_TOKEN,
                        DbHelper.SESSION_NAME, DbHelper.DESCRIPTION, DbHelper.RECORDED_AT,
                        DbHelper.DURATION, DbHelper.DISTANCE)


        val cursor = db.query(DbHelper.SESSION_TABLE_NAME, columns, "${DbHelper.SESSION_ID} = ?", arrayOf("$sessionId"), null, null, DbHelper.SESSION_ID)
        if (cursor.moveToNext()) {
            return Session(
                    cursor.getString(cursor.getColumnIndex(DbHelper.SESSION_ID)).toInt(),
                    cursor.getString(cursor.getColumnIndex(DbHelper.USER_ID)).toInt(),
                    cursor.getString(cursor.getColumnIndex(DbHelper.SESSION_TOKEN)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.SESSION_NAME)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.DESCRIPTION)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.RECORDED_AT)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.DURATION)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.DISTANCE))
            )
        }
        throw Exception("No such session")
    }

    fun getAllByUserId(userId: Int): List<Session> {
        val sessions = ArrayList<Session>()

        val columns =
                arrayOf(DbHelper.SESSION_ID, DbHelper.USER_ID, DbHelper.SESSION_TOKEN,
                        DbHelper.SESSION_NAME, DbHelper.DESCRIPTION, DbHelper.RECORDED_AT,
                        DbHelper.DURATION, DbHelper.DISTANCE)


        val cursor = db.query(DbHelper.SESSION_TABLE_NAME, columns, "${DbHelper.USER_ID} = ?", arrayOf("$userId"), null, null, DbHelper.SESSION_ID + " DESC")
        while (cursor.moveToNext()) {
            sessions.add(Session(
                    cursor.getString(cursor.getColumnIndex(DbHelper.SESSION_ID)).toInt(),
                    cursor.getString(cursor.getColumnIndex(DbHelper.USER_ID)).toInt(),
                    cursor.getString(cursor.getColumnIndex(DbHelper.SESSION_TOKEN)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.SESSION_NAME)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.DESCRIPTION)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.RECORDED_AT)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.DURATION)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.DISTANCE))
            ))
        }
        return sessions
    }

    fun updateSession(session: Session) {

        val contentValues = ContentValues()
        contentValues.put(DbHelper.SESSION_ID, session.sessionId)
        contentValues.put(DbHelper.USER_ID, session.userId)
        contentValues.put(DbHelper.SESSION_TOKEN, session.sessionToken)
        contentValues.put(DbHelper.SESSION_NAME, session.sessionName)
        contentValues.put(DbHelper.DESCRIPTION, session.description)
        contentValues.put(DbHelper.RECORDED_AT, session.recordedAt)
        contentValues.put(DbHelper.DURATION, session.duration)
        contentValues.put(DbHelper.DISTANCE, session.distance)

        db.update(DbHelper.SESSION_TABLE_NAME, contentValues, "${DbHelper.SESSION_ID} = ?", arrayOf("${session.sessionId}"))
    }

    fun removeById(sessionId: Int) {
        db.execSQL("DELETE FROM ${DbHelper.SESSION_TABLE_NAME} WHERE ${DbHelper.SESSION_TABLE_NAME}.${DbHelper.SESSION_ID}=$sessionId;")

    }

}