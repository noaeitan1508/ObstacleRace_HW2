package com.example.obstaclerace

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class ScoreRecord(val rank: Int, val distance: Int, val lat: Double, val lng: Double)

object ScoreManager {
    private const val PREFS_NAME = "ObstacleRacePrefs"
    private const val SCORES_KEY = "HighScores"

    fun saveScore(context: Context, distance: Int, lat: Double, lng: Double) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString(SCORES_KEY, "[]")
        val type = object : TypeToken<MutableList<ScoreRecord>>() {}.type
        val scores: MutableList<ScoreRecord> = gson.fromJson(json, type) ?: mutableListOf()

        scores.add(ScoreRecord(0, distance, lat, lng))

        scores.sortByDescending { it.distance }
        val top10 = scores.take(10).mapIndexed { index, record ->
            ScoreRecord(index + 1, record.distance, record.lat, record.lng)
        }

        prefs.edit().putString(SCORES_KEY, gson.toJson(top10)).apply()
    }

    fun getTopScores(context: Context): List<ScoreRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString(SCORES_KEY, "[]")
        val type = object : TypeToken<List<ScoreRecord>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}

class HighScoresActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_high_scores)

        val realScores = ScoreManager.getTopScores(this)

        val mapFragment = ScoreMapFragment()
        mapFragment.setScores(realScores)

        val listFragment = ScoreListFragment(realScores) { selectedScore ->
            mapFragment.focusOnLocation(selectedScore.lat, selectedScore.lng)
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_list, listFragment)
            .replace(R.id.frame_map, mapFragment)
            .commit()
    }
}

class ScoreListFragment(
    private val scores: List<ScoreRecord>,
    private val onScoreClick: (ScoreRecord) -> Unit
) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return RecyclerView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            layoutManager = LinearLayoutManager(context)
            adapter = ScoreAdapter(scores, onScoreClick)
        }
    }
}

class ScoreAdapter(
    private val scores: List<ScoreRecord>,
    private val clickListener: (ScoreRecord) -> Unit
) : RecyclerView.Adapter<ScoreAdapter.ScoreViewHolder>() {

    class ScoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tv_score_rank)
        val tvDistance: TextView = view.findViewById(R.id.tv_score_distance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.score_item, parent, false)
        return ScoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        val score = scores[position]
        holder.tvRank.text = "#${score.rank}"
        holder.tvDistance.text = "${score.distance}m"
        holder.itemView.setOnClickListener { clickListener(score) }
    }

    override fun getItemCount() = scores.size
}

class ScoreMapFragment : Fragment(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private var scoresList: List<ScoreRecord> = emptyList()

    fun setScores(scores: List<ScoreRecord>) {
        this.scoresList = scores
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val frameLayout = FrameLayout(requireContext()).apply {
            id = View.generateViewId()
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        val mapFragment = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction().replace(frameLayout.id, mapFragment).commit()
        mapFragment.getMapAsync(this)
        return frameLayout
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        showAllMarkers()
    }

    private fun showAllMarkers() {
        googleMap?.let { map ->
            map.clear()
            for (score in scoresList) {
                val loc = LatLng(score.lat, score.lng)
                map.addMarker(MarkerOptions().position(loc).title("Rank #${score.rank} - ${score.distance}m"))
            }
            if (scoresList.isNotEmpty()) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(scoresList[0].lat, scoresList[0].lng), 10f))
            }
        }
    }

    fun focusOnLocation(lat: Double, lng: Double) {
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f))
    }
}