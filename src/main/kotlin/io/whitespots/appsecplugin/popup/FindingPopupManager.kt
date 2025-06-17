package io.whitespots.appsecplugin.popup

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import io.whitespots.appsecplugin.models.Finding
import io.whitespots.appsecplugin.models.TriageStatus
import io.whitespots.appsecplugin.services.FindingRejectionService
import io.whitespots.appsecplugin.services.FindingsRefreshTopics
import io.whitespots.appsecplugin.utils.MarkdownConverter
import kotlinx.coroutines.*
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
            .setMinSize(Dimension(600, 400))
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

        val jsQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
        jsQuery.addHandler { query ->
            if (query.startsWith("reject-finding:")) {
                val findingId = query.substringAfter("reject-finding:").toLongOrNull()
                if (findingId != null && findingId == finding.id) {
                    handleRejectFinding(project, finding)
                }
            }
            null
        }

        val htmlContent = MarkdownConverter.toStyledHtml(
            buildFindingMarkdown(project, finding)
        )

        val htmlWithJS = htmlContent.replace(
            "</body>",
            """
            <script>
                document.addEventListener('click', function(e) {
                    if (e.target.tagName === 'A' && e.target.href.startsWith('reject-finding:')) {
                        e.preventDefault();
                        ${jsQuery.inject("e.target.href")};
                    }
                });
            </script>
            </body>
            """.trimIndent()
        )

        browser.loadHTML(htmlWithJS)

        return browser.component.apply {
            preferredSize = Dimension(600, 400)
        }
    }

    private fun buildFindingMarkdown(project: Project, finding: Finding): String {
        val markdown = StringBuilder()

        markdown.append("## ${finding.name}\n\n")

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

        if (finding.triageStatus != TriageStatus.REJECTED) {
            markdown.append("---\n\n")
            markdown.append("[🚫 Reject this finding](reject-finding:${finding.id})\n\n")
        }

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

    private fun handleRejectFinding(project: Project, finding: Finding) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val rejectionService = FindingRejectionService.getInstance(project)
                val result = rejectionService.rejectFinding(finding)

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        Messages.showInfoMessage(
                            project,
                            "Finding ${finding.id} has been rejected successfully.",
                            "Finding Rejected"
                        )

                        project.messageBus.syncPublisher(FindingsRefreshTopics.REFRESH_TOPIC)
                            .onRefreshRequested()
                    } else {
                        val error = result.exceptionOrNull()
                        Messages.showErrorDialog(
                            project,
                            "Failed to reject finding ${finding.id}: ${error?.message ?: "Unknown error"}",
                            "Rejection Failed"
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Messages.showErrorDialog(
                        project,
                        "Failed to reject finding ${finding.id}: ${e.message}",
                        "Rejection Failed"
                    )
                }
            }
        }
    }
}
