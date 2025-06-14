package io.whitespots.appsecplugin.utils

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet

object MarkdownConverter {
    private val options = MutableDataSet()
    private val parser: Parser = Parser.builder(options).build()
    private val renderer: HtmlRenderer = HtmlRenderer.builder(options).build()

    /**
     * Converts a Markdown string to an HTML string.
     */
    fun toHtml(markdown: String?): String {
        if (markdown.isNullOrBlank()) {
            return "<html><body><p>No description provided.</p></body></html>"
        }
        val document = parser.parse(markdown)
        return renderer.render(document)
    }

    /**
     * Converts a Markdown string to a styled HTML string with IntelliJ theme-aware CSS.
     */
    fun toStyledHtml(markdown: String?): String {
        if (markdown.isNullOrBlank()) {
            return ThemeUtils.createStyledHtmlTemplate("<p>No description provided.</p>", "Finding Description")
        }
        val document = parser.parse(markdown)
        val bodyContent = renderer.render(document)
        return ThemeUtils.createStyledHtmlTemplate(bodyContent, "Finding Description")
    }
}