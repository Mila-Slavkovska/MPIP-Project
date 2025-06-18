package com.example.mpip

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth;
    private var user: FirebaseUser? = null;

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

                        textView.text = "Username: $username\nEmail: $email"
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
        }
    }
}