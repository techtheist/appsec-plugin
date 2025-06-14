package io.whitespots.appsecplugin.models

import kotlinx.serialization.Serializable

enum class AssetType(val value: Int) {
    REPOSITORY(0),
    DOCKER_IMAGE(1),
    DOMAIN(2),
    HOST(3),
    CLOUD(4);
}

@Serializable
data class Asset(
    val id: Long,
    val value: String,
    val product: Long
)