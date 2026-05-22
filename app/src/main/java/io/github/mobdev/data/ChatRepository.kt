package io.github.mobdev.data

import android.content.Context
import io.github.mobdev.data.api.ApiFactory
import io.github.mobdev.data.api.ChatApi
import io.github.mobdev.data.db.AppDatabase
import io.github.mobdev.data.db.MessageEntity
import io.github.mobdev.data.db.PendingMessageEntity
import io.github.mobdev.data.model.DisplayMessage
import io.github.mobdev.data.model.ImagePayload
import io.github.mobdev.data.model.LoginRequest
import io.github.mobdev.data.model.Message
import io.github.mobdev.data.model.MessageData
import io.github.mobdev.data.model.MessageStatus
import io.github.mobdev.data.model.TextPayload
import io.github.mobdev.util.NetworkMonitor
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ChatRepository(
    private val store: CredentialStore,
    private val db: AppDatabase,
    val networkMonitor: NetworkMonitor,
    private val context: Context
) {
    private val api: ChatApi = ApiFactory.create(store)
    private val pendingDir get() = File(context.filesDir, "pending_images").also { it.mkdirs() }

    suspend fun login(name: String, password: String) {
        val token = api.login(LoginRequest(name, password)).trim()
        store.login = name
        store.password = password
        store.token = token
    }

    suspend fun loadChannels(): List<String> {
        if (networkMonitor.isOnline.value) {
            try {
                val channels = api.channels()
                store.saveChannels(channels)
                return channels
            } catch (_: Exception) { }
        }
        return store.loadCachedChannels()
    }

    suspend fun loadLatest(channel: String, limit: Int = 20): List<DisplayMessage> {
        if (networkMonitor.isOnline.value) {
            try {
                val fresh = api.channelMessages(channel, limit, Long.MAX_VALUE, true)
                db.messageDao().insertAll(fresh.map { it.toEntity(channel) })
                return fresh.map { DisplayMessage.fromServer(it) }
            } catch (_: Exception) { }
        }
        return db.messageDao().getLatest(channel).map { it.toDisplay() }
    }

    suspend fun loadOlder(channel: String, beforeId: Long, limit: Int = 20): List<DisplayMessage> {
        if (networkMonitor.isOnline.value) {
            try {
                val older = api.channelMessages(channel, limit, beforeId, true)
                db.messageDao().insertAll(older.map { it.toEntity(channel) })
                return older.map { DisplayMessage.fromServer(it) }
            } catch (_: Exception) { }
        }
        return db.messageDao().getOlderThan(channel, beforeId, limit).map { it.toDisplay() }
    }

    suspend fun loadNewer(channel: String, afterId: Long, limit: Int = 50): List<DisplayMessage> {
        if (!networkMonitor.isOnline.value) return emptyList()
        val newer = api.channelMessages(channel, limit, afterId, false)
        if (newer.isNotEmpty()) db.messageDao().insertAll(newer.map { it.toEntity(channel) })
        return newer.map { DisplayMessage.fromServer(it) }
    }

    suspend fun queueOrSendText(channel: String, text: String): SendResult {
        val name = store.login ?: error("Not logged in")
        val localId = db.pendingMessageDao().insert(
            PendingMessageEntity(channel = channel, fromUser = name, type = "text", text = text,
                imagePath = null, mimeType = null, filename = null, status = "pending")
        )
        return if (networkMonitor.isOnline.value) trySendText(localId, channel, name, text)
               else SendResult.Queued(localId)
    }

    suspend fun queueOrSendImage(
        channel: String,
        bytes: ByteArray,
        mimeType: String,
        filename: String
    ): SendResult {
        val name = store.login ?: error("Not logged in")
        val file = File(pendingDir, "img_${System.currentTimeMillis()}.jpg")
        file.writeBytes(bytes)
        val localId = db.pendingMessageDao().insert(
            PendingMessageEntity(channel = channel, fromUser = name, type = "image", text = null,
                imagePath = file.absolutePath, mimeType = mimeType, filename = filename, status = "pending")
        )
        return if (networkMonitor.isOnline.value) trySendImage(localId, channel, name, file, mimeType, filename)
               else SendResult.Queued(localId)
    }

    suspend fun getPendingMessages(channel: String): List<DisplayMessage> =
        db.pendingMessageDao().getForChannel(channel).map { it.toDisplay() }

    suspend fun flushPending(channel: String) {
        val pending = db.pendingMessageDao().getForChannel(channel)
        for (p in pending) {
            when (p.type) {
                "text" -> trySendText(p.localId, p.channel, p.fromUser, p.text ?: "")
                "image" -> {
                    val file = p.imagePath?.let { File(it) }
                    if (file != null && file.exists())
                        trySendImage(p.localId, p.channel, p.fromUser, file, p.mimeType ?: "image/jpeg", p.filename ?: "image.jpg")
                    else
                        db.pendingMessageDao().delete(p.localId)
                }
            }
        }
    }

    // Used by ChannelsViewModel for channel creation (direct network, no queuing)
    suspend fun sendText(channel: String, text: String): String {
        val name = store.login ?: error("Not logged in")
        val msg = Message(from = name, to = channel, data = MessageData(text = TextPayload(text)))
        return api.sendMessage(msg).trim()
    }

    fun onUnauthorized() = store.clearToken()

    private suspend fun trySendText(localId: Long, channel: String, name: String, text: String): SendResult {
        return try {
            val msg = Message(from = name, to = channel, data = MessageData(text = TextPayload(text)))
            api.sendMessage(msg)
            db.pendingMessageDao().delete(localId)
            SendResult.Sent(localId)
        } catch (_: Exception) {
            db.pendingMessageDao().updateStatus(localId, "failed")
            SendResult.Failed(localId)
        }
    }

    private suspend fun trySendImage(
        localId: Long,
        channel: String,
        name: String,
        file: File,
        mimeType: String,
        filename: String
    ): SendResult {
        return try {
            val bytes = file.readBytes()
            val msgJson = ApiFactory.json.encodeToString(
                Message(from = name, to = channel, data = MessageData(image = ImagePayload(link = "")))
            )
            val msgPart = MultipartBody.Part.createFormData(
                "msg", null, msgJson.toRequestBody("application/json".toMediaTypeOrNull())
            )
            val picturePart = MultipartBody.Part.createFormData(
                "picture", filename, bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            )
            api.sendMessageMultipart(msgPart, picturePart)
            db.pendingMessageDao().delete(localId)
            file.delete()
            SendResult.Sent(localId)
        } catch (_: Exception) {
            db.pendingMessageDao().updateStatus(localId, "failed")
            SendResult.Failed(localId)
        }
    }

    sealed interface SendResult {
        data class Queued(val localId: Long) : SendResult
        data class Sent(val localId: Long) : SendResult
        data class Failed(val localId: Long) : SendResult
    }
}

private fun Message.toEntity(channel: String) = MessageEntity(
    channel = channel,
    serverId = id?.toLongOrNull() ?: 0L,
    fromUser = from,
    toChannel = to,
    dataJson = ApiFactory.json.encodeToString(data),
    time = time
)

private fun MessageEntity.toDisplay(): DisplayMessage {
    val data = try { ApiFactory.json.decodeFromString<MessageData>(dataJson) }
               catch (_: Exception) { MessageData() }
    return DisplayMessage(
        id = "srv_$serverId",
        from = fromUser,
        data = data,
        time = time,
        status = MessageStatus.SENT,
        sortKey = serverId
    )
}

private fun PendingMessageEntity.toDisplay(): DisplayMessage {
    val data = if (type == "text") MessageData(text = TextPayload(text ?: ""))
               else MessageData(image = ImagePayload(link = "file://${imagePath.orEmpty()}"))
    return DisplayMessage(
        id = "lcl_$localId",
        from = fromUser,
        data = data,
        time = null,
        status = if (status == "failed") MessageStatus.FAILED else MessageStatus.PENDING,
        sortKey = Long.MAX_VALUE / 2 + localId,
        localId = localId
    )
}
