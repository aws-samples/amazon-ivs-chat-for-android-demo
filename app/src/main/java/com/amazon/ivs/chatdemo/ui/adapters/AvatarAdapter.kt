package com.amazon.ivs.chatdemo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.amazon.ivs.chatdemo.databinding.RowAvatarItemBinding
import com.amazon.ivs.chatdemo.repository.models.Avatar

private val avatarDiff = object : DiffUtil.ItemCallback<Avatar>() {
    override fun areItemsTheSame(oldItem: Avatar, newItem: Avatar) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Avatar, newItem: Avatar) = oldItem == newItem
}

class AvatarAdapter(
    private val onAvatarClicked: (avatar: Avatar) -> Unit
) : ListAdapter<Avatar, AvatarAdapter.ViewHolder>(avatarDiff) {
    inner class ViewHolder(val binding: RowAvatarItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        RowAvatarItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val avatar = currentList[position]
        holder.binding.item = avatar
        holder.binding.avatarItem.setOnClickListener {
            onAvatarClicked(avatar)
        }
    }
}
