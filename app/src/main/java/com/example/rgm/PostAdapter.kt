package com.pushkar.RGM

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase

class PostAdapter(
    private val posts: List<Post>,
    private val currentUser: String,
    private val onPostUpdated: () -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position], currentUser, onPostUpdated)
    }

    override fun getItemCount(): Int = posts.size

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvOwner: TextView = view.findViewById(R.id.tv_post_owner)
        private val ivPostImage: ImageView = view.findViewById(R.id.iv_post_image)
        private val tvCaption: TextView = view.findViewById(R.id.tv_post_caption)
        private val btnLike: ImageButton = view.findViewById(R.id.btn_like)
        private val btnComment: ImageButton = view.findViewById(R.id.btn_comment)
        private val btnShare: ImageButton = view.findViewById(R.id.btn_share)
        private val tvLikesCount: TextView = view.findViewById(R.id.tv_likes_count)
        private val tvViewComments: TextView = view.findViewById(R.id.tv_view_comments)

        fun bind(post: Post, currentUser: String, onPostUpdated: () -> Unit) {
            val context = itemView.context
            tvOwner.text = "@${post.owner}"
            tvCaption.text = post.caption ?: ""
            
            try {
                ivPostImage.setImageURI(Uri.parse(post.mediaUri))
            } catch (e: Exception) {
                ivPostImage.setImageResource(android.R.drawable.ic_menu_report_image)
            }

            val likes = post.likes ?: mutableListOf()
            val comments = post.comments ?: mutableListOf()

            // Like Logic
            val isLiked = likes.contains(currentUser)
            btnLike.setImageResource(if (isLiked) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
            tvLikesCount.text = "${likes.size} likes"

            btnLike.setOnClickListener {
                toggleLikeCloud(post, currentUser)
            }

            // Comment Logic
            tvViewComments.text = if (comments.isEmpty()) "Add a comment..." else "View all ${comments.size} comments"
            
            val showCommentDialog = {
                showCommentDialog(context, post, currentUser)
            }
            
            btnComment.setOnClickListener { showCommentDialog() }
            tvViewComments.setOnClickListener { showCommentDialog() }

            // Share Logic
            btnShare.setOnClickListener {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Check out this post by @${post.owner} on Piee!")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share post via"))
            }
        }

        private fun toggleLikeCloud(post: Post, currentUser: String) {
            val dbRef = FirebaseDatabase.getInstance().getReference("posts").child(post.id)
            val likes = post.likes ?: mutableListOf()
            
            if (likes.contains(currentUser)) {
                likes.remove(currentUser)
            } else {
                likes.add(currentUser)
            }
            
            dbRef.child("likes").setValue(likes)
        }

        private fun showCommentDialog(context: Context, post: Post, currentUser: String) {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_comments, null)
            val rvComments = dialogView.findViewById<RecyclerView>(R.id.rv_comments)
            val etComment = dialogView.findViewById<EditText>(R.id.et_comment_input)
            val btnSend = dialogView.findViewById<ImageButton>(R.id.btn_send_comment)

            val comments = post.comments ?: mutableListOf()
            val adapter = CommentAdapter(comments)
            rvComments.layoutManager = LinearLayoutManager(context)
            rvComments.adapter = adapter

            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .create()

            btnSend.setOnClickListener {
                val text = etComment.text.toString().trim()
                if (text.isNotEmpty()) {
                    val newComment = Comment(user = currentUser, text = text)
                    comments.add(newComment)
                    FirebaseDatabase.getInstance().getReference("posts")
                        .child(post.id).child("comments").setValue(comments)
                        .addOnSuccessListener {
                            etComment.text.clear()
                            adapter.notifyDataSetChanged()
                            rvComments.scrollToPosition(comments.size - 1)
                        }
                }
            }

            dialog.show()
        }
    }
}

class CommentAdapter(private val comments: List<Comment>) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {
    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUser: TextView = view.findViewById(R.id.tv_comment_user)
        val tvText: TextView = view.findViewById(R.id.tv_comment_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.tvUser.text = "@${comment.user}"
        holder.tvText.text = comment.text
    }

    override fun getItemCount() = comments.size
}
