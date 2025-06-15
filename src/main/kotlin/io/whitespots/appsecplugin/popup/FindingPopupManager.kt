package io.whitespots.appsecplugin.popup

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.jcef.JBCefBrowser
import io.whitespots.appsecplugin.models.Finding
import io.whitespots.appsecplugin.utils.MarkdownConverter
import java.awt.Dimension
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import javax.swing.JComponent

object FindingPopupManager {

    fun showFindingPopup(project: Project, finding: Finding, inputEvent: InputEvent?) {
        val popupContent = createPopupContent(finding)

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(popupContent, null)
            .setTitle("Security Finding: ${finding.name}")
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .setMinSize(Dimension(600, 400))
            .setDimensionServiceKey(project, "AppSecFindingPopup", false)
            .createPopup()

        if (inputEvent is MouseEvent) {
            popup.show(RelativePoint(inputEvent))
        } else {
            popup.showCenteredInCurrentWindow(project)
        }
    }

    private fun createPopupContent(finding: Finding): JComponent {
        val browser = JBCefBrowser()

        val htmlContent = MarkdownConverter.toStyledHtml(
            buildFindingMarkdown(finding)
        )

        browser.loadHTML(htmlContent)

        return browser.component.apply {
            preferredSize = Dimension(600, 400)
        }
    }

    private fun buildFindingMarkdown(finding: Finding): String {
        val markdown = StringBuilder()

        markdown.append("## ${finding.name}\n\n")

        // Add severity badge
        val severityColor = when (finding.severity.name.lowercase()) {
            "critical" -> "#ff0000"
            "high" -> "#ff8800"
            "medium" -> "#ffaa00"
            "low" -> "#00aa00"
            "info" -> "#0088ff"
            else -> "#888888"
        }

        markdown.append("**Severity:** <span style='color: $severityColor; font-weight: bold;'>${finding.severity.name}</span>\n\n")

        markdown.append("**Status:** ${finding.triageStatus.name.lowercase().replaceFirstChar { it.uppercase() }}\n\n")

        if (!finding.description.isNullOrBlank()) {
            markdown.append("### Description\n\n")
            markdown.append(finding.description)
            markdown.append("\n\n")
        }

        if (finding.tags.isNotEmpty()) {
            markdown.append("### Tags\n\n")
            finding.tags.forEach { tag ->
                markdown.append("- `$tag`\n")
            }
            markdown.append("\n")
        }

        if (!finding.findingUrl.isNullOrBlank()) {
            markdown.append("---\n\n")
            markdown.append("[View in Whitespots Portal](${finding.findingUrl})\n")
        }

        return markdown.toString()
    }
}
