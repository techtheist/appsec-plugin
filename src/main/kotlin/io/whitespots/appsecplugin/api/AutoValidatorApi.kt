package io.whitespots.appsecplugin.api

import com.intellij.openapi.diagnostic.logger
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AutoValidatorInstruction(
    val field: String,
    val value: String,
    val negate: Boolean = false,
    val regex: Boolean = false
)

@Serializable
data class AutoValidatorRuleRequest(
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("action_choices")
    val actionChoices: Int = 0,
    val instructions: List<AutoValidatorInstruction>,
    val tags: List<String>,
    val groups: List<String> = emptyList(),
    @SerialName("allow_all_products")
    val allowAllProducts: Boolean = true,
    @SerialName("issues_auto_create_on_verify")
    val issuesAutoCreateOnVerify: Boolean = false,
    @SerialName("affected_products_cluster")
    val affectedProductsCluster: String? = null,
    @SerialName("read_only")
    val readOnly: Boolean = false
)

object AutoValidatorApi {
    private val LOG = logger<AutoValidatorApi>()
    private val httpClient = ApiClient.client

    suspend fun createRule(rule: AutoValidatorRuleRequest): Boolean {
        val endpointPath = ApiClient.path("auto-validator/rules")

        LOG.info("Creating auto-validator rule with ${rule.instructions.size} instructions")

        try {
            val response = httpClient.post(endpointPath) {
                setBody(rule)
                contentType(ContentType.Application.Json)
            }

            LOG.info("Auto-validator rule creation response: ${response.status}")

            if (!response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                LOG.warn("Auto-validator rule creation failed - Response body: $responseBody")
            }

            return response.status.isSuccess()
        } catch (e: Exception) {
            LOG.error("Failed to create auto-validator rule", e)
            throw e
        }
    }
}
