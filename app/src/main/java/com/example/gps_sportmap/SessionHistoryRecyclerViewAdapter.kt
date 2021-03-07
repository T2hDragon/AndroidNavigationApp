package com.example.gps_sportmap

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gps_sportmap.database.dto.Session
import com.example.gps_sportmap.database.repositories.SessionRepository
import kotlinx.android.synthetic.main.session_row_view.view.*
import java.text.SimpleDateFormat
import java.util.*

class SessionHistoryRecyclerViewAdapter(
        context: Context,
        private val sessionRepository: SessionRepository
) : RecyclerView.Adapter<SessionHistoryRecyclerViewAdapter.ViewHolder>() {

    private lateinit var sessionDataSet: List<Session>
    private val dbDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
    private val representableDateFormat = SimpleDateFormat("dd-MM-yyyy' 'HH:mm", Locale.getDefault())


    fun refreshData() {
        sessionDataSet = sessionRepository.getAllByUserId(C.USER_ID)
    }

    init {
        refreshData()
    }

    private val inflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val rowView = inflater.inflate(R.layout.session_row_view, parent, false)
        return ViewHolder(rowView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = sessionDataSet[position]
        //https://stackoverflow.com/questions/55492820/how-to-open-a-new-activity-from-clicking-an-item-from-recyclerview
        holder.itemView.setOnClickListener { view ->
            val context = view!!.context
            val intent = Intent(context, SessionDetailsActivity::class.java)
            intent.putExtra("SessionId", session.sessionId.toString())
            context.startActivity(intent)
        }



        holder.itemView.tag = session.sessionId
        holder.itemView.textViewSessionName.text = session.sessionName
        holder.itemView.textViewSessionDescription.text = session.description
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            holder.itemView.textViewSessionStartAt.text = representableDateFormat.format(dbDateFormat.parse(session.recordedAt)!!)
        } else holder.itemView.textViewSessionStartAt.text = session.recordedAt

        holder.itemView.textViewDistance.text = Helpers.getDistanceString(session.distance)
        holder.itemView.textViewSessionDuration.text = Helpers.getTimeString(session.duration.toLong())
    }

    override fun getItemCount(): Int {
        return sessionDataSet.count()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

}