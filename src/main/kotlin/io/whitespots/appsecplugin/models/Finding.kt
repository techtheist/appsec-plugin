package io.whitespots.appsecplugin.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Finding(
    val id: Long,
    val name: String,
    val description: String? = null,
    @SerialName("file_path") val filePath: String? = null,
    val line: Int? = null,
    val severity: Severity,
    @SerialName("current_sla_level") val triageStatus: TriageStatus,
    val product: Long,
    @SerialName("date_created") val dateCreated: String? = null,
    @SerialName("dojo_finding_url") val findingUrl: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("line_text") val lineText: String? = null,
    val language: String? = null
)

@Serializable
data class PaginatedResponse<T>(
    val count: Int,
    val next: Int?,
    val previous: Int?,
    val results: List<T>
)