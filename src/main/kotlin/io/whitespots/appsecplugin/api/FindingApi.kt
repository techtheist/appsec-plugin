package io.whitespots.appsecplugin.api

import com.intellij.openapi.diagnostic.logger
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
    val product: Long? = null,
    val severityIn: List<Severity>? = null,
    val triageStatusIn: List<TriageStatus>? = null,
    val page: Int? = null,
    val ordering: String? = "-severity",
    @SerialName("assets__in")
    val assetsIn: Map<String, List<String>>? = null
)

object FindingApi {
    private val LOG = logger<FindingApi>()
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
                        val severityValues = severities.joinToString(",") { s -> s.intValue.toString() }
                        parameters.append("severity__in", severityValues)
                    }
                    params.triageStatusIn?.let { statuses ->
                        val statusValues = statuses.joinToString(",") { s -> s.intValue.toString() }
                        parameters.append("triage_status__in", statusValues)
                    }
                    params.assetsIn?.let { assetsMap ->
                        val jsonString = json.encodeToString(assetsMap)
                        parameters.append("assets__in", jsonString)
                    }

                    LOG.debug("Complete request URL parameters: ${this.parameters.entries()}")
                }
            }
            LOG.info("Received response with status: ${response.status}")
            LOG.debug("Response body: ${response.body<PaginatedResponse<Finding>>()}")

            if (!response.status.isSuccess()) {
                LOG.warn("Error response body: ${response.bodyAsText()}")
                throw Exception("API request failed with status ${response.status}")
            }

            val result: PaginatedResponse<Finding> = response.body()
            LOG.debug("Received ${result.results.size} findings on this page. Total count: ${result.count}, Next: ${result.next != null}, Previous: ${result.previous != null}")

            return result
        } catch (e: Exception) {
            LOG.error("Failed to execute request to $endpointPath", e)
            throw e
        }
    }

    suspend fun getAllFindings(params: FindingsQueryParams, maxFindings: Int = Int.MAX_VALUE): List<Finding> {
        val allFindings = mutableListOf<Finding>()
        var currentPage = 1
        var hasMorePages = true

        while (hasMorePages && allFindings.size < maxFindings) {
            val pageParams = params.copy(page = currentPage)

            val response = getFindings(pageParams)
            allFindings.addAll(response.results)

            hasMorePages = response.next != null
            currentPage++

            if (allFindings.size >= maxFindings) {
                LOG.info("Reached maximum findings limit of $maxFindings")
                break
            }
        }

        val finalResults = if (allFindings.size > maxFindings) {
            allFindings.take(maxFindings)
        } else {
            allFindings
        }

        LOG.info("Retrieved ${finalResults.size} total findings across $currentPage pages")
        return finalResults
    }

    suspend fun setStatus(findingId: Long, status: TriageStatus, comment: String? = null): Boolean {
        val endpointPath = ApiClient.path("findings/$findingId")

        try {
            val requestBody = SetStatusRequest(
                currentSlaLevel = status.intValue,
                comment = comment
            )

            val response = httpClient.patch(endpointPath) {
                setBody(requestBody)
                contentType(ContentType.Application.Json)
            }

            LOG.debug("Set status response: ${response.status}")
            return response.status.isSuccess()
        } catch (e: Exception) {
            LOG.error("Failed to set finding status", e)
            throw e
        }
    }

    suspend fun addTag(findingId: Long, tag: TagRequest): Boolean {
        val endpointPath = ApiClient.path("findings/$findingId/tags/add")

        try {
            val response = httpClient.post(endpointPath) {
                setBody(tag)
                contentType(ContentType.Application.Json)
            }

            LOG.debug("Add tag response: ${response.status}")
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