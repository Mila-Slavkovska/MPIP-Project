package com.example.mpip

import android.annotation.SuppressLint
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.android.gms.location.*

class UserProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth;
    private var user: FirebaseUser? = null;

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var lastSavedLocation: android.location.Location? = null
    private lateinit var locationText: TextView

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private fun goToLogin() {
        val intent = Intent(applicationContext, LoginActivity::class.java);
        startActivity(intent);
        finish();
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance();
        user = auth.currentUser;

        val button: Button = findViewById(R.id.logout);
        val textView: TextView = findViewById(R.id.user_details);
        val friendsButton: ImageButton = findViewById(R.id.friends_btn);
        val mailboxButton: Button = findViewById(R.id.mailbox);

        if(user == null){
            goToLogin();
        } else {
            textView.text = user!!.email;
            val database = FirebaseDatabase.getInstance("https://mpip-project-ea779-default-rtdb.europe-west1.firebasedatabase.app")
            val userReference = database.getReference("users").child(user!!.uid)

            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val username = snapshot.child("username").getValue(String::class.java) ?: "N/A"
                        val email = snapshot.child("email").getValue(String::class.java) ?: "N/A"
                        val latitude = snapshot.child("location").child("latitude").value ?: "N/A"
                        val longitude = snapshot.child("location").child("longitude").value ?: "N/A"
                        val id = user!!.uid

                        textView.text = "Username: $username\nEmail: $email\nCode: $id"
                        locationText.text = "Lat: ${latitude}, Lng: ${longitude}"
                    } else {
                        textView.text = "No user data found"
                    }
                }

                @SuppressLint("SetTextI18n")
                override fun onCancelled(error: DatabaseError) {
                    textView.text = "Failed to load user data: ${error.message}"
                }
            })

            button.setOnClickListener {
                FirebaseAuth.getInstance().signOut();
                goToLogin();
            }

            locationText = findViewById(R.id.location)
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            checkPermissionAndStartUpdates()
        }

        friendsButton.setOnClickListener {
            val intent = Intent(applicationContext, FriendListActivity::class.java)
            startActivity(intent)
        }

        mailboxButton.setOnClickListener {
            val intent = Intent(applicationContext, MailboxActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkPermissionAndStartUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 43200000)
            .setMinUpdateDistanceMeters(200f)
            .build()

        locationCallback = object : LocationCallback() {
            @SuppressLint("SetTextI18n")
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    if (lastSavedLocation == null || location.distanceTo(lastSavedLocation!!) >= 200) {
                        val database = FirebaseDatabase.getInstance("https://mpip-project-ea779-default-rtdb.europe-west1.firebasedatabase.app")
                        val reference = database.getReference("users").child(user!!.uid).child("location")

                        val locData = mapOf(
                            "latitude" to location.latitude,
                            "longitude" to location.longitude,
                            "timestamp" to System.currentTimeMillis()
                        )

                        reference.setValue(locData)
                        lastSavedLocation = location
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionAndStartUpdates()
    }
}