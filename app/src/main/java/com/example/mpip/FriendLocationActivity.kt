package com.example.mpip

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class FriendLocationActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private val database = FirebaseDatabase.getInstance("https://mpip-project-ea779-default-rtdb.europe-west1.firebasedatabase.app")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))
        setContentView(R.layout.activity_friend_location)

        val friendId = intent.getStringExtra("friendId") ?: return
        mapView = findViewById(R.id.map)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val ref = database.getReference("users/$friendId/location")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitude").getValue(Double::class.java) ?: return
                val lon = snapshot.child("longitude").getValue(Double::class.java) ?: return
                showFriendLocation(lat, lon)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FriendLocationActivity, "Failed to load location", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showFriendLocation(lat: Double, lon: Double) {
        val startPoint = GeoPoint(lat, lon)
        val controller = mapView.controller
        controller.setZoom(15.0)
        controller.setCenter(startPoint)

        val friendId = intent.getStringExtra("friendId") ?: return
        val friendRef = database.getReference("users/$friendId/username")

        val marker = Marker(mapView)
        marker.position = startPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        friendRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.getValue(String::class.java) ?: "Friend"

                marker.title = "$name's Location"
                marker.setOnMarkerClickListener { _, _ ->
                    showSendThoughtsDialog(friendId)
                    true
                }

                mapView.overlays.add(marker)
                mapView.invalidate()
            }

            override fun onCancelled(error: DatabaseError) {
                marker.title = "Friend's Location"
                marker.setOnMarkerClickListener { _, _ ->
                    showSendThoughtsDialog(friendId)
                    true
                }

                mapView.overlays.add(marker)
                mapView.invalidate()
            }
        })
    }

    private fun showSendThoughtsDialog(friendId: String) {
        AlertDialog.Builder(this)
            .setTitle("Send Good Thoughts?")
            .setMessage("Do you want to send good thoughts to this friend?")
            .setPositiveButton("Yes") { _, _ ->
                //TODO: Create SendThoughtsActivity
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("friendId", friendId)
                startActivity(intent)
            }
            .setNegativeButton("No", null)
            .show()
    }

}