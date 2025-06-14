package io.whitespots.appsecplugin.startup

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import io.whitespots.appsecplugin.services.FindingsRefreshTopics

class PluginStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.messageBus.syncPublisher(FindingsRefreshTopics.REFRESH_TOPIC).onRefreshRequested()
    }
}