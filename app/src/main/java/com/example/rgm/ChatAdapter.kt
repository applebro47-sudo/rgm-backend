package com.pushkar.RGM

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(
    private val currentUser: String,
    private val onAction: (Message, ChatAction) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private val messages = mutableListOf<Message>()
    private val selectedMessageIds = mutableSetOf<String>()
    var isSelectionMode = false
        private set

    fun setMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    fun toggleSelection(messageId: String) {
        if (selectedMessageIds.contains(messageId)) {
            selectedMessageIds.remove(messageId)
        } else {
            selectedMessageIds.add(messageId)
        }
        if (selectedMessageIds.isEmpty()) {
            isSelectionMode = false
        }
        notifyDataSetChanged()
    }

    fun startSelection(messageId: String) {
        isSelectionMode = true
        selectedMessageIds.add(messageId)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        isSelectionMode = false
        selectedMessageIds.clear()
        notifyDataSetChanged()
    }

    fun getSelectedMessages(): List<Message> {
        return messages.filter { selectedMessageIds.contains(it.id) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layout = if (viewType == VIEW_TYPE_SENT) {
            R.layout.item_chat_sent
        } else {
            R.layout.item_chat_received
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ChatViewHolder(view, onAction)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message, currentUser, selectedMessageIds.contains(message.id), isSelectionMode)
        
        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(message.id)
                onAction(message, ChatAction.SELECTION_CHANGED)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                startSelection(message.id)
                onAction(message, ChatAction.SELECTION_CHANGED)
                true
            } else {
                false
            }
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].sender == currentUser) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    class ChatViewHolder(view: View, private val onAction: (Message, ChatAction) -> Unit) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tv_message_text)
        private val ivMedia: ImageView = view.findViewById(R.id.iv_media)
        private val tvStatusLabel: TextView? = view.findViewById(R.id.tv_status_label)
        private val tvSavedIndicator: TextView? = view.findViewById(R.id.tv_saved_indicator)
        private val mediaActions: LinearLayout? = view.findViewById(R.id.media_actions)
        private val btnView: Button? = view.findViewById(R.id.btn_view)
        private val btnSave: Button? = view.findViewById(R.id.btn_save)

        fun bind(message: Message, currentUser: String, isSelected: Boolean, isSelectionMode: Boolean) {
            // Background for selection
            if (isSelected) {
                itemView.setBackgroundColor(Color.parseColor("#333797EF")) // Light blue overlay
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT)
            }

            // Text Message
            if (message.text != null) {
                tvMessage.visibility = View.VISIBLE
                var textToShow = message.text
                if (message.isEdited) {
                    textToShow += " (Edited)"
                }
                tvMessage.text = textToShow
            } else {
                tvMessage.visibility = View.GONE
            }

            // Media Handling
            if (message.mediaUri != null) {
                if (message.isOneTime) {
                    ivMedia.visibility = View.GONE
                    tvStatusLabel?.visibility = View.VISIBLE
                    
                    if (message.isViewed) {
                        tvStatusLabel?.text = "Viewed"
                        mediaActions?.visibility = View.GONE
                    } else {
                        tvStatusLabel?.text = "One-time ${message.mediaType?.lowercase()}"
                        if (message.sender != currentUser && !isSelectionMode) {
                            mediaActions?.visibility = View.VISIBLE
                        } else {
                            mediaActions?.visibility = View.GONE
                        }
                    }
                } else {
                    ivMedia.visibility = View.VISIBLE
                    tvStatusLabel?.visibility = View.GONE
                    mediaActions?.visibility = View.GONE
                    try {
                        ivMedia.setImageURI(Uri.parse(message.mediaUri))
                    } catch (e: Exception) {
                        ivMedia.setImageResource(android.R.drawable.ic_menu_report_image)
                    }
                }
            } else {
                ivMedia.visibility = View.GONE
                tvStatusLabel?.visibility = View.GONE
                mediaActions?.visibility = View.GONE
            }

            // Saved Indicator (Sender side)
            if (message.sender == currentUser && message.isSaved) {
                tvSavedIndicator?.visibility = View.VISIBLE
            } else {
                tvSavedIndicator?.visibility = View.GONE
            }

            // Actions (only if not selecting)
            if (!isSelectionMode) {
                btnView?.setOnClickListener { onAction(message, ChatAction.VIEW) }
                btnSave?.setOnClickListener { onAction(message, ChatAction.SAVE) }
                
                // Allow editing on double tap or another way? 
                // Let's stick to selection mode for delete/edit choice.
            } else {
                btnView?.setOnClickListener(null)
                btnSave?.setOnClickListener(null)
            }
        }
    }

    enum class ChatAction {
        VIEW, SAVE, EDIT, DELETE, SELECTION_CHANGED
    }

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
}
