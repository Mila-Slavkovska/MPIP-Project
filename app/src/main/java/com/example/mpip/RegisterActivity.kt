package com.example.mpip

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mpip.domain.UserClass
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance();
    private lateinit var database: FirebaseDatabase;
    private lateinit var reference: DatabaseReference;

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

        val editTextUsername: TextInputEditText = findViewById(R.id.username);
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
            val username = editTextUsername.text.toString().trim();
            val email = editTextEmail.text.toString().trim();
            val password = editTextPassword.text.toString().trim();

            Log.d("RegisterActivity", "Email: $email, Password: $password");

            if(TextUtils.isEmpty(email)){
                Toast.makeText(this, "Enter an email", Toast.LENGTH_SHORT).show();
                return@setOnClickListener;
            }

            if(TextUtils.isEmpty(password)){
                Toast.makeText(this, "Enter a password", Toast.LENGTH_SHORT).show();
                return@setOnClickListener;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            database = FirebaseDatabase.getInstance("https://mpip-project-ea779-default-rtdb.europe-west1.firebasedatabase.app");
            reference = database.getReference("users");

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility = View.GONE;
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        val user = auth.currentUser;
                        val userId = user?.uid;

                        val userData = UserClass(username, email);
                        Log.d("RegisterActivity", "Saving user with UID: $userId")
                        reference.child(userId!!).setValue(userData)

                        Toast.makeText(
                            baseContext,
                            "Account created.",
                            Toast.LENGTH_SHORT,
                        ).show()

                        val intent = Intent(applicationContext, LoginActivity::class.java);
                        startActivity(intent);
                        finish();
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