package com.pushkar.RGM

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pushkar.RGM.databinding.FragmentChatBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var currentUser: String
    private lateinit var otherUser: String
    private var database: DatabaseReference? = null
    private var chatListener: ValueEventListener? = null
    private lateinit var appDatabase: AppDatabase

    private val pickMediaLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                try {
                    requireActivity().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) { e.printStackTrace() }
                val type = if (requireActivity().contentResolver.getType(uri)?.contains("video") == true) "VIDEO" else "IMAGE"
                sendMediaMessage(uri.toString(), type)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        appDatabase = AppDatabase.getDatabase(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            if (FirebaseApp.getApps(requireContext()).isNotEmpty()) {
                database = FirebaseDatabase.getInstance().reference
            }
        } catch (e: Exception) { e.printStackTrace() }

        val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
        currentUser = sharedPref.getString("CURRENT_USER", "") ?: ""
        otherUser = arguments?.getString("otherUser") ?: ""

        if (currentUser.isEmpty() || otherUser.isEmpty()) {
            Toast.makeText(context, "Error: User data missing", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        // Fetch nickname from local DB and update header
        lifecycleScope.launch {
            if (otherUser == "AI Assistant") {
                binding.tvChatWithName.text = "Smart AI Assistant"
            } else {
                val user = appDatabase.userDao().getUserByUsername(otherUser)
                binding.tvChatWithName.text = user?.nickname ?: otherUser
            }
        }

        chatAdapter = ChatAdapter(currentUser) { message, action ->
            handleChatAction(message, action)
        }
        binding.rvMessages.layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = chatAdapter

        // Load messages from local storage first for instant display
        loadMessagesLocally()

        if (otherUser == "AI Assistant") {
            loadAiMessages()
        } else if (database != null) {
            listenForCloudMessages()
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                binding.etMessage.text.clear()
            }
        }

        binding.btnPickImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickMediaLauncher.launch(intent)
        }
        
        binding.btnPickVideo.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "video/*"
            }
            pickMediaLauncher.launch(intent)
        }

        binding.btnCancelSelection.setOnClickListener {
            chatAdapter.clearSelection()
            updateSelectionUi()
        }

        binding.btnDeleteSelected.setOnClickListener {
            deleteSelectedMessages()
        }
    }

    private fun listenForCloudMessages() {
        val chatId = getChatId(currentUser, otherUser)
        chatListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lifecycleScope.launch {
                    snapshot.children.forEach { child ->
                        val msg = child.getValue(Message::class.java)
                        if (msg != null && (msg.deletedBy == null || !msg.deletedBy.contains(currentUser))) {
                            // Save each cloud message to local database
                            val entity = MessageEntity(
                                id = msg.id,
                                sender = msg.sender,
                                receiver = msg.receiver,
                                text = msg.text,
                                mediaUri = msg.mediaUri,
                                mediaType = msg.mediaType,
                                timestamp = msg.timestamp,
                                chatId = chatId
                            )
                            appDatabase.messageDao().insertMessage(entity)
                        }
                    }
                    // Refresh UI from local database to ensure consistency
                    loadMessagesLocally()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Chat Sync Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        database?.child("chats")?.child(chatId)?.addValueEventListener(chatListener!!)
    }

    private fun getChatId(user1: String, user2: String): String {
        val list = listOf(user1, user2).sorted()
        return "${list[0]}_${list[1]}"
    }

    private fun sendMessage(text: String) {
        val message = Message(sender = currentUser, receiver = otherUser, text = text)
        if (otherUser == "AI Assistant") {
            saveAiMessage(message)
            generateAiResponse(text)
        } else {
            val chatId = getChatId(currentUser, otherUser)
            
            // Save locally first
            lifecycleScope.launch {
                val entity = MessageEntity(
                    id = message.id,
                    sender = message.sender,
                    receiver = message.receiver,
                    text = message.text,
                    mediaUri = message.mediaUri,
                    mediaType = message.mediaType,
                    timestamp = message.timestamp,
                    chatId = chatId
                )
                appDatabase.messageDao().insertMessage(entity)
                loadMessagesLocally()
            }

            // Then sync to cloud
            database?.child("chats")?.child(chatId)?.child(message.id)?.setValue(message)
        }
    }

    private fun sendMediaMessage(uri: String, type: String) {
        val isOneTime = binding.cbOneTime.isChecked
        val message = Message(sender = currentUser, receiver = otherUser, mediaUri = uri, mediaType = type, isOneTime = isOneTime)
        if (otherUser != "AI Assistant") {
            val chatId = getChatId(currentUser, otherUser)
            
            lifecycleScope.launch {
                val entity = MessageEntity(
                    id = message.id,
                    sender = message.sender,
                    receiver = message.receiver,
                    text = message.text,
                    mediaUri = message.mediaUri,
                    mediaType = message.mediaType,
                    timestamp = message.timestamp,
                    chatId = chatId
                )
                appDatabase.messageDao().insertMessage(entity)
                loadMessagesLocally()
            }

            database?.child("chats")?.child(chatId)?.child(message.id)?.setValue(message)
        }
    }

    private fun handleChatAction(message: Message, action: ChatAdapter.ChatAction) {
        when(action) {
            ChatAdapter.ChatAction.DELETE -> {
                lifecycleScope.launch {
                    if (otherUser == "AI Assistant") {
                        deleteAiMessage(message.id)
                    } else {
                        appDatabase.messageDao().deleteMessageById(message.id)
                        loadMessagesLocally()
                    }
                }
            }
            ChatAdapter.ChatAction.SELECTION_CHANGED -> {
                updateSelectionUi()
            }
            else -> {}
        }
    }

    private fun updateSelectionUi() {
        if (chatAdapter.isSelectionMode) {
            binding.selectionHeader.visibility = View.VISIBLE
            binding.tvSelectionCount.text = "${chatAdapter.getSelectedMessages().size} selected"
        } else {
            binding.selectionHeader.visibility = View.GONE
        }
    }

    private fun deleteSelectedMessages() {
        val selected = chatAdapter.getSelectedMessages()
        lifecycleScope.launch {
            selected.forEach { message ->
                if (otherUser == "AI Assistant") {
                    deleteAiMessage(message.id)
                } else {
                    appDatabase.messageDao().deleteMessageById(message.id)
                    val chatId = getChatId(currentUser, otherUser)
                    database?.child("chats")?.child(chatId)?.child(message.id)?.removeValue()
                }
            }
            chatAdapter.clearSelection()
            updateSelectionUi()
            if (otherUser != "AI Assistant") loadMessagesLocally()
        }
    }

    private fun loadMessagesLocally() {
        if (otherUser == "AI Assistant") return
        val chatId = getChatId(currentUser, otherUser)
        lifecycleScope.launch {
            val localMessages = appDatabase.messageDao().getMessagesByChatId(chatId)
            val messages = localMessages.map { entity ->
                Message(
                    id = entity.id,
                    sender = entity.sender,
                    receiver = entity.receiver,
                    text = entity.text,
                    mediaUri = entity.mediaUri,
                    mediaType = entity.mediaType,
                    timestamp = entity.timestamp
                )
            }
            chatAdapter.setMessages(messages)
            if (messages.isNotEmpty()) {
                binding.rvMessages.post {
                    binding.rvMessages.scrollToPosition(messages.size - 1)
                }
            }
        }
    }

    private fun loadAiMessages() {
        val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
        val json = sharedPref.getString("AI_CHATS_$currentUser", "[]")
        val type = object : TypeToken<List<Message>>() {}.type
        val messages: List<Message> = Gson().fromJson(json, type)
        chatAdapter.setMessages(messages)
        if (messages.isNotEmpty()) {
            binding.rvMessages.post {
                binding.rvMessages.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun generateAiResponse(userText: String) {
        val response = when {
            userText.contains("hello", ignoreCase = true) || userText.contains("hi", ignoreCase = true) -> 
                "Hello! I'm your Smart AI Assistant. I'm here to help you explore RGM!"
            userText.contains("help", ignoreCase = true) -> 
                "You can share posts, watch reels, and chat with friends. I can also help you with settings."
            userText.contains("joke", ignoreCase = true) -> 
                "Why did the developer go broke? Because he used up all his cache! 😄"
            userText.contains("who are you", ignoreCase = true) -> 
                "I am the RGM AI Assistant, your personal companion in this social network."
            userText.contains("reels", ignoreCase = true) -> 
                "You can find Reels in the bottom navigation. Enjoy short video content!"
            else -> "I see! That's very interesting. Is there anything specific about RGM you'd like to know?"
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val aiMessage = Message(sender = "AI Assistant", receiver = currentUser, text = response)
            saveAiMessage(aiMessage)
        }, 1200)
    }

    private fun saveAiMessage(message: Message) {
        val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
        val json = sharedPref.getString("AI_CHATS_$currentUser", "[]")
        val type = object : TypeToken<MutableList<Message>>() {}.type
        val messages: MutableList<Message> = Gson().fromJson(json, type)
        messages.add(message)
        sharedPref.edit().putString("AI_CHATS_$currentUser", Gson().toJson(messages)).apply()
        loadAiMessages()
    }

    private fun deleteAiMessage(messageId: String) {
        val sharedPref = requireActivity().getSharedPreferences("PIEE_PREFS", Context.MODE_PRIVATE)
        val json = sharedPref.getString("AI_CHATS_$currentUser", "[]")
        val type = object : TypeToken<MutableList<Message>>() {}.type
        val messages: MutableList<Message> = Gson().fromJson(json, type)
        messages.removeAll { it.id == messageId }
        sharedPref.edit().putString("AI_CHATS_$currentUser", Gson().toJson(messages)).apply()
        loadAiMessages()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chatListener?.let {
            val chatId = getChatId(currentUser, otherUser)
            database?.child("chats")?.child(chatId)?.removeEventListener(it)
        }
        _binding = null
    }
}
