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
import androidx.core.content.ContextCompat
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
import org.w3c.dom.Text

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
        val friendsButton: ImageButton = findViewById(R.id.friends_btn);
        val mailboxButton: Button = findViewById(R.id.mailbox);
        val drawable = ContextCompat.getDrawable(this, R.drawable.mailbox)

        drawable?.setBounds(0, 0, 64, 64)

        mailboxButton.setCompoundDrawables(
            drawable,
            null,
            null,
            null
        )
        mailboxButton.compoundDrawablePadding = 16

        val usernameTextView: TextView = findViewById(R.id.username_text)
        val emailTextView: TextView = findViewById(R.id.email_text)
        val codeTextView: TextView = findViewById(R.id.code_text)
        val copyButton: ImageButton = findViewById(R.id.copy_code_btn)

        if(user == null){
            goToLogin();
        } else {
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

                        usernameTextView.text = "Username: $username"
                        emailTextView.text = "Email: $email"
                        codeTextView.text = id
                        locationText.text = "Lat: $latitude, Lng: $longitude"
                    }
                }

                @SuppressLint("SetTextI18n")
                override fun onCancelled(error: DatabaseError) {
                    usernameTextView.text = "Failed to load user data: ${error.message}"
                }
            })

            val availablePointsTextView: TextView = findViewById(R.id.available_text)
            val totalPointsTextView: TextView = findViewById(R.id.total_points_text)
            val tasksCompletedTextView: TextView = findViewById(R.id.tasks_text)
            val lastActiveDateTextView: TextView = findViewById(R.id.last_active_text)
            val levelTextView: TextView = findViewById(R.id.level_text)
            val currentStreakTextView: TextView = findViewById(R.id.current_streak_text)
            val longestStreakTextView: TextView = findViewById(R.id.longest_streak_text)

            val userProgressRef = database.getReference("user_progress").child(user!!.uid)

            userProgressRef.addListenerForSingleValueEvent(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()){
                        val availablePoints = snapshot.child("availablePoints").value ?: 0
                        val tasksCompleted = snapshot.child("tasksCompleted").value ?: 0
                        val totalPoints = snapshot.child("totalPoints").value ?: 0
                        val lastActiveDate = snapshot.child("lastActiveDate").value ?: "N/A"
                        val level = snapshot.child("level").value ?: 1
                        val currentStreak = snapshot.child("currentStreak").value ?: 0
                        val longestStreak = snapshot.child("longestStreak").value ?: 0

                        availablePointsTextView.text = "✨ You’ve got $availablePoints points ready to shine!"
                        tasksCompletedTextView.text = "✅ Tasks crushed: $tasksCompleted — keep it up!"
                        totalPointsTextView.text = "🏆 Total points earned: $totalPoints — legend status!"
                        lastActiveDateTextView.text = "📅 Last spotted active: $lastActiveDate"
                        levelTextView.text = "🚀 Level $level — reaching new heights!"
                        currentStreakTextView.text = "🔥 On fire! Current streak: $currentStreak days"
                        longestStreakTextView.text = "🌟 Your longest winning streak: $longestStreak days"

                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

            button.setOnClickListener {
                FirebaseAuth.getInstance().signOut();
                goToLogin();
            }

            locationText = findViewById(R.id.location_view)
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

        copyButton.setOnClickListener {
            val codeOnly = codeTextView.text.toString()
            copyToClipboard(codeOnly)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("User Code", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Code copied!", Toast.LENGTH_SHORT).show()
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