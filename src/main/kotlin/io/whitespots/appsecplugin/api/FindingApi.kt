package io.whitespots.appsecplugin.api

import com.intellij.openapi.diagnostic.Logger
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.whitespots.appsecplugin.models.Finding
import io.whitespots.appsecplugin.models.PaginatedResponse
import io.whitespots.appsecplugin.models.Severity
import io.whitespots.appsecplugin.models.TriageStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class TagRequest(
    val name: String
)

@Serializable
data class SetStatusRequest(
    @SerialName("current_sla_level")
    val currentSlaLevel: Int,
    val comment: String? = null
)

data class FindingsQueryParams(
    val search: String? = null,
    val product: Long?,
    val severityIn: List<Severity>? = null,
    val triageStatusIn: List<TriageStatus>? = null,
    val page: Int? = null,
    val ordering: String? = "-severity",
    @SerialName("assets__in")
    val assetsIn: Map<String, List<String>>? = null
)

object FindingApi {
    private val LOG = Logger.getInstance(FindingApi::class.java)
    private val httpClient = ApiClient.client
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getFindings(params: FindingsQueryParams): PaginatedResponse<Finding> {
        val endpointPath = ApiClient.path("findings")
        LOG.info("Requesting findings from endpoint: $endpointPath with params: $params")
        try {
            val response = httpClient.get(endpointPath) {
                url {
                    params.product?.let { parameters.append("product", it.toString()) }
                    params.ordering?.let { parameters.append("ordering", it) }
                    params.search?.let { parameters.append("search", it) }
                    params.page?.let { parameters.append("page", it.toString()) }
                    params.severityIn?.let { severities ->
                        parameters.append("severity__in", severities.joinToString(",") { s -> s.intValue.toString() })
                    }
                    params.triageStatusIn?.let { statuses ->
                        parameters.append("triage_status__in", statuses.joinToString(",") { s -> s.intValue.toString() })
                    }
                    params.assetsIn?.let { assetsMap ->
                        val jsonString = json.encodeToString(assetsMap)
                        parameters.append("assets__in", jsonString)
                    }
                }
            }

            LOG.info("Received response with status: ${response.status}")
            if (!response.status.isSuccess()) {
                LOG.warn("Error response body: ${response.bodyAsText()}")
            }
            return response.body()
        } catch (e: Exception) {
            LOG.error("Failed to execute request to $endpointPath", e)
            throw e
        }
    }

    suspend fun setStatus(findingId: Long, status: TriageStatus, comment: String? = null): Boolean {
        val endpointPath = ApiClient.path("findings/$findingId")
        LOG.info("Setting status for finding $findingId to $status")

        try {
            val requestBody = SetStatusRequest(
                currentSlaLevel = status.intValue,
                comment = comment
            )

            val response = httpClient.patch(endpointPath) {
                setBody(requestBody)
                contentType(ContentType.Application.Json)
            }

            LOG.info("Set status response: ${response.status}")
            return response.status.isSuccess()
        } catch (e: Exception) {
            LOG.error("Failed to set finding status", e)
            throw e
        }
    }

    suspend fun addTag(findingId: Long, tag: TagRequest): Boolean {
        val endpointPath = ApiClient.path("findings/$findingId/tags/add")
        LOG.info("Adding tag '${tag.name}' to finding $findingId using endpoint: $endpointPath")

        try {
            val response = httpClient.post(endpointPath) {
                setBody(tag)
                contentType(ContentType.Application.Json)
            }

            LOG.info("Add tag response: ${response.status}")
            if (!response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                LOG.warn("Add tag failed - Response body: $responseBody")
            }
            return response.status.isSuccess()
        } catch (e: Exception) {
            LOG.error("Failed to add tag to finding", e)
            throw e
        }
    }
}