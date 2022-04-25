package com.amazon.ivs.chatdemo.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.amazon.ivs.chatdemo.databinding.RowChatGreenPillBinding
import com.amazon.ivs.chatdemo.databinding.RowChatMessageItemBinding
import com.amazon.ivs.chatdemo.databinding.RowChatRedPillBinding
import com.amazon.ivs.chatdemo.databinding.RowChatStickerItemBinding
import com.amazon.ivs.chatdemo.repository.models.ChatMessageResponse
import com.amazon.ivs.chatdemo.repository.models.MessageViewType
import kotlin.properties.Delegates

class ChatAdapter(
    private val onItemHold: (Int) -> Unit,
    private val onItemTouched: () -> Unit
) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    var messages: List<ChatMessageResponse> by Delegates.observable(emptyList()) { _, old, new ->
        DiffUtil.calculateDiff(MembersDiff(old, new)).dispatchUpdatesTo(this)
    }

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

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int) = messages[position].viewType.index

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
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

    private fun View.setListener(viewHolder: ViewHolder) {
        setOnLongClickListener {
            onItemHold(viewHolder.layoutPosition)
            true
        }
    }

    inner class ViewHolder(val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root)

    inner class MembersDiff(
        private val oldItems: List<ChatMessageResponse>,
        private val newItems: List<ChatMessageResponse>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldItems.size

        override fun getNewListSize() = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition].timeStamp == newItems[newItemPosition].timeStamp
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}
