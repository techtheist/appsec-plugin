package io.whitespots.appsecplugin.toolWindow

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.ui.JBUI
import io.whitespots.appsecplugin.utils.ThemeUtils
import java.net.URL
import java.util.*
import java.util.jar.JarFile
import javax.swing.JComponent

class WebViewToolWindow(private val project: Project) {
    companion object {
        private val LOG = logger<WebViewToolWindow>()
    }

    private val browser = JBCefBrowser()
    private val jsQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)

    val content: JComponent = browser.component.apply {
        border = JBUI.Borders.empty()
    }

    init {
        ThemeUtils.configureBrowserForExternalLinks(browser)
        setupJSBridge()
        loadWebView()
    }

    private fun setupJSBridge() {
        jsQuery.addHandler { request ->
            try {
                LOG.debug("JS Bridge: $request")
                JBCefJSQuery.Response("success")
            } catch (e: Exception) {
                LOG.error("JS Bridge error", e)
                JBCefJSQuery.Response(null, 0, "Error: ${e.message}")
            }
        }
    }

    private fun loadWebView() {
        try {
            val html = createDynamicHtml()
            browser.loadHTML(html)
            LOG.info("WebView loaded successfully")
        } catch (e: Exception) {
            LOG.error("WebView load failed", e)
            browser.loadHTML(createErrorPage(e.message ?: "Unknown error"))
        }
    }

    private fun createDynamicHtml(): String {
        val allAssets = scanWebViewResources()
        val embeddedAssets = embedAssets(allAssets)

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>AppSec WebView</title>
                <style>
                    ${embeddedAssets.css}
                    ${embeddedAssets.fonts}
                </style>
                <script>
                    window.intellij = {
                        query: function(data) {
                            ${jsQuery.inject("data")}
                        }
                    };
                </script>
            </head>
            <body>
                <div id="app"></div>
                <script>
                    ${embeddedAssets.js}
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    private data class EmbeddedAssets(
        val css: String = "",
        val js: String = "",
        val fonts: String = ""
    )

    private fun scanWebViewResources(): Map<String, List<String>> {
        val resources = mutableMapOf<String, MutableList<String>>()
        val webviewPath = "/webview"

        try {
            val url = this::class.java.getResource(webviewPath)
            if (url != null) {
                when (url.protocol) {
                    "jar" -> scanJarResources(url, webviewPath, resources)
                    "file" -> scanFileResources(url, webviewPath, resources)
                }
            }
        } catch (e: Exception) {
            LOG.warn("Failed to scan webview resources", e)
        }

        return resources
    }

    private fun scanJarResources(url: URL, basePath: String, resources: MutableMap<String, MutableList<String>>) {
        val jarPath = url.toString().substringBefore("!").removePrefix("jar:file:")
        val decodedJarPath = java.net.URLDecoder.decode(jarPath, "UTF-8")
        val jarFile = JarFile(decodedJarPath)

        jarFile.entries().asSequence()
            .filter { !it.isDirectory && it.name.startsWith(basePath.removePrefix("/")) }
            .forEach { entry ->
                val resourcePath = "/${entry.name}"
                val extension = getFileExtension(resourcePath)
                resources.getOrPut(extension) { mutableListOf() }.add(resourcePath)
            }

        jarFile.close()
    }

    private fun scanFileResources(url: URL, basePath: String, resources: MutableMap<String, MutableList<String>>) {
        val dir = java.io.File(url.toURI())
        if (dir.exists() && dir.isDirectory) {
            dir.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    val relativePath = file.absolutePath.substringAfter(dir.absolutePath)
                        .replace("\\", "/")
                    val resourcePath = "$basePath$relativePath"
                    val extension = getFileExtension(resourcePath)
                    resources.getOrPut(extension) { mutableListOf() }.add(resourcePath)
                }
        }
    }

    private fun getFileExtension(path: String): String {
        return when {
            path.endsWith(".js") -> "js"
            path.endsWith(".css") -> "css"
            path.endsWith(".ttf") || path.endsWith(".woff") || path.endsWith(".woff2") || path.endsWith(".otf") -> "fonts"
            else -> "other"
        }
    }

    private fun embedAssets(allAssets: Map<String, List<String>>): EmbeddedAssets {
        val css = allAssets["css"]?.joinToString("\n") { loadResourceAsString(it) } ?: ""
        val js = allAssets["js"]?.joinToString("\n") { loadResourceAsString(it) } ?: ""
        val fonts = generateFontFaces(allAssets["fonts"] ?: emptyList())

        return EmbeddedAssets(css, js, fonts)
    }

    private fun generateFontFaces(fontPaths: List<String>): String {
        return fontPaths.mapNotNull { fontPath ->
            this::class.java.getResourceAsStream(fontPath)?.use { stream ->
                val fontData = Base64.getEncoder().encodeToString(stream.readBytes())
                val fileName = fontPath.substringAfterLast("/")
                val fontFamily = deriveFontFamily(fileName)
                val format = deriveFontFormat(fileName)

                """
                @font-face {
                    font-family: '$fontFamily';
                    src: url(data:font/$format;charset=utf-8;base64,$fontData) format('$format');
                }
                """.trimIndent()
            }
        }.joinToString("\n")
    }

    private fun deriveFontFamily(fileName: String): String {
        val baseName = fileName.substringBeforeLast(".")
        return when {
            baseName.contains("Montserrat", ignoreCase = true) -> "Montserrat"
            baseName.contains("Roboto") && baseName.contains("Mono", ignoreCase = true) -> "RobotoMono"
            baseName.contains("Roboto", ignoreCase = true) -> "Roboto"
            baseName.contains("Inter", ignoreCase = true) -> "Inter"
            baseName.contains("Arial", ignoreCase = true) -> "Arial"
            else -> baseName.replace(Regex("[^a-zA-Z0-9]"), "")
        }
    }

    private fun deriveFontFormat(fileName: String): String {
        return when {
            fileName.endsWith(".woff2") -> "woff2"
            fileName.endsWith(".woff") -> "woff"
            fileName.endsWith(".ttf") -> "truetype"
            fileName.endsWith(".otf") -> "opentype"
            else -> "truetype"
        }
    }

    private fun loadResourceAsString(path: String): String {
        return this::class.java.getResourceAsStream(path)?.use { 
            it.bufferedReader().readText() 
        } ?: run {
            LOG.warn("Resource not found: $path")
            "/* Resource not found: $path */"
        }
    }

    private fun createErrorPage(error: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>WebView Error</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        padding: 20px;
                        margin: 0;
                        background: #f5f5f5;
                        color: #333;
                        overflow-y: auto;
                    }
                    .error {
                        background: #fff;
                        padding: 20px;
                        border-radius: 8px;
                        border-left: 4px solid #ff4444;
                        max-width: 600px;
                        margin: 0 auto;
                    }
                </style>
            </head>
            <body>
                <div class="error">
                    <h2>WebView Loading Error</h2>
                    <p>$error</p>
                    <p><small>Check the plugin logs for more details.</small></p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
