package me.cubesicle

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.InetSocketAddress

@Serializable
data class Config(
    @Serializable(with = InetSockAddressSerializer::class)
    val bind: InetSocketAddress,
    @SerialName("forwarding-secret")
    val forwardingSecret: String,
    val servers: Map<String, @Serializable(with = InetSockAddressSerializer::class) InetSocketAddress>,
)

object InetSockAddressSerializer : KSerializer<InetSocketAddress> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(javaClass.name, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: InetSocketAddress) =
        encoder.encodeString("${value.address}:${value.port}")

    override fun deserialize(decoder: Decoder): InetSocketAddress {
        val string = decoder.decodeString()
        val parts = string.split(':')
        if (parts.size != 2) error("Invalid address format: $string")

        val host = parts[0]
        val port = parts[1].toInt()
        return InetSocketAddress(host, port)
    }
}