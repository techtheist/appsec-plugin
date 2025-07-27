package io.whitespots.appsecplugin.utils

import com.intellij.ide.BrowserUtil
import com.intellij.ide.ui.LafManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import io.whitespots.appsecplugin.models.Finding
import io.whitespots.appsecplugin.models.TriageStatus
import io.whitespots.appsecplugin.services.AppSecPluginSettings
import io.whitespots.appsecplugin.services.AutoValidatorService
import io.whitespots.appsecplugin.services.FindingRejectionService
import io.whitespots.appsecplugin.services.FindingsRefreshTopics
import kotlinx.coroutines.*
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest

object ThemeUtils {
    private val LOG = logger<ThemeUtils>()

    fun isDarkTheme(): Boolean {
        return LafManager.getInstance().currentUIThemeLookAndFeel.isDark
    }

    fun configureBrowserForExternalLinks(browser: JBCefBrowser) {
        val cefBrowser = browser.cefBrowser
        val client = cefBrowser.client

        client.addRequestHandler(object : CefRequestHandlerAdapter() {
            override fun onBeforeBrowse(
                browser: CefBrowser,
                frame: CefFrame,
                request: CefRequest,
                userGesture: Boolean,
                isRedirect: Boolean
            ): Boolean {
                val url = request.url

                if (url.startsWith("data:") || url.startsWith("about:")) {
                    return false
                }

                if (url.startsWith("http://") || url.startsWith("https://")) {
                    try {
                        BrowserUtil.browse(url)
                        LOG.info("Opened external URL in system browser: $url")
                    } catch (e: Exception) {
                        LOG.warn("Failed to open URL in system browser: $url", e)
                    }
                    return true
                }

                return false
            }
        })
    }

    fun getIntellijThemeCSS(): String {
        return if (isDarkTheme()) {
            getDarkThemeCSS()
        } else {
            getLightThemeCSS()
        }
    }

    private fun getDarkThemeCSS(): String {
        return """
            body {
                background-color: #1E1F22;
                color: #BCBEC4;
                font-family: 'SF Pro Text', 'Segoe UI', Ubuntu, Arial, sans-serif;
                font-size: 13px;
                text-wrap: pretty;
                overflow-wrap: break-word;
                margin: 16px;
                padding: 0;
            }
            h1, h2, h3, h4, h5, h6 {
                margin-top: 24px;
                margin-bottom: 16px;
                font-weight: 600;
            }
            h1 { font-size: 3em; }
            h2 { font-size: 2em; }
            h3 { font-size: 1.5em; }
            h4 { font-size: 1em; }
            h5 { font-size: 0.83em; }
            h6 { font-size: 0.75em; }
            p {
                margin: 16px 0;
            }
            code {
                background-color: #3C3F41;
                color: #A9B7C6;
                padding: 2px 6px;
                border-radius: 4px;
                font-family: 'JetBrains Mono', 'Courier New', monospace;
                font-size: 12px;
            }
            pre {
                background-color: #3C3F41;
                border: 1px solid #555555;
                border-radius: 6px;
                padding: 16px;
                overflow-x: auto;
                margin: 16px 0;
            }
            pre code {
                background-color: transparent;
                padding: 0;
                border-radius: 0;
            }
            a {
                color: #589DF6;
                text-decoration: underline;
                cursor: pointer;
            }
            blockquote {
                border-left: 4px solid #CC7832;
                margin: 16px 0;
                padding: 4px 0 4px 16px;
                color: #9876AA;
                background-color: rgba(204, 120, 50, 0.1);
                border-radius: 0 4px 4px 0;
            }
            ul, ol {
                margin: 16px 0;
                padding-left: 32px;
            }
            li {
                margin: 4px 0;
            }
            table {
                border-collapse: collapse;
                width: 100%;
                margin: 16px 0;
            }
            th, td {
                border: 1px solid #555555;
                padding: 8px 12px;
                text-align: left;
            }
            th {
                background-color: #3C3F41;
                font-weight: 600;
            }
            tr:nth-child(even) {
                background-color: rgba(255, 255, 255, 0.05);
            }
            strong, b {
                color: #FFC66D;
                font-weight: 600;
            }
            em, i {
                color: #6A8759;
                font-style: italic;
            }
            hr {
                border: none;
                border-top: 1px solid #4C5052;
                margin: 24px 0;
                background: transparent;
            }
        """.trimIndent()
    }

