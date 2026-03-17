package com.pushkar.RGM

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView

class ReelsAdapter(private val reels: List<Post>) : RecyclerView.Adapter<ReelsAdapter.ReelsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReelsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reel, parent, false)
        return ReelsViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReelsViewHolder, position: Int) {
        holder.bind(reels[position])
    }

    override fun getItemCount(): Int = reels.size

    class ReelsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val videoView: VideoView = view.findViewById(R.id.vv_reel)
        private val tvOwner: TextView = view.findViewById(R.id.tv_reel_owner)
        private val tvCaption: TextView = view.findViewById(R.id.tv_reel_caption)

        fun bind(post: Post) {
            tvOwner.text = "@${post.owner}"
            tvCaption.text = post.caption ?: ""
            
            val uri = Uri.parse(post.mediaUri)
            videoView.setVideoURI(uri)
            
            videoView.setOnPreparedListener { mp ->
                mp.isLooping = true
                val videoRatio = mp.videoWidth / mp.videoHeight.toFloat()
                val screenRatio = videoView.width / videoView.height.toFloat()
                val scale = videoRatio / screenRatio
                if (scale >= 1f) {
                    videoView.scaleX = scale
                } else {
                    videoView.scaleY = 1f / scale
                }
                videoView.start()
            }
        }
    }
}
