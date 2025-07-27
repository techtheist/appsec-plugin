package io.whitespots.appsecplugin.api

import com.intellij.openapi.diagnostic.logger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AuthTokenResponse(
    val token: String
)

object AuthApi {
    private val LOG = logger<AuthApi>()

    suspend fun login(baseUrl: String, username: String, password: String): String {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
            install(HttpCookies)
        }

        try {
            val cleanBaseUrl = baseUrl.removeSuffix("/")
            val loginUrl = "$cleanBaseUrl/api/v1/login/"

            LOG.info("Attempting login to: $loginUrl")

            val loginResponse = client.submitForm(
                url = loginUrl,
                formParameters = parameters {
                    append("username", username)
                    append("password", password)
                }
            )

            if (!loginResponse.status.isSuccess()) {
                LOG.warn("Login failed with status: ${loginResponse.status}")
                throw Exception("Login failed: ${loginResponse.status.description}")
            }

            LOG.info("Login successful, requesting auth token")

            val tokenUrl = "$cleanBaseUrl/api/v1/profile/auth-token/"
            val tokenResponse = client.get(tokenUrl)

            if (!tokenResponse.status.isSuccess()) {
                LOG.warn("Token request failed with status: ${tokenResponse.status}")
                throw Exception("Failed to get auth token: ${tokenResponse.status.description}")
            }

            val authTokenResponse: AuthTokenResponse = tokenResponse.body()
            LOG.info("Successfully obtained auth token")

            return authTokenResponse.token

        } catch (e: Exception) {
            LOG.error("Authentication failed", e)
            throw e
        } finally {
            client.close()
        }
    }
}
