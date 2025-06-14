package io.whitespots.appsecplugin.settings

data class AppSecPluginSettingsState(
    var apiUrl: String = "https://portal-dev.whitespots.io/",
    var apiToken: String = "",

    var highlightFindings: Boolean = true,

    var maxFindings: Int = 100,
    var triageStatuses: MutableSet<String> = mutableSetOf("Verified", "Assigned"),
    var severityLevels: MutableSet<String> = mutableSetOf("Critical", "High", "Medium", "Low", "Info")
)