package io.whitespots.appsecplugin.protocol

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import io.whitespots.appsecplugin.services.AppSecPluginSettings
import io.whitespots.appsecplugin.services.FindingsRefreshTopics
import io.whitespots.appsecplugin.settings.AppSecPluginSettingsConfigurable
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/**
 * Handles jetbrains:// protocol URLs for plugin configuration.
 * 
 * This handler works with the built-in JetBrains protocol. Users can click URLs like:
 * jetbrains://idea/whitespots-appsec/setup?token=xxx&url=yyy
 * 
 * This works across all JetBrains IDEs (IntelliJ IDEA, WebStorm, PyCharm, etc.)
 */
class WhitespotsApplicationStarter : ApplicationStarter {
    companion object {
        private val LOG = logger<WhitespotsApplicationStarter>()
    }
    private val TOKEN_PATTERN = Pattern.compile("^[a-zA-Z0-9]{32,128}$")

    override fun main(args: List<String>) {
        LOG.info("WhitespotsApplicationStarter.main() called with args: $args")

        if (args.size < 2) {
            LOG.warn("No subcommand provided to whitespots-appsec")
            return
        }

        val command = args[1]
        LOG.info("Processing command: $command")

        when (command) {
            "setup" -> {
                val params = parseSetupArgs(args.drop(2))
                handleSetup(params)
            }
            else -> {
                LOG.warn("Unsupported command: $command")
                showErrorDialog("Unsupported command: $command")
            }
        }
    }

    private fun parseSetupArgs(args: List<String>): Map<String, String> {
        val params = mutableMapOf<String, String>()

        args.forEach { arg ->
            if (arg.contains("=")) {
                val parts = arg.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8)
                    val value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    params[key] = value
                }
            }
        }

        return params
    }

    private fun handleSetup(params: Map<String, String>) {
        val token = params["token"]
        val url = params["url"]

        if (token.isNullOrBlank() || url.isNullOrBlank()) {
            LOG.warn("Setup command missing required parameters. Token: ${token != null}, URL: ${url != null}")
            showWarningDialog("Setup command is missing required parameters (token or url).")
            return
        }

        if (!isValidToken(token)) {
            LOG.warn("Invalid token format received")
            showErrorDialog("Invalid token format. Token must be alphanumeric and 32-128 characters long.")
            return
        }

        LOG.info("Setting up plugin with URL: $url")

        try {
            val settings = AppSecPluginSettings.instance
            val previousUrl = settings.state.apiUrl
            val previousToken = settings.state.apiToken

            settings.state.apiToken = token
            settings.state.apiUrl = url

            ApplicationManager.getApplication().invokeLater {
                showSetupSuccessDialog(url, token, previousUrl, previousToken)
            }
        } catch (e: Exception) {
            LOG.error("Failed to update plugin settings", e)
            showErrorDialog("Failed to update plugin settings: ${e.message}")
        }
    }

    private fun showSetupSuccessDialog(url: String, token: String, previousUrl: String, previousToken: String) {
        val isUpdate = previousUrl.isNotBlank() && previousToken.isNotBlank()
        val action = if (isUpdate) "updated" else "configured"

        val result = Messages.showYesNoDialog(
            "Whitespots AppSec plugin has been $action successfully!\n\n" +
            "API URL: $url\n" +
            "Token: ${token.take(8)}...\n\n" +
            if (isUpdate) "Previous configuration has been replaced.\n\n" else "" +
            "Would you like to open the settings to review the configuration?",
            "Whitespots AppSec Setup Complete",
            "Open Settings",
            "Continue",
            Messages.getInformationIcon()
        )

        if (result == Messages.YES) {
            openPluginSettings()
        }

        refreshFindings()
    }

    private fun openPluginSettings() {
        try {
            val project = ProjectManager.getInstance().openProjects.firstOrNull()
                ?: ProjectManager.getInstance().defaultProject
            ShowSettingsUtil.getInstance().showSettingsDialog(
                project,
                AppSecPluginSettingsConfigurable::class.java
            )
        } catch (e: Exception) {
            LOG.warn("Failed to open plugin settings", e)
        }
    }

    private fun refreshFindings() {
        try {
            ProjectManager.getInstance().openProjects.forEach { project ->
                project.messageBus.syncPublisher(FindingsRefreshTopics.REFRESH_TOPIC)
                    .onRefreshRequested()
            }
        } catch (e: Exception) {
            LOG.warn("Failed to refresh findings", e)
        }
    }

    private fun isValidToken(token: String): Boolean {
        return token.isNotBlank() && TOKEN_PATTERN.matcher(token).matches()
    }

    private fun showErrorDialog(message: String) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(
                message,
                "Whitespots AppSec Setup Error"
            )
        }
    }

    private fun showWarningDialog(message: String) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showWarningDialog(
                message,
                "Whitespots AppSec Setup"
            )
        }
    }
}
