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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
}