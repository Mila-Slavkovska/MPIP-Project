package com.example.mpip

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

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
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val editTextEmail: TextInputEditText = findViewById(R.id.email);
        val editTextPassword: TextInputEditText = findViewById(R.id.password);
        val buttonReg: Button = findViewById(R.id.btn_register);
        val progressBar: ProgressBar = findViewById(R.id.progress_bar);
        val textView: TextView = findViewById(R.id.login_now);

        textView.setOnClickListener {
            val intent = Intent(applicationContext, LoginActivity::class.java);
            startActivity(intent);
            finish();
        }

        buttonReg.setOnClickListener {
            progressBar.visibility = View.VISIBLE;
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

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility = View.GONE;
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Toast.makeText(
                            baseContext,
                            "Account created.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(
                            baseContext,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }
    }
}