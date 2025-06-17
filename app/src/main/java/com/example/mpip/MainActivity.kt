package com.example.mpip

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

class MainActivity : AppCompatActivity() {
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
        setContentView(R.layout.activity_main)
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
        }

        button.setOnClickListener {
            FirebaseAuth.getInstance().signOut();
            goToLogin();
        }
    }
}