package com.pushkar.RGM

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SuggestionAdapter(
    private var suggestions: List<User>,
    private val onSuggestionClick: (String) -> Unit
) : RecyclerView.Adapter<SuggestionAdapter.SuggestionViewHolder>() {

    class SuggestionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProfile: ImageView = view.findViewById(R.id.iv_suggestion_profile)
        val tvName: TextView = view.findViewById(R.id.tv_suggestion_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_suggestion, parent, false)
        return SuggestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        val user = suggestions[position]
        holder.tvName.text = if (user.nickname?.isNotEmpty() == true) user.nickname else user.username
        
        if (user.profileImage?.isNotEmpty() == true) {
            try {
                holder.ivProfile.setImageURI(Uri.parse(user.profileImage))
            } catch (e: Exception) {
                holder.ivProfile.setImageResource(android.R.drawable.ic_menu_camera)
            }
        } else {
            holder.ivProfile.setImageResource(android.R.drawable.ic_menu_camera)
        }

        holder.itemView.setOnClickListener { onSuggestionClick(user.username) }
    }

    override fun getItemCount(): Int = suggestions.size
}
