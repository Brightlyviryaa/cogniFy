// src/main/java/com/example/cognify/adapters/MessageAdapter.kt

package com.example.cognify.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cognify.R
import com.example.cognify.models.Message
import java.util.Locale

class MessageAdapter(private var messages: List<Message>) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    // ViewHolder untuk setiap item pesan
    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val roleTextView: TextView = itemView.findViewById(R.id.roleTextView)
        val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]

        // Menggunakan replaceFirstChar untuk kapitalisasi yang benar
        holder.roleTextView.text = message.role.replaceFirstChar { it.uppercaseChar() }

        holder.contentTextView.text = message.content

        // Set background dan warna teks berdasarkan role
        if (message.role.lowercase(Locale.getDefault()) == "assistant") {
            holder.contentTextView.setBackgroundResource(R.drawable.assistant_message_background)
            holder.contentTextView.setTextColor(holder.itemView.context.getColor(R.color.white))
        } else {
            holder.contentTextView.setBackgroundResource(R.drawable.user_message_background)
            holder.contentTextView.setTextColor(holder.itemView.context.getColor(R.color.black))
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    // Fungsi untuk memperbarui daftar pesan
    fun updateMessages(newMessages: List<Message>) {
        this.messages = newMessages
        notifyDataSetChanged()
    }
}
