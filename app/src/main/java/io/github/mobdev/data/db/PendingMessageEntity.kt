package io.github.mobdev.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val channel: String,
    val fromUser: String,
    val type: String,       // "text" or "image"
    val text: String?,
    val imagePath: String?,
    val mimeType: String?,
    val filename: String?,
    val status: String = "pending",   // "pending" or "failed"
    val createdAt: Long = System.currentTimeMillis()
)
