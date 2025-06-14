package io.whitespots.appsecplugin.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val username: String,
    val email: String,
    @SerialName("is_staff")
    val isStaff: Boolean
)