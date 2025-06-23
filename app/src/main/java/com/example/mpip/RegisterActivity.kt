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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mpip.domain.UserClass
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            Log.d("RegisterActivity", "firebaseAuthWithGoogle:" + account.id)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Log.w("RegisterActivity", "Google sign in failed", e)
            Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show()
        }
    }

    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
            finish()
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

        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)
        } catch (e: Exception) {
            Log.w("RegisterActivity", "Google Sign-In configuration failed", e)
        }

        val testButton = Button(this)
        testButton.text = "Test Google Client"
        testButton.setOnClickListener {
            Log.d("GoogleSignIn", "Testing Google client...")
            Log.d("GoogleSignIn", "Client configured: ${::googleSignInClient.isInitialized}")

            if (::googleSignInClient.isInitialized) {
                googleSignInClient.silentSignIn()
                    .addOnSuccessListener { account ->
                        Log.d("GoogleSignIn", "Silent sign-in success: ${account.email}")
                    }
                    .addOnFailureListener { e ->
                        Log.d("GoogleSignIn", "Silent sign-in failed (expected): ${e.message}")
                    }
            }
        }

        val editTextUsername: TextInputEditText = findViewById(R.id.username)
        val editTextEmail: TextInputEditText = findViewById(R.id.email)
        val editTextPassword: TextInputEditText = findViewById(R.id.password)
        val buttonReg: Button = findViewById(R.id.btn_register)
        val progressBar: ProgressBar = findViewById(R.id.progress_bar)
        val textView: TextView = findViewById(R.id.login_now)

        val buttonGoogleSignIn: Button? = try {
            findViewById(R.id.btn_google_signin)
        } catch (e: Exception) {
            Log.d("RegisterActivity", "Google Sign-In button not found in layout")
            null
        }

        database =
            FirebaseDatabase.getInstance("https://mpip-project-ea779-default-rtdb.europe-west1.firebasedatabase.app")
        reference = database.getReference("users")

        textView.setOnClickListener {
            val intent = Intent(applicationContext, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        buttonReg.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            val username = editTextUsername.text.toString().trim()
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            Log.d("RegisterActivity", "Email: $email, Password: $password")

            if (TextUtils.isEmpty(username)) {
                Toast.makeText(this, "Enter a username", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Enter an email", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Enter a password", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT)
                    .show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility = View.GONE
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val userId = user?.uid

                        val userData = UserClass(username, email)
                        Log.d("RegisterActivity", "Saving user with UID: $userId")
                        reference.child(userId!!).setValue(userData)

                        Toast.makeText(baseContext, "Account created.", Toast.LENGTH_SHORT).show()

                        val intent = Intent(applicationContext, PetSelectionActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(
                            baseContext,
                            "Authentication failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        buttonGoogleSignIn?.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        try {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Google Sign-In not available", Toast.LENGTH_SHORT).show()
            Log.e("RegisterActivity", "Google Sign-In error", e)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("RegisterActivity", "signInWithCredential:success")
                    val user = auth.currentUser

                    val userData = UserClass(
                        username = user?.displayName ?: "Google User",
                        email = user?.email ?: ""
                    )

                    reference.child(user?.uid!!).setValue(userData)
                        .addOnSuccessListener {
                            Log.d("RegisterActivity", "Google user data created")
                            Toast.makeText(
                                baseContext,
                                "Account created with Google.",
                                Toast.LENGTH_SHORT
                            ).show()

                            val intent =
                                Intent(applicationContext, PetSelectionActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { exception ->
                            Log.e("RegisterActivity", "Failed to create user data", exception)
                            Toast.makeText(
                                this,
                                "Failed to create user profile",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    Log.w("RegisterActivity", "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Google authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}