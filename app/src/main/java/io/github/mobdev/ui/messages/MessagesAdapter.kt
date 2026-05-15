package io.github.mobdev.ui.messages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import io.github.mobdev.R
import io.github.mobdev.data.model.Message
import io.github.mobdev.databinding.ItemMessageImageInBinding
import io.github.mobdev.databinding.ItemMessageImageOutBinding
import io.github.mobdev.databinding.ItemMessageInBinding
import io.github.mobdev.databinding.ItemMessageOutBinding
import io.github.mobdev.util.Urls

class MessagesAdapter(
    private val currentUser: String,
    private val onImageClick: (String) -> Unit
) : ListAdapter<Message, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int {
        val msg = getItem(position)
        val mine = msg.from == currentUser
        val isImage = msg.data.image != null
        return when {
            mine && isImage -> TYPE_OUT_IMAGE
            mine -> TYPE_OUT_TEXT
            isImage -> TYPE_IN_IMAGE
            else -> TYPE_IN_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_IN_TEXT -> InTextVH(ItemMessageInBinding.inflate(inflater, parent, false))
            TYPE_OUT_TEXT -> OutTextVH(ItemMessageOutBinding.inflate(inflater, parent, false))
            TYPE_IN_IMAGE -> InImageVH(ItemMessageImageInBinding.inflate(inflater, parent, false), onImageClick)
            TYPE_OUT_IMAGE -> OutImageVH(ItemMessageImageOutBinding.inflate(inflater, parent, false), onImageClick)
            else -> error("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is InTextVH -> holder.bind(msg)
            is OutTextVH -> holder.bind(msg)
            is InImageVH -> holder.bind(msg)
            is OutImageVH -> holder.bind(msg)
        }
    }

    class InTextVH(private val b: ItemMessageInBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: Message) {
            b.sender.text = msg.from
            b.messageText.text = msg.data.text?.text.orEmpty()
        }
    }

    class OutTextVH(private val b: ItemMessageOutBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: Message) {
            b.messageText.text = msg.data.text?.text.orEmpty()
        }
    }

    class InImageVH(
        private val b: ItemMessageImageInBinding,
        private val onClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: Message) {
            b.sender.text = msg.from
            val link = msg.data.image?.link ?: return
            b.messageImage.load(Urls.thumb(link)) {
                crossfade(true)
                placeholder(R.drawable.ic_image_placeholder)
                error(R.drawable.ic_image_placeholder)
            }
            b.messageImage.setOnClickListener { onClick(link) }
        }
    }

    class OutImageVH(
        private val b: ItemMessageImageOutBinding,
        private val onClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: Message) {
            val link = msg.data.image?.link ?: return
            b.messageImage.load(Urls.thumb(link)) {
                crossfade(true)
                placeholder(R.drawable.ic_image_placeholder)
                error(R.drawable.ic_image_placeholder)
            }
            b.messageImage.setOnClickListener { onClick(link) }
        }
    }

    companion object {
        private const val TYPE_IN_TEXT = 0
        private const val TYPE_OUT_TEXT = 1
        private const val TYPE_IN_IMAGE = 2
        private const val TYPE_OUT_IMAGE = 3

        val DIFF = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean =
                oldItem == newItem
        }
    }
}