    private fun getLightThemeCSS(): String {
        return """
            body {
                background-color: #FFFFFF;
                color: #000000;
                font-family: 'SF Pro Text', 'Segoe UI', Ubuntu, Arial, sans-serif;
                font-size: 13px;
                text-wrap: pretty;
                overflow-wrap: break-word;
                margin: 16px;
                padding: 0;
            }
            h1, h2, h3, h4, h5, h6 {
                margin-top: 24px;
                margin-bottom: 16px;
                font-weight: 600;
            }
            h1 { font-size: 3em; }
            h2 { font-size: 2em; }
            h3 { font-size: 1.5em; }
            h4 { font-size: 1em; }
            h5 { font-size: 0.83em; }
            h6 { font-size: 0.75em; }
            p {
                margin: 16px 0;
            }
            code {
                background-color: #F5F5F5;
                color: #D73A49;
                padding: 2px 6px;
                border-radius: 4px;
                font-family: 'JetBrains Mono', 'Courier New', monospace;
                font-size: 12px;
            }
            pre {
                background-color: #F6F8FA;
                border: 1px solid #E1E4E8;
                border-radius: 6px;
                padding: 16px;
                overflow-x: auto;
                margin: 16px 0;
            }
            pre code {
                background-color: transparent;
                color: #24292E;
                padding: 0;
                border-radius: 0;
            }
            a {
                color: #0366D6;
                text-decoration: underline;
                cursor: pointer;
            }
            blockquote {
                border-left: 4px solid #DFE2E5;
                margin: 16px 0;
                padding: 4px 0 4px 16px;
                color: #6A737D;
                background-color: rgba(223, 226, 229, 0.2);
                border-radius: 0 4px 4px 0;
            }
            ul, ol {
                margin: 16px 0;
                padding-left: 32px;
            }
            li {
                margin: 4px 0;
            }
            table {
                border-collapse: collapse;
                width: 100%;
                margin: 16px 0;
            }
            th, td {
                border: 1px solid #E1E4E8;
                padding: 8px 12px;
                text-align: left;
            }
            th {
                background-color: #F6F8FA;
                font-weight: 600;
            }
            tr:nth-child(even) {
                background-color: #F6F8FA;
            }
            strong, b {
                color: #24292E;
                font-weight: 600;
            }
            em, i {
                color: #6A737D;
                font-style: italic;
            }
            hr {
                border: none;
                border-top: 1px solid #D0D7DE;
                margin: 24px 0;
                background: transparent;
            }
        """.trimIndent()
    }

