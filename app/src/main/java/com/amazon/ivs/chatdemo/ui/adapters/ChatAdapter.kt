package com.amazon.ivs.chatdemo.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amazon.ivs.chatdemo.databinding.RowChatGreenPillBinding
import com.amazon.ivs.chatdemo.databinding.RowChatMessageItemBinding
import com.amazon.ivs.chatdemo.databinding.RowChatRedPillBinding
import com.amazon.ivs.chatdemo.databinding.RowChatStickerItemBinding
import com.amazon.ivs.chatdemo.repository.networking.models.ChatMessageResponse
import com.amazon.ivs.chatdemo.repository.networking.models.MessageViewType

private val chatDiff = object : DiffUtil.ItemCallback<ChatMessageResponse>() {
    override fun areItemsTheSame(oldItem: ChatMessageResponse, newItem: ChatMessageResponse) = oldItem.timeStamp == newItem.timeStamp
    override fun areContentsTheSame(oldItem: ChatMessageResponse, newItem: ChatMessageResponse) = oldItem == newItem
}

class ChatAdapter(
    private val onItemHold: (Int) -> Unit,
    private val onItemTouched: () -> Unit
) : ListAdapter<ChatMessageResponse, ChatAdapter.ViewHolder>(chatDiff) {
    inner class ViewHolder(val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = when (viewType) {
            MessageViewType.MESSAGE.index -> RowChatMessageItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            MessageViewType.STICKER.index -> RowChatStickerItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            MessageViewType.RED.index -> RowChatRedPillBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            else -> {
                RowChatGreenPillBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            }
        }
        return ViewHolder(binding)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = currentList[position]
        when (message.viewType) {
            MessageViewType.MESSAGE -> {
                (holder.binding as RowChatMessageItemBinding).item = message
                holder.binding.messagePill.setListener(holder)
            }
            MessageViewType.STICKER -> {
                (holder.binding as RowChatStickerItemBinding).item = message
                holder.binding.stickerPill.setListener(holder)
            }
            MessageViewType.RED -> {
                (holder.binding as RowChatRedPillBinding).item = message
            }
            else -> { /* Ignored */ }
        }
        holder.binding.root.setOnTouchListener { _, _ ->
            onItemTouched()
            false
        }
    }

    override fun getItemViewType(position: Int) = currentList[position].viewType.index

    private fun View.setListener(viewHolder: ViewHolder) {
        setOnLongClickListener {
            onItemHold(viewHolder.layoutPosition)
            true
        }
    }
}
