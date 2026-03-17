package com.pushkar.RGM

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FriendAdapter(private val friends: List<String>) : RecyclerView.Adapter<FriendAdapter.FriendViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_list, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(friends[position])
    }

    override fun getItemCount(): Int = friends.size

    class FriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivUserImage: ImageView = view.findViewById(R.id.iv_chat_user_image)
        private val tvNickname: TextView = view.findViewById(R.id.tv_chat_user_nickname)
        private val tvUsername: TextView = view.findViewById(R.id.tv_last_message)

        fun bind(username: String) {
            val sharedPref = itemView.context.getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
            val nickname = sharedPref.getString("${username}_nickname", username)
            val imageUriString = sharedPref.getString("${username}_image", null)

            tvNickname.text = nickname
            tvUsername.text = "@$username"
            
            if (imageUriString != null) {
                try {
                    ivUserImage.setImageURI(Uri.parse(imageUriString))
                } catch (e: Exception) {
                    ivUserImage.setImageResource(android.R.drawable.ic_menu_camera)
                }
            } else {
                ivUserImage.setImageResource(android.R.drawable.ic_menu_camera)
            }
        }
    }
}
