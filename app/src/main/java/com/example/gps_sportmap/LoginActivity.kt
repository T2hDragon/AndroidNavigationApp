package com.example.gps_sportmap

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.example.gps_sportmap.database.dto.User
import com.example.gps_sportmap.database.repositories.UserRepository
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        userRepository = UserRepository(this).open()
        val user: User? = userRepository.getUserLoggedIn()
        if (user != null) {
            C.USER_ID = user.userId
            C.REST_PASSWORD = user.password
            C.REST_EMAIL = user.email
            val i = Intent(applicationContext, MapsActivity::class.java)
            startActivity(i)
        }
    }

    override fun onResume() {
        super.onResume()
        userRepository.open()
        val user: User? = userRepository.getUserLoggedIn()
        if (user != null) {
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        userRepository.close()
    }

    fun onClickLogIn(view: View) {
        val requestJsonParameters = JSONObject()
        requestJsonParameters.put("email", editTextEmail.text.toString())
        requestJsonParameters.put("password", editTextPassword.text.toString())
        val handler = WebApiSingletonHandler.getInstance(applicationContext)
        val httpRequest = JsonObjectRequest(
                Request.Method.POST,
                C.REST_BASE_URL + "account/login",
                requestJsonParameters,
                { response ->
                    Log.d(TAG, "response received: $response")
                    textViewMessage.setTextColor(Color.CYAN)
                    C.REST_EMAIL = editTextEmail.text.toString()
                    C.REST_PASSWORD = editTextPassword.text.toString()
                    C.REST_TOKEN = response.getString("token")
                    val user = User(C.REST_EMAIL, C.REST_PASSWORD, true)
                    if (userRepository.getByEmail(C.REST_EMAIL) == null) userRepository.add(user) else {
                        userRepository.setUserLoggedInState(C.REST_EMAIL, true)
                    }
                    C.USER_ID = user.userId
                    val intent = Intent(applicationContext, MapsActivity::class.java)
                    userRepository.close()
                    editTextEmail.setText("")
                    editTextPassword.setText("")
                    startActivity(intent)
                },
                { error ->
                    Log.d(TAG, "response error:$error")
                    textViewMessage.setTextColor(Color.RED)
                    textViewMessage.text = "Incorrect email or password"
                }
        )
        handler.addToRequestQueue(httpRequest)
    }

    fun onClickRegister(view: View) {
        val intent = Intent(applicationContext, RegisterActivity::class.java)
        userRepository.close()
        startActivity(intent)
    }


}

