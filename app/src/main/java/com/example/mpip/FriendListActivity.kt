package com.example.mpip

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mpip.adapters.FriendAdapter
import com.example.mpip.domain.FriendClass
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FriendListActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FriendAdapter
    private val friendLiveData = MutableLiveData<List<FriendClass>>()

    private val database = FirebaseDatabase.getInstance("https://mpip-project-ea779-default-rtdb.europe-west1.firebasedatabase.app")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_friend_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val addFriendButton: Button = findViewById(R.id.add_friend_btn)
        addFriendButton.setOnClickListener {
            showAddFriendDialog()
        }

        recyclerView = findViewById(R.id.friend_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = FriendAdapter(emptyList()) { friend ->
            Log.d("FriendListActivity", "$friend was clicked")
            val intent = Intent(this, FriendLocationActivity::class.java)
            intent.putExtra("friendId", friend.id)
            startActivity(intent)
        }

        recyclerView.adapter = adapter

        friendLiveData.observe(this) { updatedList ->
            adapter = FriendAdapter(updatedList) { friend ->
                Log.d("FriendListActivity", "$friend was clicked")
                val intent = Intent(this, FriendLocationActivity::class.java)
                intent.putExtra("friendId", friend.id)
                startActivity(intent)
            }
            recyclerView.adapter = adapter
        }

        loadFriends()
    }

    private fun loadFriends() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = database.getReference("users/$currentUserId/friends")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newFriends = mutableListOf<FriendClass>()
                val friendIds = snapshot.children.mapNotNull { it.getValue(String::class.java) }
                for (id in friendIds) {
                    database.getReference("users/$id/username")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(nameSnapshot: DataSnapshot) {
                                val name = nameSnapshot.getValue(String::class.java) ?: "Unknown"
                                newFriends.add(FriendClass(id, name))
                                friendLiveData.value = newFriends.toList()
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showAddFriendDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_friend, null)
        val uidEditText = dialogView.findViewById<EditText>(R.id.uidEditText)

        AlertDialog.Builder(this)
            .setTitle("Add Friend")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val friendUid = uidEditText.text.toString().trim()
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setPositiveButton
                if (friendUid.isNotEmpty() && friendUid != currentUserId) {
                    val userCheckRef = database.getReference("users/$friendUid")
                    userCheckRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(existingSnapshot: DataSnapshot) {
                            val ref = database.getReference("users/$currentUserId/friends")

                            if (existingSnapshot.exists()) {
                                Toast.makeText(this@FriendListActivity, "Friend already added", Toast.LENGTH_SHORT).show()
                            } else {
                                ref.push().setValue(friendUid)
                                    .addOnSuccessListener {
                                        Toast.makeText(this@FriendListActivity, "Friend added", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this@FriendListActivity, "Failed to add friend", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@FriendListActivity, "Database error", Toast.LENGTH_SHORT).show()
                        }
                    })
                } else {
                    Toast.makeText(this, "Invalid UID", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}