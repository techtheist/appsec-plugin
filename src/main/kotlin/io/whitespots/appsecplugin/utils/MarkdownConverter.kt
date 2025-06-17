package io.whitespots.appsecplugin.utils

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet

object MarkdownConverter {
    private val options = MutableDataSet()
    private val parser: Parser = Parser.builder(options).build()
    private val renderer: HtmlRenderer = HtmlRenderer.builder(options).build()

    fun toStyledHtml(markdown: String?): String {
        if (markdown.isNullOrBlank()) {
            return ThemeUtils.createStyledHtmlTemplate("<p>No description provided.</p>", "Finding Description")
        }
        val document = parser.parse(markdown)
        val bodyContent = renderer.render(document)
        return ThemeUtils.createStyledHtmlTemplate(bodyContent, "Finding Description")
    }
}