package com.example.mpip

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var auth: FirebaseAuth

    private lateinit var petNameText: TextView
    private lateinit var petTypeText: TextView
    private lateinit var petImage: ImageView
    private lateinit var happinessBar: ProgressBar
    private lateinit var energyBar: ProgressBar
    private lateinit var happinessText: TextView
    private lateinit var energyText: TextView
    private lateinit var levelText: TextView
    private lateinit var feedButton: Button
    private lateinit var playButton: Button
    private lateinit var careButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivityFlow", "=== MainActivity onCreate() started ===")

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Log.e("MainActivityFlow", "No authenticated user found")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        Log.d("MainActivityFlow", "User authenticated: ${currentUser.email}")

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val userSpecificKey = "PetPrefs_${currentUser.uid}"
        sharedPrefs = getSharedPreferences(userSpecificKey, MODE_PRIVATE)
        Log.d("MainActivityFlow", "Using preferences: $userSpecificKey")

        val hasPet = sharedPrefs.getBoolean("pet_selected", false)
        Log.d("MainActivityFlow", "User has pet: $hasPet")

        initializeViews()
        setupClickListeners()

        if (hasPet) {
            Log.d("MainActivityFlow", "Loading existing pet data")
            loadPetData()

            val newPetCreated = intent.getBooleanExtra("new_pet_created", false)
            if (newPetCreated) {
                Log.d("MainActivityFlow", "New pet created, showing welcome message")
                showNewPetWelcome()
            }
        } else {
            Log.d("MainActivityFlow", "No pet found - redirecting to pet selection")
            redirectToPetSelection()
        }

        Log.d("MainActivityFlow", "=== MainActivity setup complete ===")
    }

    private fun initializeViews() {
        Log.d("MainActivityFlow", "Initializing views...")

        try {
            val userProfileBtn: ImageButton = findViewById(R.id.user_profile)

            petNameText = findViewById(R.id.pet_name)
            petTypeText = findViewById(R.id.pet_type)
            petImage = findViewById(R.id.pet_image)
            happinessBar = findViewById(R.id.happiness_bar)
            energyBar = findViewById(R.id.energy_bar)
            happinessText = findViewById(R.id.happiness_text)
            energyText = findViewById(R.id.energy_text)
            levelText = findViewById(R.id.level_text)

            feedButton = findViewById(R.id.feed_button)
            playButton = findViewById(R.id.play_button)
            careButton = findViewById(R.id.care_button)

            Log.d("MainActivityFlow", "All views found successfully")

        } catch (e: Exception) {
            Log.e("MainActivityFlow", "Failed to initialize views", e)
            Toast.makeText(this, "View error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupClickListeners() {
        Log.d("MainActivityFlow", "Setting up click listeners...")

        try {
            val userProfileBtn: ImageButton = findViewById(R.id.user_profile)
            userProfileBtn.setOnClickListener {
                Log.d("MainActivityFlow", "Profile button clicked")
                val intent = Intent(applicationContext, UserProfileActivity::class.java)
                startActivity(intent)
            }

            feedButton.setOnClickListener { feedPet() }
            playButton.setOnClickListener { playWithPet() }
            careButton.setOnClickListener { carePet() }
            petImage.setOnClickListener { petTapped() }

        } catch (e: Exception) {
            Log.e("MainActivityFlow", "Failed to setup click listeners", e)
        }
    }

    private fun redirectToPetSelection() {
        Log.d("MainActivityFlow", "Redirecting to pet selection")
        Toast.makeText(this, "Let's choose your pet companion! 🐾", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, PetSelectionActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showNewPetWelcome() {
        val petName = sharedPrefs.getString("pet_name", "Your pet") ?: "Your pet"
        val petType = sharedPrefs.getString("selected_pet_type", "cat") ?: "cat"

        val welcomeMessage = when (petType) {
            "cat" -> "🐱 $petName is ready to start this journey with you!"
            "turtle" -> "🐢 $petName is excited to be your companion!"
            else -> "🐾 $petName is here to support your self-care journey!"
        }

        val dialog = android.app.AlertDialog.Builder(this)
        dialog.setTitle("Welcome to Your Pet Journey!")
        dialog.setMessage("$welcomeMessage\n\nTake care of your pet by feeding, playing, and showing love. Your pet's happiness reflects your own self-care progress!")
        dialog.setPositiveButton("Let's Start!") { _, _ ->
            Toast.makeText(this, "Tap your pet to interact! 🎉", Toast.LENGTH_LONG).show()
        }
        dialog.setCancelable(true)
        dialog.show()
    }

    private fun loadPetData() {
        Log.d("MainActivityFlow", "Loading pet data...")

        try {
            val petName = sharedPrefs.getString("pet_name", "Unknown") ?: "Unknown"
            val petType = sharedPrefs.getString("selected_pet_type", "cat") ?: "cat"
            val happiness = sharedPrefs.getInt("pet_happiness", 100)
            val energy = sharedPrefs.getInt("pet_energy", 100)
            val level = sharedPrefs.getInt("pet_level", 1)

            Log.d(
                "MainActivityFlow",
                "Loaded - Name: $petName, Type: $petType, Happiness: $happiness, Energy: $energy"
            )

            petNameText.text = petName
            petTypeText.text = when (petType) {
                "cat" -> "Cat"
                "turtle" -> "Turtle"
                else -> "Pet"
            }
            levelText.text = "Level $level"

            try {
                val imageResource = when (petType) {
                    "cat" -> R.drawable.pet_cat
                    "turtle" -> R.drawable.pet_turtle
                    else -> android.R.drawable.sym_def_app_icon
                }
                petImage.setImageResource(imageResource)
                Log.d("MainActivityFlow", "Pet image set for type: $petType")
            } catch (e: Exception) {
                Log.e("MainActivityFlow", "Failed to set pet image, using fallback", e)
                petNameText.text = "$petName ${if (petType == "cat") "🐱" else "🐢"}"
            }

            happinessBar.progress = happiness
            energyBar.progress = energy
            happinessText.text = "$happiness%"
            energyText.text = "$energy%"

            happinessBar.visibility = android.view.View.VISIBLE
            energyBar.visibility = android.view.View.VISIBLE

            val newPetCreated = intent.getBooleanExtra("new_pet_created", false)
            if (!newPetCreated) {
                Toast.makeText(this, "Welcome back, $petName! 🎉", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("MainActivityFlow", "Error loading pet data", e)
            Toast.makeText(this, "Error loading pet: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun feedPet() {
        val currentHappiness = sharedPrefs.getInt("pet_happiness", 100)
        val newHappiness = minOf(100, currentHappiness + 15)

        sharedPrefs.edit().putInt("pet_happiness", newHappiness).apply()

        val petName = sharedPrefs.getString("pet_name", "Your pet") ?: "Your pet"
        Toast.makeText(this, "$petName is happy! 😊 (+15 happiness)", Toast.LENGTH_SHORT).show()

        happinessBar.progress = newHappiness
        happinessText.text = "$newHappiness%"

        animatePet()
    }

    private fun playWithPet() {
        val currentEnergy = sharedPrefs.getInt("pet_energy", 100)
        val currentHappiness = sharedPrefs.getInt("pet_happiness", 100)

        val newEnergy = minOf(100, currentEnergy + 20)
        val newHappiness = minOf(100, currentHappiness + 10)

        with(sharedPrefs.edit()) {
            putInt("pet_energy", newEnergy)
            putInt("pet_happiness", newHappiness)
            apply()
        }

        val petName = sharedPrefs.getString("pet_name", "Your pet") ?: "Your pet"
        Toast.makeText(
            this,
            "$petName had fun playing! 🎮 (+20 energy, +10 happiness)",
            Toast.LENGTH_SHORT
        ).show()

        energyBar.progress = newEnergy
        energyText.text = "$newEnergy%"
        happinessBar.progress = newHappiness
        happinessText.text = "$newHappiness%"

        animatePet()
    }

    private fun carePet() {
        val currentEnergy = sharedPrefs.getInt("pet_energy", 100)
        val currentHappiness = sharedPrefs.getInt("pet_happiness", 100)

        val newEnergy = minOf(100, currentEnergy + 10)
        val newHappiness = minOf(100, currentHappiness + 10)

        with(sharedPrefs.edit()) {
            putInt("pet_energy", newEnergy)
            putInt("pet_happiness", newHappiness)
            apply()
        }

        val petName = sharedPrefs.getString("pet_name", "Your pet") ?: "Your pet"
        Toast.makeText(
            this,
            "$petName feels loved! ❤️ (+10 energy, +10 happiness)",
            Toast.LENGTH_SHORT
        ).show()

        energyBar.progress = newEnergy
        energyText.text = "$newEnergy%"
        happinessBar.progress = newHappiness
        happinessText.text = "$newHappiness%"

        animatePet()
    }

    private fun petTapped() {
        val petName = sharedPrefs.getString("pet_name", "Your pet") ?: "Your pet"
        val petType = sharedPrefs.getString("selected_pet_type", "cat") ?: "cat"

        val message = when (petType) {
            "cat" -> "$petName purrs contentedly! 🐱"
            "turtle" -> "$petName slowly blinks at you! 🐢"
            else -> "$petName is happy to see you!"
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        animatePet()
    }

    private fun animatePet() {
        try {
            petImage.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(150)
                .withEndAction {
                    petImage.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(150)
                        .start()
                }
                .start()
        } catch (e: Exception) {
            Log.e("MainActivityFlow", "Animation failed", e)
        }
    }
}
