// src/main/java/com/example/cognify/adapters/ChatSessionAdapter.kt

package com.example.cognify.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cognify.R
import com.example.cognify.models.ChatSession

class ChatSessionAdapter(
    private val chatSessions: List<ChatSession>,
    private val onItemClick: (ChatSession) -> Unit
) : RecyclerView.Adapter<ChatSessionAdapter.ChatSessionViewHolder>() {

    class ChatSessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chatSessionName: TextView = itemView.findViewById(R.id.chat_session_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatSessionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_session, parent, false)
        return ChatSessionViewHolder(view)
    }

    override fun getItemCount(): Int = chatSessions.size

    override fun onBindViewHolder(holder: ChatSessionViewHolder, position: Int) {
        val chatSession = chatSessions[position]
        holder.chatSessionName.text = "Chat ${position + 1}"
        holder.itemView.setOnClickListener {
            onItemClick(chatSession)
        }
    }
}
