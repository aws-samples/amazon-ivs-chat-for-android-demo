package com.amazon.ivs.chatdemo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.amazon.ivs.chatdemo.databinding.RowStickerItemBinding
import com.amazon.ivs.chatdemo.repository.models.Sticker
import kotlin.properties.Delegates

class StickerAdapter(
    private val onStickerClicked: (sticker: Sticker) -> Unit
) : RecyclerView.Adapter<StickerAdapter.ViewHolder>() {

    var stickers: List<Sticker> by Delegates.observable(emptyList()) { _, old, new ->
        DiffUtil.calculateDiff(MembersDiff(old, new)).dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(RowStickerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = stickers.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sticker = stickers[position]
        holder.binding.sticker = sticker.resource
        holder.binding.stickerItem.setOnClickListener {
            onStickerClicked(sticker)
        }
    }

    inner class ViewHolder(val binding: RowStickerItemBinding) : RecyclerView.ViewHolder(binding.root)

    inner class MembersDiff(private val oldItems: List<Sticker>,
                            private val newItems: List<Sticker>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldItems.size

        override fun getNewListSize() = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition].id == newItems[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}
