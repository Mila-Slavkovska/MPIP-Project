package com.example.mpip

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(applicationContext, MainActivity::class.java);
            startActivity(intent);
            finish();
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val editTextEmail: TextInputEditText = findViewById(R.id.email);
        val editTextPassword: TextInputEditText = findViewById(R.id.password);
        val buttonLogin: Button = findViewById(R.id.btn_login);
        val textView: TextView = findViewById(R.id.register_now);

        textView.setOnClickListener {
            val intent = Intent(applicationContext, RegisterActivity::class.java);
            startActivity(intent);
            finish();
        }

        buttonLogin.setOnClickListener {
            val email = editTextEmail.text.toString();
            val password = editTextPassword.text.toString();

            Log.d("RegisterActivity", "Email: $email, Password: $password");

            if(TextUtils.isEmpty(email)){
                Toast.makeText(this, "Enter an email", Toast.LENGTH_SHORT).show();
                return@setOnClickListener;
            }

            if(TextUtils.isEmpty(password)){
                Toast.makeText(this, "Enter a password", Toast.LENGTH_SHORT).show();
                return@setOnClickListener;
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Toast.makeText(applicationContext, "Login Successful", Toast.LENGTH_SHORT).show();
                        val intent = Intent(applicationContext, MainActivity::class.java);
                        startActivity(intent);
                        finish();
//                        val user = auth.currentUser
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(
                            baseContext,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show();
                    }
                }
        }
    }
}