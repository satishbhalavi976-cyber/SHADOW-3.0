package com.project.shadow.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.project.shadow.R
import com.project.shadow.data.ChatMessage
import com.project.shadow.data.MessageRole
import com.project.shadow.databinding.ItemChatShadowBinding
import com.project.shadow.databinding.ItemChatUserBinding

private const val TYPE_USER = 0
private const val TYPE_SHADOW = 1

/**
 * SHADOW 3.0 Chat RecyclerView Adapter
 * Renders user messages (white bubble) and SHADOW responses (dark bubble)
 */
class ChatAdapter(
    private val onCopy: (String) -> Unit,
    private val onSpeak: (String) -> Unit
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    override fun getItemViewType(position: Int) =
        if (getItem(position).role == MessageRole.USER) TYPE_USER else TYPE_SHADOW

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserViewHolder(
                ItemChatUserBinding.inflate(inflater, parent, false)
            )
            else -> ShadowViewHolder(
                ItemChatShadowBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is UserViewHolder -> holder.bind(message)
            is ShadowViewHolder -> holder.bind(message, onCopy, onSpeak)
        }
    }

    // =================== ViewHolders ===================

    class UserViewHolder(private val binding: ItemChatUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.tvMessage.text = message.content
        }
    }

    class ShadowViewHolder(private val binding: ItemChatShadowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage, onCopy: (String) -> Unit, onSpeak: (String) -> Unit) {
            if (message.isLoading) {
                binding.tvMessage.text = "● ● ●"
                binding.btnCopy.visibility = View.GONE
                binding.btnSpeak.visibility = View.GONE
            } else {
                binding.tvMessage.text = message.content
                binding.tvTime.text = message.formattedTime()
                binding.btnCopy.visibility = View.VISIBLE
                binding.btnSpeak.visibility = View.VISIBLE
                binding.btnCopy.setOnClickListener { onCopy(message.content) }
                binding.btnSpeak.setOnClickListener { onSpeak(message.content) }
            }
        }
    }

    private class ChatDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
            oldItem == newItem
    }
}
