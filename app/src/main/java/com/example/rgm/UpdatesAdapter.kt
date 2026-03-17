package com.pushkar.RGM

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase

class UpdatesAdapter(
    private val requests: MutableList<String>,
    private val onAcceptClick: (String) -> Unit
) : RecyclerView.Adapter<UpdatesAdapter.UpdatesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdatesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend_request, parent, false)
        return UpdatesViewHolder(view)
    }

    override fun onBindViewHolder(holder: UpdatesViewHolder, position: Int) {
        val requester = requests[position]
        holder.bind(requester, onAcceptClick)
    }

    override fun getItemCount(): Int = requests.size

    class UpdatesViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivRequesterImage: ImageView = view.findViewById(R.id.iv_requester_image)
        private val tvRequesterName: TextView = view.findViewById(R.id.tv_requester_name)
        private val btnAccept: Button = view.findViewById(R.id.btn_accept)

        fun bind(requester: String, onAcceptClick: (String) -> Unit) {
            // First, check local for fast loading
            val sharedPref = itemView.context.getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
            val localNickname = sharedPref.getString("${requester}_nickname", requester)
            val localImage = sharedPref.getString("${requester}_image", null)

            tvRequesterName.text = "$localNickname wants to be your friend"
            updateImage(localImage)

            // Then, fetch from Cloud to ensure it's up to date
            FirebaseDatabase.getInstance().reference.child("user_profiles").child(requester).get()
                .addOnSuccessListener { snapshot ->
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        tvRequesterName.text = "${user.nickname} wants to be your friend"
                        updateImage(user.profileImage)
                    }
                }

            btnAccept.setOnClickListener {
                onAcceptClick(requester)
            }
        }

        private fun updateImage(imageUri: String?) {
            if (imageUri != null && imageUri.isNotEmpty()) {
                try {
                    ivRequesterImage.setImageURI(Uri.parse(imageUri))
                } catch (e: Exception) {
                    ivRequesterImage.setImageResource(android.R.drawable.ic_menu_camera)
                }
            } else {
                ivRequesterImage.setImageResource(android.R.drawable.ic_menu_camera)
            }
        }
    }
}
