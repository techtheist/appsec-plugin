package io.whitespots.appsecplugin.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowContentUiType
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class AppSecToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.setDefaultContentUiType(ToolWindowContentUiType.TABBED)

        val appSecToolWindow = AppSecToolWindow(project, toolWindow.disposable)
        val appSecContent = ContentFactory.getInstance().createContent(
            appSecToolWindow.getContent(),
            "App Sec",
            false)
        val webViewContent = ContentFactory.getInstance().createContent(
            WebViewToolWindow(project).content,
            "Chat",
            false
        )
        toolWindow.contentManager.addContent(appSecContent)
        toolWindow.contentManager.addContent(webViewContent)
    }
}