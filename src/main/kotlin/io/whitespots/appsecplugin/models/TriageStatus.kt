package io.whitespots.appsecplugin.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object TriageStatusSerializer : KSerializer<TriageStatus> {
    override val descriptor = PrimitiveSerialDescriptor("TriageStatus", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: TriageStatus) {
        encoder.encodeInt(value.intValue)
    }

    override fun deserialize(decoder: Decoder): TriageStatus {
        val intValue = decoder.decodeInt()
        return TriageStatus.fromInt(intValue)
    }
}

@OptIn(ExperimentalStdlibApi::class)
@Serializable(with = TriageStatusSerializer::class)
enum class TriageStatus(val intValue: Int) {
    RESOLVED(0),
    UNVERIFIED(1),
    VERIFIED(2),
    ASSIGNED(3),
    REJECTED(4),
    TEMPORARILY(5),
    PERMANENTLY(6);

    companion object {
        fun fromInt(value: Int) = entries.find { it.intValue == value } ?: UNVERIFIED
    }
}