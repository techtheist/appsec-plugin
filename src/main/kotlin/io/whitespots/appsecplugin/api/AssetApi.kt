package io.whitespots.appsecplugin.api

import com.intellij.openapi.diagnostic.Logger
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.whitespots.appsecplugin.models.Asset
import io.whitespots.appsecplugin.models.PaginatedResponse

data class AssetQueryParams(
    val search: String?,
    val asset_type: Int?
)

object AssetApi {
    private val LOG = Logger.getInstance(AssetApi::class.java)
    private val httpClient = ApiClient.client

    suspend fun getAssets(params: AssetQueryParams): PaginatedResponse<Asset> {
        val endpointPath = ApiClient.path("product-assets")
        LOG.info("Requesting assets with params: $params")

        return httpClient.get(endpointPath) {
            url {
                params.search?.let { parameters.append("search", it) }
                params.asset_type?.let { parameters.append("asset_type", it.toString()) }
            }
        }.body()
    }
}