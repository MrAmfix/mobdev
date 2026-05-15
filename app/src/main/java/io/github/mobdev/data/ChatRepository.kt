package io.github.mobdev.data

import io.github.mobdev.data.api.ApiFactory
import io.github.mobdev.data.api.ChatApi
import io.github.mobdev.data.model.ImagePayload
import io.github.mobdev.data.model.LoginRequest
import io.github.mobdev.data.model.Message
import io.github.mobdev.data.model.MessageData
import io.github.mobdev.data.model.TextPayload
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ChatRepository(private val store: CredentialStore) {

    private val api: ChatApi = ApiFactory.create(store)

    suspend fun login(name: String, password: String) {
        val token = api.login(LoginRequest(name, password)).trim()
        store.login = name
        store.password = password
        store.token = token
    }

    suspend fun loadChannels(): List<String> = api.channels()

    suspend fun loadLatest(channel: String, limit: Int = 20): List<Message> =
        api.channelMessages(
            name = channel,
            limit = limit,
            lastKnownId = Long.MAX_VALUE,
            reverse = true
        )

    suspend fun loadOlder(channel: String, beforeId: Long, limit: Int = 20): List<Message> =
        api.channelMessages(
            name = channel,
            limit = limit,
            lastKnownId = beforeId,
            reverse = true
        )

    suspend fun sendText(channel: String, text: String): String {
        val name = store.login ?: error("Not logged in")
        val msg = Message(
            from = name,
            to = channel,
            data = MessageData(text = TextPayload(text))
        )
        return api.sendMessage(msg).trim()
    }

    suspend fun sendImage(
        channel: String,
        bytes: ByteArray,
        mimeType: String,
        filename: String
    ): String {
        val name = store.login ?: error("Not logged in")
        val msgJson = ApiFactory.json.encodeToString(
            Message(
                from = name,
                to = channel,
                data = MessageData(image = ImagePayload(link = ""))
            )
        )
        val msgPart = MultipartBody.Part.createFormData(
            name = "msg",
            filename = null,
            body = msgJson.toRequestBody("application/json".toMediaTypeOrNull())
        )
        val picturePart = MultipartBody.Part.createFormData(
            name = "picture",
            filename = filename,
            body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        )
        return api.sendMessageMultipart(msgPart, picturePart).trim()
    }

    fun onUnauthorized() {
        store.clearToken()
    }
}
