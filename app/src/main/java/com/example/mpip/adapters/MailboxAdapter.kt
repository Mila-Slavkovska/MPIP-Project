package com.example.mpip.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mpip.domain.ThoughtMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.example.mpip.R

class MailboxAdapter : ListAdapter<ThoughtMessage, MailboxAdapter.MailboxViewHolder>(DiffCallback()) {

    inner class MailboxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val senderNameText: TextView = itemView.findViewById(R.id.senderName)
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)

        fun bind(message: ThoughtMessage) {
            senderNameText.text = message.senderName
            messageText.text = message.message

            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val dateString = sdf.format(Date(message.timestamp))
            timestampText.text = dateString
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MailboxViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_thought_message, parent, false)
        return MailboxViewHolder(view)
    }

    override fun onBindViewHolder(holder: MailboxViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<ThoughtMessage>() {
        override fun areItemsTheSame(oldItem: ThoughtMessage, newItem: ThoughtMessage): Boolean {
            return oldItem.timestamp == newItem.timestamp && oldItem.senderId == newItem.senderId
        }

        override fun areContentsTheSame(oldItem: ThoughtMessage, newItem: ThoughtMessage): Boolean {
            return oldItem == newItem
        }
    }
}

