package io.github.mobdev.data.model

enum class MessageStatus { PENDING, FAILED, SENT }

data class DisplayMessage(
    val id: String,
    val from: String,
    val data: MessageData,
    val time: String?,
    val status: MessageStatus,
    val sortKey: Long,
    val localId: Long = 0
) {
    companion object {
        fun fromServer(msg: Message) = DisplayMessage(
            id = "srv_${msg.id}",
            from = msg.from,
            data = msg.data,
            time = msg.time,
            status = MessageStatus.SENT,
            sortKey = msg.id?.toLongOrNull() ?: 0L
        )
    }
}
