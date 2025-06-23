package com.example.mpip

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mpip.domain.ThoughtMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            observeMessages(currentUser.uid, this)
        }
    }

    private fun observeMessages(userId: String, context: Context) {
        val dbRef = FirebaseDatabase.getInstance("https://mpip-project-ea779-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("messages")
            .child(userId)

        dbRef.orderByChild("notified").equalTo(false)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val thought = child.getValue(ThoughtMessage::class.java) ?: continue
                        showLocalNotification(thought, context)
                        child.ref.child("notified").setValue(true)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MyApp", "Failed to fetch old messages: ${error.message}")
                }
            })

        dbRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d("MyApp", "New thought received: ${snapshot.value}")
                val thought = snapshot.getValue(ThoughtMessage::class.java) ?: return

                if (!thought.notified) {
                    showLocalNotification(thought, context)
                    snapshot.ref.child("notified").setValue(true)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showLocalNotification(thought: ThoughtMessage, context: Context) {
        val channelId = "thoughts_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Thoughts", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MailboxActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("You've received a thought!")
            .setContentText(thought.message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun sendThoughtToFriend(friendId: String, message: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val database = FirebaseDatabase.getInstance("https://mpip-project-ea779-default-rtdb.europe-west1.firebasedatabase.app")

        val refName = database.getReference("users/${currentUser.uid}/username")
        refName.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.getValue(String::class.java) ?: "Someone"
                val msg = ThoughtMessage(
                    senderId = currentUser.uid,
                    senderName = username,
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    notified = false,
                    opened = false,
                    key = null
                )
                val messageRef = database.getReference("messages").child(friendId).push()
                messageRef.setValue(msg)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MyApp", "Failed to send message: ${error.message}")
            }
        })
    }
}
