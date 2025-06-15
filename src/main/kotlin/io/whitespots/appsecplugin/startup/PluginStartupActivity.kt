package io.whitespots.appsecplugin.startup

import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import io.whitespots.appsecplugin.listeners.EditorListener
import io.whitespots.appsecplugin.services.FindingsRefreshTopics

class PluginStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val connection = project.messageBus.connect()
        connection.subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            EditorListener(project)
        )
        project.messageBus.syncPublisher(FindingsRefreshTopics.REFRESH_TOPIC).onRefreshRequested()
    }
}