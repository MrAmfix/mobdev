package io.github.mobdev.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

object FlexibleStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        val input = decoder as? JsonDecoder ?: return decoder.decodeString()
        val element = input.decodeJsonElement()
        if (element is JsonPrimitive) {
            return element.contentOrNull
        }
        return null
    }

    override fun serialize(encoder: Encoder, value: String?) {
        val output = encoder as? JsonEncoder
        if (output != null) {
            output.encodeJsonElement(
                if (value == null) JsonPrimitive(null as String?) else JsonPrimitive(value)
            )
        } else {
            encoder.encodeString(value.orEmpty())
        }
    }
}

@Serializable
data class Message(
    @Serializable(with = FlexibleStringSerializer::class)
    val id: String? = null,
    val from: String,
    val to: String? = null,
    val data: MessageData,
    @Serializable(with = FlexibleStringSerializer::class)
    val time: String? = null
)

@Serializable
data class MessageData(
    @SerialName("Text") val text: TextPayload? = null,
    @SerialName("Image") val image: ImagePayload? = null
)

@Serializable
data class TextPayload(val text: String)

@Serializable
data class ImagePayload(val link: String)
