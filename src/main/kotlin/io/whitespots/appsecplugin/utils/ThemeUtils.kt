package io.whitespots.appsecplugin.utils

import com.intellij.ide.ui.LafManager

object ThemeUtils {
    fun isDarkTheme(): Boolean {
        return LafManager.getInstance().currentUIThemeLookAndFeel.isDark
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
                text-wrap: wrap;
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
                text-decoration: none;
            }
            a:hover {
                text-decoration: underline;
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
                border-top: 1px solid #555555;
                margin: 24px 0;
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
                text-wrap: wrap;
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
                text-decoration: none;
            }
            a:hover {
                text-decoration: underline;
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
                border-top: 1px solid #E1E4E8;
                margin: 24px 0;
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
}
