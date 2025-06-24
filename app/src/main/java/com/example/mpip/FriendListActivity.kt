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
import com.example.mpip.adapters.ThoughtsAdapter
import com.example.mpip.domain.FriendClass
import com.example.mpip.domain.ThoughtMessage
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

        adapter = FriendAdapter(emptyList(),
            onViewLocationClick = { friend -> checkFriendLocationAndNavigate(friend.id) },
            onSendThought = { friend -> showThoughtSelectionDialog(friend.id, friend.name) }
        )

        recyclerView.adapter = adapter

        friendLiveData.observe(this) { updatedList ->
            adapter = FriendAdapter(updatedList,
                onViewLocationClick = { friend -> checkFriendLocationAndNavigate(friend.id) },
                onSendThought = { friend -> showThoughtSelectionDialog(friend.id, friend.name) }
            )
            recyclerView.adapter = adapter
        }

        loadFriends()
    }

    private fun checkFriendLocationAndNavigate(friendId: String) {
        val locationRef = database.getReference("users/$friendId/location")

        locationRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitude").getValue(Double::class.java)
                val lon = snapshot.child("longitude").getValue(Double::class.java)

                if (snapshot.exists() && lat != null && lon != null) {
                    val intent = Intent(this@FriendListActivity, FriendLocationActivity::class.java)
                    intent.putExtra("friendId", friendId)
                    startActivity(intent)
                } else {
                    AlertDialog.Builder(this@FriendListActivity)
                        .setTitle("Location Unavailable")
                        .setMessage("This friend hasn't shared their location.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FriendListActivity, "Failed to check location", Toast.LENGTH_SHORT).show()
            }
        })
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
                            if (!existingSnapshot.exists()) {
                                Toast.makeText(this@FriendListActivity, "User with this UID does not exist", Toast.LENGTH_SHORT).show()
                                return
                            }
                            addFriendMutually(currentUserId, friendUid)
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

    private fun addFriendMutually(currentUserId: String, friendUid: String) {
        val currentUserFriendsRef = database.getReference("users/$currentUserId/friends")
        val friendFriendsRef = database.getReference("users/$friendUid/friends")

        currentUserFriendsRef.orderByValue().equalTo(friendUid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(currentSnapshot: DataSnapshot) {
                    if (currentSnapshot.exists()) {
                        Toast.makeText(this@FriendListActivity, "Friend already added", Toast.LENGTH_SHORT).show()
                        return
                    }

                    friendFriendsRef.orderByValue().equalTo(currentUserId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(friendSnapshot: DataSnapshot) {
                                if (friendSnapshot.exists()) {
                                    Toast.makeText(this@FriendListActivity, "Friend already added", Toast.LENGTH_SHORT).show()
                                    return
                                }

                                val updates = hashMapOf<String, Any>(
                                    "/users/$currentUserId/friends/${currentUserFriendsRef.push().key}" to friendUid,
                                    "/users/$friendUid/friends/${friendFriendsRef.push().key}" to currentUserId
                                )

                                database.reference.updateChildren(updates)
                                    .addOnSuccessListener {
                                        Toast.makeText(this@FriendListActivity, "Friend added successfully", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this@FriendListActivity, "Failed to add friend", Toast.LENGTH_SHORT).show()
                                    }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(this@FriendListActivity, "Database error", Toast.LENGTH_SHORT).show()
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@FriendListActivity, "Database error", Toast.LENGTH_SHORT).show()
                }
            })
    }


    private fun showThoughtSelectionDialog(friendId: String, friendName: String) {
        val thoughts = listOf(
            "🌞 Good morning",
            "🌙 Good evening",
            "🤗 Sending you hugs",
            "💪 I've got your back",
            "🌟 I believe in you",
            "💧 Drink some water",
            "💖 Be kind to yourself"
        )

        val dialogView = layoutInflater.inflate(R.layout.dialog_thoughts, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.thoughtsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        lateinit var alertDialog: AlertDialog

        val adapter = ThoughtsAdapter(thoughts) { selectedThought ->
            sendThoughtToFriend(friendId, selectedThought)
            alertDialog.dismiss()
        }
        recyclerView.adapter = adapter

        val builder = AlertDialog.Builder(this)
            .setTitle("Send a Thought to $friendName")
            .setView(dialogView)

        alertDialog = builder.create()
        alertDialog.show()
    }

    private fun sendThoughtToFriend(friendId: String, message: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val refName = database.getReference("users/$currentUserId/username")

        refName.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.getValue(String::class.java) ?: "Someone"

                val msg = ThoughtMessage(
                    senderId = currentUserId,
                    senderName = username,
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    notified = false,
                    opened = false,
                    key = null
                )

                val messageRef = database.getReference("messages").child(friendId).push()
                messageRef.setValue(msg)
                Toast.makeText(this@FriendListActivity, "Thought sent!", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FriendListActivity", "Error sending message")
                Toast.makeText(this@FriendListActivity, "Failed to send thought!", Toast.LENGTH_SHORT).show()
            }
        })
    }

}