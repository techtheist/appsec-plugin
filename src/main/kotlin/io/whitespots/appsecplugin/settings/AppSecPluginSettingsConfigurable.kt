package io.whitespots.appsecplugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.ui.dsl.builder.*
import io.whitespots.appsecplugin.api.AuthApi
import io.whitespots.appsecplugin.models.Severity
import io.whitespots.appsecplugin.models.TriageStatus
import io.whitespots.appsecplugin.services.AppSecPluginSettings
import io.whitespots.appsecplugin.ui.LoginDialog
import kotlinx.coroutines.*
import javax.swing.JButton

class AppSecPluginSettingsConfigurable : BoundConfigurable("Whitespots AppSec") {

    companion object {
        private val LOG = logger<AppSecPluginSettingsConfigurable>()
    }

    private val settings = AppSecPluginSettings.instance.state
    private var apiTokenField: Cell<*>? = null
    private var loginButton: JButton? = null

    override fun createPanel(): DialogPanel = panel {
        group("Base") {
            row("External Portal URL:") {
                textField()
                    .bindText(settings::apiUrl)
                    .columns(42)
                    .comment("The base URL of your Whitespots instance")
                    .validationOnInput { field ->
                        val url = field.text.trim()
                        loginButton?.isEnabled = url.isNotEmpty() &&
                            (url.startsWith("http://") || url.startsWith("https://"))
                        when {
                            url.isEmpty() -> error("URL cannot be empty")
                            !url.startsWith("http://") && !url.startsWith("https://") ->
                                warning("URL should start with http:// or https://")
                            else -> null
                        }
                    }
            }

            row("Auth API Token:") {
                apiTokenField = passwordField()
                    .bindText(settings::apiToken)
                    .columns(42)
                    .comment("Your personal access token for authentication")
                    .validationOnInput { field ->
                        val token = field.password.concatToString().trim()
                        when {
                            token.isEmpty() -> error("Token cannot be empty")
                            token.length < 32 -> warning("Token seems too short")
                            else -> null
                        }
                    }
            }
            row {
                loginButton = button("Login") {
                    performLogin()
                }.apply {
                    val isEnabled = settings.apiUrl.isNotEmpty() &&
                        (settings.apiUrl.startsWith("http://") || settings.apiUrl.startsWith("https://"))
                    enabled(isEnabled)
                    comment("Login with username/password to obtain token automatically")
                }.component
            }
        }

        group("Personalization") {
            row {
                checkBox("Enable vulnerability highlighting")
                    .bindSelected(settings::highlightFindings)
                    .comment("Highlight security findings in the code editor")
            }
        }

        group("Filter") {
            row("Maximum number of findings to display in the list:") {
                intTextField(IntRange(1, 10000))
                    .bindIntText(settings::maxFindings)
                    .columns(10)
                    .comment("Limit the number of findings")
            }

            separator()

            row {
                label("List of triage statuses to show findings:")
            }
            indent {
                row {
                    comment("Select which triage statuses should be included when displaying findings")
                }
                for (status in TriageStatus.entries) {
                    row {
                        val statusName = status.name
                        val displayName = when (status) {
                            TriageStatus.RESOLVED -> "Resolved"
                            TriageStatus.UNVERIFIED -> "Unverified"
                            TriageStatus.VERIFIED -> "Verified"
                            TriageStatus.ASSIGNED -> "Assigned"
                            TriageStatus.REJECTED -> "Rejected"
                            TriageStatus.TEMPORARILY -> "Temporarily Risk Accepted"
                            TriageStatus.PERMANENTLY -> "Permanently Risk Accepted"
                        }
                        checkBox(displayName)
                            .bindSelected(
                                { settings.enabledTriageStatuses.contains(statusName) },
                                { enabled ->
                                    if (enabled) {
                                        settings.enabledTriageStatuses.add(statusName)
                                    } else {
                                        settings.enabledTriageStatuses.remove(statusName)
                                    }
                                }
                            )
                    }
                }
            }

            separator()

            row {
                label("List of severities to show findings:")
            }
            indent {
                row {
                    comment("Select which severity levels should be included when displaying findings")
                }
                for (severity in Severity.entries.reversed()) { // Show from Critical to Info
                    row {
                        val severityName = severity.name
                        val displayName = when (severity) {
                            Severity.CRITICAL -> "Critical"
                            Severity.HIGH -> "High"
                            Severity.MEDIUM -> "Medium"
                            Severity.LOW -> "Low"
                            Severity.INFO -> "Info"
                        }
                        checkBox(displayName)
                            .bindSelected(
                                { settings.enabledSeverities.contains(severityName) },
                                { enabled ->
                                    if (enabled) {
                                        settings.enabledSeverities.add(severityName)
                                    } else {
                                        settings.enabledSeverities.remove(severityName)
                                    }
                                }
                            )
                    }
                }
            }
        }
    }

    private fun performLogin() {
        val currentApiUrl = settings.apiUrl.trim()
        if (currentApiUrl.isEmpty()) {
            Messages.showErrorDialog(
                "Please configure the Portal URL first",
                "Login Error"
            )
            return
        }

        val loginDialog = LoginDialog(null)
        if (!loginDialog.showAndGet()) {
            return
        }

        val username = loginDialog.username
        val password = loginDialog.password

        ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Logging in...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Authenticating with portal..."
                    indicator.isIndeterminate = true

                    runBlocking {
                        val token = AuthApi.login(currentApiUrl, username, password)

                        ApplicationManager.getApplication().invokeLater {
                            settings.apiToken = token
                            apiTokenField?.let { field ->
                                settings.apiToken = token
                            }
                            Messages.showInfoMessage(
                                "Successfully logged in and obtained authentication token!",
                                "Login Successful"
                            )
                        }
                    }
                } catch (e: Exception) {
                    LOG.error("Login failed", e)
                    ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            "Login failed: ${e.message}",
                            "Login Error"
                        )
                    }
                }
            }
        })
    }
}