    fun createStyledHtmlTemplate(bodyContent: String, title: String = "Content"): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>$title</title>
                <style>
                    ${getIntellijThemeCSS()}
                </style>
            </head>
            <body>
                $bodyContent
            </body>
            </html>
        """.trimIndent()
    }

    fun buildFindingMarkdown(finding: Finding): String {
        val markdown = StringBuilder()

        val settings = service<AppSecPluginSettings>().state
        val severityColor = when (finding.severity.name.lowercase()) {
            "critical" -> "#ff0000"
            "high" -> "#ff8800"
            "medium" -> "#ffaa00"
            "low" -> "#00aa00"
            "info" -> "#0088ff"
            else -> "#888888"
        }
        val url = "<a href='${settings.apiUrl.removeSuffix("/")}/products/${finding.product}/findings/${finding.id}' target='_blank'>${finding.id}</a>"

        markdown.append("### ${url}: <span style='color: $severityColor; font-weight: bold;'>${finding.severity.name}</span> - ${finding.name}\n\n")

        markdown.append("### Status: ${finding.triageStatus.name.lowercase().replaceFirstChar { it.uppercase() }}\n\n")

        if (finding.triageStatus != TriageStatus.REJECTED) {
            markdown.append("[Reject this finding](reject-finding:${finding.id}) | ")
            markdown.append("[Reject finding forever](reject-finding-forever:${finding.id})\n\n")
        }

        if (!finding.lineText.isNullOrBlank()) {
            markdown.append("### Code snippet:\n\n")
            markdown.append("```${finding.language}\n${finding.lineText}\n```")
            markdown.append("\n\n")
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

        return markdown.toString()
    }

    fun prepareMarkdownPage(
        browser: JBCefBrowser,
        finding: Finding,
        project: Project
    ) {
        configureBrowserForExternalLinks(browser)

        val jsQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
        jsQuery.addHandler { query ->
            when {
                query.startsWith("reject-finding:") -> {
                    val findingId = query.substringAfter("reject-finding:").toLongOrNull()
                    if (findingId != null && findingId == finding.id) {
                        handleRejectFinding(project, finding)
                    }
                }
                query.startsWith("reject-finding-forever:") -> {
                    val findingId = query.substringAfter("reject-finding-forever:").toLongOrNull()
                    if (findingId != null && findingId == finding.id) {
                        handleRejectFindingForever(project, finding)
                    }
                }
                query.startsWith("open-external:") -> {
                    val url = query.substringAfter("open-external:")
                    try {
                        BrowserUtil.browse(url)
                        LOG.info("Opened external URL in system browser via JS: $url")
                    } catch (e: Exception) {
                        LOG.warn("Failed to open URL in system browser via JS: $url", e)
                    }
                }
            }
            null
        }

        val htmlContent = MarkdownConverter.toStyledHtml(
            buildFindingMarkdown(finding)
        )

        val htmlWithJS = htmlContent.replace(
            "</body>",
            """
                <script>
                    // Click handling
                    document.addEventListener('click', function(e) {
                        if (e.target.tagName === 'A' && (e.target.href.startsWith('reject-finding:') || e.target.href.startsWith('reject-finding-forever:'))) {
                            e.preventDefault();
                            ${jsQuery.inject("e.target.href")};
                        } else if (e.target.tagName === 'A' && e.target.hasAttribute('target') && e.target.getAttribute('target') === '_blank') {
                            e.preventDefault();
                            ${jsQuery.inject("'open-external:' + e.target.href")};
                        }
                    });
                </script>
                </body>
                """.trimIndent()
        )

        browser.loadHTML(htmlWithJS)
    }

    @OptIn(DelicateCoroutinesApi::class)
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

    @OptIn(DelicateCoroutinesApi::class)
    private fun handleRejectFindingForever(project: Project, finding: Finding) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                LOG.info("Starting reject finding forever process for finding ${finding.id}")
                val autoValidatorService = AutoValidatorService.getInstance(project)
                val ruleResult = autoValidatorService.rejectFindingForever(finding)

                withContext(Dispatchers.Main) {
                    if (ruleResult.isSuccess) {
                        Messages.showInfoMessage(
                            project,
                            "Auto-validator rule has been created to reject similar findings forever.",
                            "Finding Rejected Forever"
                        )
                    } else {
                        val error = ruleResult.exceptionOrNull()
                        Messages.showWarningDialog(
                            project,
                            "Failed to create auto-validator rule: ${error?.message ?: "Unknown error"}",
                            "Error"
                        )
                    }

                    project.messageBus.syncPublisher(FindingsRefreshTopics.REFRESH_TOPIC)
                        .onRefreshRequested()
                }

            } catch (e: Exception) {
                LOG.error("Failed to reject finding forever for finding ${finding.id}", e)
                withContext(Dispatchers.Main) {
                    Messages.showErrorDialog(
                        project,
                        "Failed to reject finding ${finding.id} forever: ${e.message}",
                        "Rejection Failed"
                    )
                }
            }
        }
    }
}
