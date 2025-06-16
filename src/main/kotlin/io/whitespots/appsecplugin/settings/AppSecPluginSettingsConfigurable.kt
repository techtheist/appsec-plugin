package io.whitespots.appsecplugin.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import io.whitespots.appsecplugin.models.Severity
import io.whitespots.appsecplugin.models.TriageStatus
import io.whitespots.appsecplugin.services.AppSecPluginSettings

class AppSecPluginSettingsConfigurable : BoundConfigurable("Whitespots AppSec") {

    private val settings = AppSecPluginSettings.instance.state

    override fun createPanel(): DialogPanel = panel {
        group("Base") {
            row("External Portal URL:") {
                textField()
                    .bindText(settings::apiUrl)
                    .columns(50)
                    .comment("The base URL of your Whitespots instance")
                    .validationOnInput { field ->
                        val url = field.text.trim()
                        when {
                            url.isEmpty() -> error("URL cannot be empty")
                            !url.startsWith("http://") && !url.startsWith("https://") -> 
                                warning("URL should start with http:// or https://")
                            else -> null
                        }
                    }
            }

            row("Auth API Token:") {
                passwordField()
                    .bindText(settings::apiToken)
                    .columns(50)
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
}