package com.example.mpip

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class PetSelectionActivity : AppCompatActivity() {
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var auth: FirebaseAuth

    private lateinit var catCard: LinearLayout
    private lateinit var turtleCard: LinearLayout
    private lateinit var catSelectedIndicator: TextView
    private lateinit var turtleSelectedIndicator: TextView
    private lateinit var petNameInput: TextInputEditText
    private lateinit var continueButton: Button

    private var selectedPetType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("PetSelectionFlow", "=== PetSelectionActivity started ===")

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Log.e("PetSelectionFlow", "No authenticated user found")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        Log.d("PetSelectionFlow", "User authenticated: ${currentUser.email}")

        enableEdgeToEdge()
        setContentView(R.layout.activity_pet_selection)

        val userSpecificKey = "PetPrefs_${currentUser.uid}"
        sharedPrefs = getSharedPreferences(userSpecificKey, MODE_PRIVATE)
        Log.d("PetSelectionFlow", "Using preferences: $userSpecificKey")

        val hasPet = sharedPrefs.getBoolean("pet_selected", false)
        Log.d("PetSelectionFlow", "User already has pet: $hasPet")

        if (hasPet) {
            Log.d("PetSelectionFlow", "Pet exists, redirecting to MainActivity")
            Toast.makeText(this, "Welcome back to your pet! 🐾", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        initializeUI()

        Log.d("PetSelectionFlow", "=== PetSelectionActivity setup complete ===")
    }

    private fun initializeUI() {
        Log.d("PetSelectionFlow", "Initializing card-based UI")

        catCard = findViewById(R.id.cat_card)
        turtleCard = findViewById(R.id.turtle_card)
        catSelectedIndicator = findViewById(R.id.cat_selected)
        turtleSelectedIndicator = findViewById(R.id.turtle_selected)
        petNameInput = findViewById(R.id.pet_name_input)
        continueButton = findViewById(R.id.continue_button)

        petNameInput.setText("")

        catCard.setOnClickListener {
            selectPet("cat")
        }

        turtleCard.setOnClickListener {
            selectPet("turtle")
        }

        petNameInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                validateInputs()
            }
        })

        continueButton.setOnClickListener {
            createPet()
        }

        validateInputs()
    }

    private fun selectPet(petType: String) {
        Log.d("PetSelectionFlow", "Pet selected: $petType")

        selectedPetType = petType

        when (petType) {
            "cat" -> {
                catSelectedIndicator.visibility = View.VISIBLE
                turtleSelectedIndicator.visibility = View.GONE
                petNameInput.setText("MyCat")
                petNameInput.setSelection(petNameInput.text?.length ?: 0)
            }

            "turtle" -> {
                catSelectedIndicator.visibility = View.GONE
                turtleSelectedIndicator.visibility = View.VISIBLE
                petNameInput.setText("MyTurtle")
                petNameInput.setSelection(petNameInput.text?.length ?: 0)
            }
        }

        validateInputs()
    }

    private fun validateInputs() {
        val nameText = petNameInput.text.toString().trim()
        val isValid = selectedPetType != null && nameText.length >= 2

        continueButton.isEnabled = isValid
    }

    private fun createPet() {
        val petType = selectedPetType ?: return
        val petName = petNameInput.text.toString().trim()

        if (petName.length < 2) {
            Toast.makeText(this, "Name must be at least 2 characters", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("PetSelectionFlow", "Creating pet: $petType named $petName")

        with(sharedPrefs.edit()) {
            putString("selected_pet_type", petType)
            putString("pet_name", petName)
            putBoolean("pet_selected", true)
            putInt("pet_happiness", 100)
            putInt("pet_energy", 100)
            putInt("pet_level", 1)
            putLong("creation_time", System.currentTimeMillis())
            apply()
        }

        Log.d("PetSelectionFlow", "Pet data saved successfully")

        val petDisplayName = when (petType) {
            "cat" -> "Cat"
            "turtle" -> "Turtle"
            else -> "Pet"
        }

        Toast.makeText(
            this,
            "Welcome to your journey with $petName the $petDisplayName! 🎉",
            Toast.LENGTH_LONG
        ).show()

        Log.d("PetSelectionFlow", "Navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("new_pet_created", true) // Flag to show welcome message
        startActivity(intent)
        finish()
    }
}