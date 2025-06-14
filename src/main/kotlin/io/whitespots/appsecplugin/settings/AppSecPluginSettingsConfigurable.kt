package io.whitespots.appsecplugin.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import io.whitespots.appsecplugin.services.AppSecPluginSettings

class AppSecPluginSettingsConfigurable : BoundConfigurable("Whitespots AppSec") {

    private val settings = AppSecPluginSettings.instance.state

    override fun createPanel(): DialogPanel = panel {
        group("Backend Connection") {
            row("API URL:") {
                textField().bindText(settings::apiUrl).comment("The base URL of your Whitespots instance.")
            }
            row("API Token:") {
                passwordField().bindText(settings::apiToken).comment("Your personal access token.")
            }
        }

        group("Editor Integration") {
            row {
                checkBox("Highlight findings in the editor").bindSelected(settings::highlightFindings)
            }
        }
    }
}