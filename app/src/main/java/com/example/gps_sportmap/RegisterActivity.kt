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
import kotlinx.android.synthetic.main.activity_register.*
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
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

    fun onClickRegister(view: View) {
        val requestJsonParameters = JSONObject()
        requestJsonParameters.put("email", editTextRegisterEmail.text.toString())
        requestJsonParameters.put("password", editTextRegisterPassword.text.toString())
        requestJsonParameters.put("firstName", editTextRegisterFirstName.text.toString())
        requestJsonParameters.put("lastName", editTextRegisterLastName.text.toString())
        val handler = WebApiSingletonHandler.getInstance(applicationContext)
        val httpRequest = JsonObjectRequest(
                Request.Method.POST,
                C.REST_BASE_URL + "account/register",
                requestJsonParameters,
                { response ->
                    Log.d(TAG, "response received: $response")
                    textViewRegisterMessage.setTextColor(Color.CYAN)
                    C.REST_EMAIL = editTextRegisterEmail.text.toString()
                    C.REST_PASSWORD = editTextRegisterPassword.text.toString()
                    C.REST_TOKEN = response.getString("token")
                    val user: User = User(C.REST_EMAIL, C.REST_PASSWORD, true)
                    if (userRepository.getByEmail(C.REST_EMAIL) == null) userRepository.add(user)
                    C.USER_ID = user.userId
                    val intent = Intent(applicationContext, MapsActivity::class.java)
                    userRepository.close()
                    editTextRegisterEmail.setText("")
                    editTextRegisterPassword.setText("")
                    editTextRegisterFirstName.setText("")
                    editTextRegisterLastName.setText("")
                    startActivity(intent)
                },
                { error ->
                    Log.d(TAG, "response error:$error")
                    textViewRegisterMessage.setTextColor(Color.RED)
                    textViewRegisterMessage.text = "Email taken"
                }
        )
        handler.addToRequestQueue(httpRequest)
    }

    fun OnClickBackToLogin(view: View) {
        userRepository.close()
        finish()
    }
}