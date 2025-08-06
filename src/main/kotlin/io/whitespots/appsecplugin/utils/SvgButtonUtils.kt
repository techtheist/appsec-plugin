package io.whitespots.appsecplugin.utils

import com.intellij.openapi.diagnostic.logger
import java.io.IOException
import java.util.*

object SvgButtonUtils {
    private val LOG = logger<SvgButtonUtils>()

    private val svgCache = mutableMapOf<String, String>()

    fun loadSvgAsDataUri(resourcePath: String): String {
        return svgCache.getOrPut(resourcePath) {
            try {
                val inputStream = SvgButtonUtils::class.java.classLoader.getResourceAsStream(resourcePath)
                    ?: throw IOException("Resource not found: $resourcePath")

                val svgContent = inputStream.bufferedReader().use { it.readText() }
                val encodedSvg = Base64.getEncoder().encodeToString(svgContent.toByteArray())
                "data:image/svg+xml;base64,$encodedSvg"
            } catch (e: Exception) {
                LOG.warn("Failed to load SVG resource: $resourcePath", e)
                ""
            }
        }
    }

    fun createSvgButton(svgResourcePath: String, altText: String, commandUrl: String, title: String): String {
        val svgDataUri = loadSvgAsDataUri(svgResourcePath)
        return """<div class="tooltip"><a href="$commandUrl"><img src="$svgDataUri" alt="$altText" style="height: 20px;"/><span class="tooltiptext">$title</span></a></div>"""
    }

    fun createRejectButton(findingId: Long): String {
        return createSvgButton(
            "icons/ButtonReject.svg",
            "Reject finding",
            "reject-finding:$findingId",
            "Reject this finding"
        )
    }

    fun createRejectForeverButton(findingId: Long): String {
        return createSvgButton(
            "icons/ButtonRejectForever.svg",
            "Reject finding forever",
            "reject-finding-forever:$findingId",
            "Reject all findings with this name and file path"
        )
    }
}
