package me.cubesicle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val bind: String,
    @SerialName("forwarding-secret")
    val forwardingSecret: String,
    val servers: Map<String, String>,
)
