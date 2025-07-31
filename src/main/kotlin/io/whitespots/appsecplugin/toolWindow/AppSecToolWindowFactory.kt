package io.whitespots.appsecplugin.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowContentUiType
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class AppSecToolWindowFactory : ToolWindowFactory {
    private val WEBVIEW_ENABLED = false

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        toolWindow.setDefaultContentUiType(ToolWindowContentUiType.TABBED)

        val appSecToolWindow = AppSecToolWindow(project, toolWindow.disposable)
        val appSecContent = ContentFactory.getInstance().createContent(
            appSecToolWindow.getContent(),
            "Findings",
            false
        )
        toolWindow.contentManager.addContent(appSecContent)
        if (WEBVIEW_ENABLED) {
            val webViewContent = ContentFactory.getInstance().createContent(
                WebViewToolWindow(project).content,
                "Chat",
                false
            )
            toolWindow.contentManager.addContent(webViewContent)
        }
    }
}