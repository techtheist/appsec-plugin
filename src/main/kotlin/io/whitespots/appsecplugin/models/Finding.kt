package io.whitespots.appsecplugin.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Finding(
    val id: Long,
    val name: String,
    val description: String?,
    @SerialName("file_path")
    val filePath: String?,
    val line: Int?,
    val severity: Severity,
    @SerialName("current_sla_level")
    val triageStatus: TriageStatus,
    val product: Long,
    @SerialName("date_created")
    val dateCreated: String?,
    @SerialName("dojo_finding_url")
    val findingUrl: String?,
    val tags: List<String> = emptyList()
)

@Serializable
data class PaginatedResponse<T>(
    val count: Int,
    val next: Int?,
    val previous: Int?,
    val results: List<T>
)