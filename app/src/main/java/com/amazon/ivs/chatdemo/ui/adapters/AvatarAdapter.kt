package com.amazon.ivs.chatdemo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.amazon.ivs.chatdemo.databinding.RowAvatarItemBinding
import com.amazon.ivs.chatdemo.repository.models.Avatar
import kotlin.properties.Delegates

class AvatarAdapter(
    private val onAvatarClicked: (avatar: Avatar) -> Unit
) : RecyclerView.Adapter<AvatarAdapter.ViewHolder>() {

    var avatars: List<Avatar> by Delegates.observable(emptyList()) { _, old, new ->
        DiffUtil.calculateDiff(MembersDiff(old, new)).dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(RowAvatarItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = avatars.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val avatar = avatars[position]
        holder.binding.item = avatar
        holder.binding.avatarItem.tag = avatar
        holder.binding.avatarItem.setOnClickListener { view ->
            onAvatarClicked((view.tag as Avatar))
        }
    }

    inner class ViewHolder(val binding: RowAvatarItemBinding) : RecyclerView.ViewHolder(binding.root)

    inner class MembersDiff(private val oldItems: List<Avatar>,
                            private val newItems: List<Avatar>
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
