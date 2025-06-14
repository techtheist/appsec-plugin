package io.whitespots.appsecplugin.api

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.whitespots.appsecplugin.services.AppSecPluginSettings
import kotlinx.serialization.json.Json
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger

class ApiClientConfigurationException(message: String) : Exception(message)

object ApiClient {
    private val LOG = Logger.getInstance(ApiClient::class.java)
    private const val API_BASE_PATH = "/api/v1/"

    val client: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }

            defaultRequest {
                val settings = service<AppSecPluginSettings>().state
                val baseUrl = settings.apiUrl
                val token = settings.apiToken

                LOG.info("Configuring Ktor client with base URL: $baseUrl")
                if (baseUrl.isBlank() || token.isBlank()) {
                    LOG.warn("API URL or Token is not configured in settings.")
                    throw ApiClientConfigurationException("API URL or Token is not configured in settings.")
                }
                LOG.debug("Using token starting with: ${token.take(4)}...")

                url {
                    protocol = if (baseUrl.startsWith("https://")) URLProtocol.HTTPS else URLProtocol.HTTP
                    host = baseUrl.removePrefix("https://").removePrefix("http://").removeSuffix("/")
                }

                header(HttpHeaders.Authorization, "Token $token")
            }
        }
    }

    fun path(endpoint: String): String {
        return API_BASE_PATH + endpoint.trim('/') + "/"
    }
}