package io.whitespots.appsecplugin.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.jcef.JBCefBrowser
import io.whitespots.appsecplugin.models.Finding
import java.awt.Dimension
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import javax.swing.JComponent

object FindingPopupManager {

    fun showFindingPopup(project: Project, finding: Finding, inputEvent: InputEvent?) {
        val popupContent = createPopupContent(project, finding)

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(popupContent, null)
            .setTitle("Security Finding: ${finding.name}")
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .setMinSize(Dimension(400, 300))
            .setDimensionServiceKey(project, "AppSecFindingPopup", false)
            .createPopup()

        if (inputEvent is MouseEvent) {
            popup.show(RelativePoint(inputEvent))
        } else {
            popup.showCenteredInCurrentWindow(project)
        }
    }

    private fun createPopupContent(project: Project, finding: Finding): JComponent {
        val browser = JBCefBrowser()

        ThemeUtils.configureBrowserForExternalLinks(browser)
        ThemeUtils.prepareMarkdownPage(browser, finding, project)

        return browser.component.apply {
            preferredSize = Dimension(600, 400)
        }
    }
}