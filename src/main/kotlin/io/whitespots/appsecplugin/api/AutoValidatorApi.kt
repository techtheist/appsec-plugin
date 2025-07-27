package io.whitespots.appsecplugin.api

import com.intellij.openapi.diagnostic.logger
import io.ktor.client.call.body
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
    val issuesAutoCreateOnVerify: Boolean? = null,
    @SerialName("affected_products_cluster")
    val affectedProductsCluster: String? = null,
    @SerialName("read_only")
    val readOnly: Boolean = false
)

@Serializable
data class AutoValidatorInstructionResponse(
    val id: Long,
    val field: String,
    val value: String,
    val negate: Boolean = false,
    val regex: Boolean = false
)

@Serializable
data class RelatedObjectsMeta(
    @SerialName("affected_products_cluster")
    val affectedProductsCluster: Map<String, Boolean>
)

@Serializable
data class AutoValidatorRule(
    val id: Long,
    @SerialName("is_active")
    val isActive: Boolean,
    @SerialName("action_choices")
    val actionChoices: Int,
    val instructions: List<AutoValidatorInstructionResponse>,
    val tags: List<String>,
    val groups: List<String>,
    @SerialName("related_objects_meta")
    val relatedObjectsMeta: RelatedObjectsMeta? = null,
    @SerialName("affected_products_cluster")
    val affectedProductsCluster: Long? = null,
    @SerialName("affected_products_count")
    val affectedProductsCount: Int = 0,
    @SerialName("issues_auto_create_on_verify")
    val issuesAutoCreateOnVerify: Boolean? = null,
    @SerialName("read_only")
    val readOnly: Boolean
)

@Serializable
data class AutoValidatorRulesListResponse(
    val next: String? = null,
    val previous: String? = null,
    val current: Int,
    val count: Int,
    @SerialName("pages_count")
    val pagesCount: Int,
    val results: List<AutoValidatorRule>
)

data class QueryParamsAutovalidatorRule(
    val actionChoices: Int,
    val search: String
)

object AutoValidatorApi {
    private val LOG = logger<AutoValidatorApi>()
    private val httpClient = ApiClient.client

    suspend fun createRule(rule: AutoValidatorRuleRequest): Result<Long> {
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
                return Result.failure(Exception("Failed to create rule: ${response.status.description}"))
            }

            val createdRule: AutoValidatorRule = response.body()
            LOG.info("Successfully created auto-validator rule with ID: ${createdRule.id}")
            return Result.success(createdRule.id)
        } catch (e: Exception) {
            LOG.error("Failed to create auto-validator rule", e)
            return Result.failure(e)
        }
    }

    suspend fun getRules(queryParams: QueryParamsAutovalidatorRule): Result<AutoValidatorRulesListResponse> {
        val endpointPath = ApiClient.path("auto-validator/rules")

        LOG.info("Getting auto-validator rules with search: ${queryParams.search}")

        try {
            val response = httpClient.get(endpointPath) {
                parameter("action_choices", queryParams.actionChoices)
                parameter("search", queryParams.search)
            }

            LOG.info("Auto-validator rules query response: ${response.status}")

            if (!response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                LOG.warn("Auto-validator rules query failed - Response body: $responseBody")
                return Result.failure(Exception("Failed to get rules: ${response.status.description}"))
            }

            val rulesResponse: AutoValidatorRulesListResponse = response.body()
            LOG.info("Successfully retrieved ${rulesResponse.results.size} auto-validator rules")
            return Result.success(rulesResponse)
        } catch (e: Exception) {
            LOG.error("Failed to get auto-validator rules", e)
            return Result.failure(e)
        }
    }
}
