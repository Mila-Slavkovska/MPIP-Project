package com.example.mpip.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mpip.R
import com.example.mpip.domain.FriendClass

class FriendAdapter(
    private val friends: List<FriendClass>,
    private val onViewLocationClick: (FriendClass) -> Unit
): RecyclerView.Adapter<FriendAdapter.FriendViewHolder>() {

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.friendName)
        val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        val viewOnMapButton: Button = itemView.findViewById(R.id.view_on_map_btn)
        val sendThoughtsButton: Button = itemView.findViewById(R.id.send_thoughts_btn)

        fun bind(friend: FriendClass) {
            nameText.text = friend.name
            viewOnMapButton.setOnClickListener { onViewLocationClick(friend) }
            //TODO:sendThoughtsButton
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun getItemCount() = friends.size

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(friends[position])
    }

}