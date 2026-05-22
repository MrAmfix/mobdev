package io.github.mobdev.data.db

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "messages",
    primaryKeys = ["channel", "serverId"],
    indices = [Index("channel")]
)
data class MessageEntity(
    val channel: String,
    val serverId: Long,
    val fromUser: String,
    val toChannel: String?,
    val dataJson: String,
    val time: String?
)
