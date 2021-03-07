package com.example.gps_sportmap

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gps_sportmap.database.repositories.LocationRepository
import com.example.gps_sportmap.database.repositories.SessionRepository
import com.example.gps_sportmap.database.repositories.UserRepository
import kotlinx.android.synthetic.main.activity_session_history.*


class SessionHistoryActivity : AppCompatActivity() {


    private lateinit var locationRepository: LocationRepository
    private lateinit var userRepository: UserRepository
    private lateinit var adapter: RecyclerView.Adapter<*>
    private lateinit var sessionRepository: SessionRepository


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_history)

        userRepository = UserRepository(this).open()
        locationRepository = LocationRepository(this).open()
        sessionRepository = SessionRepository(this).open()


        recyclerViewSessionHistory.layoutManager = LinearLayoutManager(this)
        adapter = SessionHistoryRecyclerViewAdapter(this, sessionRepository)
        recyclerViewSessionHistory.adapter = adapter

        deleteOnSwipeLeft()

    }

    private fun deleteOnSwipeLeft() {
        val itemTouchHelperCallback =
                object :
                        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                    override fun onMove(
                            recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder
                    ): Boolean {

                        return false
                    }

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        sessionRepository.removeById(viewHolder.itemView.tag as Int)
                        (adapter as SessionHistoryRecyclerViewAdapter).refreshData()

                        adapter.notifyDataSetChanged()
                    }

                }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerViewSessionHistory)
    }


    fun logOutOnClick(view: View) {
        userRepository.setUserLoggedInState(C.REST_EMAIL, false)
        locationRepository.close()
        sessionRepository.close()
        userRepository.close()
        val intent = Intent(applicationContext, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }


    override fun onResume() {
        super.onResume()
        (adapter as SessionHistoryRecyclerViewAdapter).refreshData()

        adapter.notifyDataSetChanged()
    }


    override fun onDestroy() {
        super.onDestroy()
        sessionRepository.close()
        locationRepository.close()
        userRepository.close()
    }
}
