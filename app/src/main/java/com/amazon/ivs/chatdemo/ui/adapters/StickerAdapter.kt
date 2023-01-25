package com.amazon.ivs.chatdemo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amazon.ivs.chatdemo.databinding.RowStickerItemBinding
import com.amazon.ivs.chatdemo.repository.models.Sticker

private val stickerDiff = object : DiffUtil.ItemCallback<Sticker>() {
    override fun areItemsTheSame(oldItem: Sticker, newItem: Sticker) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Sticker, newItem: Sticker) = oldItem == newItem
}

class StickerAdapter(
    private val onStickerClicked: (sticker: Sticker) -> Unit
) : ListAdapter<Sticker, StickerAdapter.ViewHolder>(stickerDiff) {
    inner class ViewHolder(val binding: RowStickerItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(RowStickerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sticker = currentList[position]
        holder.binding.sticker = sticker.resource
        holder.binding.stickerItem.setOnClickListener {
            onStickerClicked(sticker)
        }
    }
}
