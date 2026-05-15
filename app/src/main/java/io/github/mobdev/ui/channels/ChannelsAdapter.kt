package io.github.mobdev.ui.channels

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.mobdev.R
import io.github.mobdev.databinding.ItemChannelBinding

data class ChannelRow(val name: String, val selected: Boolean)

class ChannelsAdapter(
    private val onClick: (String) -> Unit
) : ListAdapter<ChannelRow, ChannelsAdapter.VH>(DIFF) {

    inner class VH(private val b: ItemChannelBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(row: ChannelRow) {
            b.root.isActivated = row.selected
            b.channelName.text = b.root.context.getString(R.string.channel_prefix) + row.name
            b.channelSubtitle.text = row.name
            val letter = row.name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "#"
            b.avatarLetter.text = letter
            b.root.setOnClickListener { onClick(row.name) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        return VH(ItemChannelBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ChannelRow>() {
            override fun areItemsTheSame(oldItem: ChannelRow, newItem: ChannelRow): Boolean =
                oldItem.name == newItem.name
            override fun areContentsTheSame(oldItem: ChannelRow, newItem: ChannelRow): Boolean =
                oldItem == newItem
        }
    }
}
