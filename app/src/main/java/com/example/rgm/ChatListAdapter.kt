package com.pushkar.RGM

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatListAdapter(
    private val chattedUsers: List<String>,
    private val currentUser: String,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_list, parent, false)
        return ChatListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatListViewHolder, position: Int) {
        val otherUser = chattedUsers[position]
        holder.bind(otherUser, currentUser, onItemClick)
    }

    override fun getItemCount(): Int = chattedUsers.size

    class ChatListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivUserImage: ImageView = view.findViewById(R.id.iv_chat_user_image)
        private val tvNickname: TextView = view.findViewById(R.id.tv_chat_user_nickname)
        private val tvLastMsg: TextView = view.findViewById(R.id.tv_last_message)

        fun bind(otherUser: String, currentUser: String, onItemClick: (String) -> Unit) {
            val context = itemView.context
            val appDatabase = AppDatabase.getDatabase(context)
            
            // Generate Chat ID (alphabetical sort to match ChatFragment logic)
            val list = listOf(currentUser, otherUser).sorted()
            val chatId = "${list[0]}_${list[1]}"

            // Use Coroutine to fetch from Room Database
            CoroutineScope(Dispatchers.Main).launch {
                val lastMessage = withContext(Dispatchers.IO) {
                    appDatabase.messageDao().getMessagesByChatId(chatId).lastOrNull()
                }

                val userProfile = withContext(Dispatchers.IO) {
                    appDatabase.userDao().getUserByUsername(otherUser)
                }

                // UI Updates
                tvNickname.text = userProfile?.nickname ?: otherUser
                tvLastMsg.text = lastMessage?.text ?: "No messages"

                val imageUriString = userProfile?.profileImage
                if (!imageUriString.isNullOrEmpty()) {
                    try {
                        ivUserImage.setImageURI(Uri.parse(imageUriString))
                    } catch (e: Exception) {
                        ivUserImage.setImageResource(android.R.drawable.ic_menu_camera)
                    }
                } else {
                    ivUserImage.setImageResource(android.R.drawable.ic_menu_camera)
                }
            }

            itemView.setOnClickListener { onItemClick(otherUser) }
        }
    }
}
