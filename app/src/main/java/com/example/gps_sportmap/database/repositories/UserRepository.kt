package com.example.gps_sportmap.database.repositories

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.gps_sportmap.database.DbHelper
import com.example.gps_sportmap.database.dto.User

class UserRepository(val context: Context) {

    private lateinit var dbHelper: DbHelper
    private lateinit var db: SQLiteDatabase

    fun open(): UserRepository {
        dbHelper = DbHelper(context)
        db = dbHelper.writableDatabase
/*
        dbHelper.reset(db)
*/


        return this
    }

    fun close() {
        dbHelper.close()
    }


    fun getUserLoggedIn(): User? {
        val columns =
                arrayOf(DbHelper.USER_ID, DbHelper.EMAIL, DbHelper.PASSWORD, DbHelper.LOGGED_IN)


        val cursor = db.query(DbHelper.USER_TABLE_NAME, columns, "${DbHelper.LOGGED_IN} = ?", arrayOf("1"), null, null, null)
        if (!cursor.moveToNext()) {
            return null
        }
        return User(
                cursor.getString(cursor.getColumnIndex(DbHelper.USER_ID)).toInt(),
                cursor.getString(cursor.getColumnIndex(DbHelper.EMAIL)),
                cursor.getString(cursor.getColumnIndex(DbHelper.PASSWORD)),
                cursor.getString(cursor.getColumnIndex(DbHelper.LOGGED_IN)).toInt() == 1
        )
    }

    fun setUserLoggedInState(email: String, isLoggedIn: Boolean) {
        val columns =
                arrayOf(DbHelper.USER_ID, DbHelper.EMAIL, DbHelper.PASSWORD, DbHelper.LOGGED_IN)
        val contentValues = ContentValues()
        contentValues.put(DbHelper.LOGGED_IN, if (isLoggedIn) "1" else "0")

        val cursor = db.update(DbHelper.USER_TABLE_NAME, contentValues, "${DbHelper.EMAIL} = ?", arrayOf(email))
    }

    fun add(user: User) {
        val contentValues = ContentValues()
        contentValues.put(DbHelper.EMAIL, user.email)
        contentValues.put(DbHelper.PASSWORD, user.password)
        contentValues.put(DbHelper.LOGGED_IN, if (user.loggedIn) "1" else "0")
        user.userId = db.insert(DbHelper.USER_TABLE_NAME, null, contentValues).toInt()
    }

    fun getByUserId(userId: Int): User? {
        val columns =
                arrayOf(DbHelper.USER_ID, DbHelper.EMAIL, DbHelper.PASSWORD, DbHelper.LOGGED_IN)


        val cursor = db.query(DbHelper.USER_TABLE_NAME, columns, "${DbHelper.USER_ID} = ?", arrayOf("$userId"), null, null, null)
        if (cursor.moveToNext()) {
            return User(
                    cursor.getString(cursor.getColumnIndex(DbHelper.USER_ID)).toInt(),
                    cursor.getString(cursor.getColumnIndex(DbHelper.EMAIL)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.PASSWORD)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.LOGGED_IN)).toInt() == 1
            )
        }
        return null
    }

    fun getByEmail(email: String): User? {
        val columns =
                arrayOf(DbHelper.USER_ID, DbHelper.EMAIL, DbHelper.PASSWORD, DbHelper.LOGGED_IN)


        val cursor = db.query(DbHelper.USER_TABLE_NAME, columns, "${DbHelper.EMAIL} = ?", arrayOf(email), null, null, null)
        if (cursor.moveToNext()) {
            return User(
                    cursor.getString(cursor.getColumnIndex(DbHelper.USER_ID)).toInt(),
                    cursor.getString(cursor.getColumnIndex(DbHelper.EMAIL)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.PASSWORD)),
                    cursor.getString(cursor.getColumnIndex(DbHelper.LOGGED_IN)).toInt() == 1
            )
        }
        return null
    }


    fun removeById(userId: Int) {
        db.execSQL("DELETE FROM ${DbHelper.USER_TABLE_NAME} WHERE ${DbHelper.USER_TABLE_NAME}.${DbHelper.USER_ID}=$userId;")

    }

}