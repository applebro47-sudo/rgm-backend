package com.pushkar.RGM

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdminUserAdapter(
    private val userList: List<String>
) : RecyclerView.Adapter<AdminUserAdapter.AdminViewHolder>() {

    class AdminViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_admin_user_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_user, parent, false)
        return AdminViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminViewHolder, position: Int) {
        holder.tvName.text = userList[position]
    }

    override fun getItemCount(): Int = userList.size
}
