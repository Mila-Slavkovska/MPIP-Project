package com.example.mpip

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mpip.adapters.MailboxAdapter
import com.example.mpip.domain.ThoughtMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MailboxActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MailboxAdapter
    private val database = FirebaseDatabase.getInstance("https://mpip-project-ea779-default-rtdb.europe-west1.firebasedatabase.app")

    private val thoughtMessagesLiveData = MutableLiveData<List<ThoughtMessage>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mailbox)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MailboxAdapter { message ->
            showMessagePopup(message)
        }
        recyclerView.adapter = adapter

        thoughtMessagesLiveData.observe(this) { messages ->
            adapter.submitList(messages.reversed())
        }

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = database.getReference("messages/$currentUserId")

        ref.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ThoughtMessage>()
                snapshot.children.forEach {
                    val msg = it.getValue(ThoughtMessage::class.java)
                    val key = it.key

                    if (msg != null) {
                        msg.key = key
                        list.add(msg)
                    }
                }
                adapter.submitList(list.reversed())
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun markMessageAsOpened(userId: String, messageKey: String) {
        val msgRef = database.getReference("messages/$userId/$messageKey/opened")
        msgRef.setValue(true)
    }

    private fun showMessagePopup(message: ThoughtMessage) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        message.key?.let { messageKey ->
            markMessageAsOpened(currentUserId, messageKey)
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Thought from ${message.senderName}")
        builder.setMessage(message.message)

        builder.setPositiveButton("Send Good Thought Back") { dialog, _ ->
            val myApp = application as MyApp
            myApp.sendThoughtToFriend(message.senderId, "Sending you a good thought! 😊")
            Toast.makeText(this, "Thought sent!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        builder.setNegativeButton("Close") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }


}
