package io.whitespots.appsecplugin.settings

data class AppSecPluginSettingsState(
    var apiUrl: String = "",
    var apiToken: String = "",

    var highlightFindings: Boolean = true,

    var maxFindings: Int = 100,
    var enabledTriageStatuses: MutableSet<String> = mutableSetOf("VERIFIED", "ASSIGNED"),
    var enabledSeverities: MutableSet<String> = mutableSetOf("CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO")
)