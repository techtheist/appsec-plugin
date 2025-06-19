package io.whitespots.appsecplugin.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object SeveritySerializer : KSerializer<Severity> {
    override val descriptor = PrimitiveSerialDescriptor("Severity", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Severity) {
        encoder.encodeInt(value.intValue)
    }

    override fun deserialize(decoder: Decoder): Severity {
        val intValue = decoder.decodeInt()
        return Severity.fromInt(intValue)
    }
}

@Serializable(with = SeveritySerializer::class)
enum class Severity(val intValue: Int) {
    INFO(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    companion object {
        fun fromInt(value: Int) = entries.find { it.intValue == value } ?: INFO
    }
}