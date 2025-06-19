package io.whitespots.appsecplugin.startup

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import io.whitespots.appsecplugin.listeners.EditorListener
import io.whitespots.appsecplugin.services.ProjectFindingsService
import kotlinx.coroutines.delay

class PluginStartupActivity : ProjectActivity {
    companion object {
        private val LOG = logger<PluginStartupActivity>()
    }

    override suspend fun execute(project: Project) {
        LOG.info("Initializing AppSec plugin for project: ${project.name}")

        val connection = project.messageBus.connect()
        connection.subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            EditorListener(project)
        )

        delay(500)

        val findingsService = ProjectFindingsService.getInstance(project)
        if (findingsService.isConfigured()) {
            LOG.info("Plugin is configured, starting automatic findings refresh")
            findingsService.startAutoRefresh()
        } else {
            LOG.info("Plugin not configured, skipping automatic refresh")
        }
    }
}