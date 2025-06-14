package io.whitespots.appsecplugin.utils

data class ParsedGitUrl(val domain: String, val path: String)

object GitUrlParser {

    private const val SUFFIX = ".git"
    private val PREFIXES = listOf("git@", "https://", "ssh://", "git://")

    /**
     * Example: "git@github.com:org/repo.git" -> ParsedGitUrl(domain="github.com", path="org/repo")
     */
    fun parse(url: String?): ParsedGitUrl? {
        if (url.isNullOrBlank()) return null

        var workingUrl = url.removeSuffix(SUFFIX)

        val prefix = PREFIXES.find { workingUrl.startsWith(it) } ?: return null
        workingUrl = workingUrl.substring(prefix.length)

        if (prefix != "git@") {
            workingUrl = workingUrl.substringAfter('@', workingUrl)
        }

        val divider = if (prefix == "git@") ':' else '/'
        val dividerIndex = workingUrl.indexOf(divider)
        if (dividerIndex == -1) return null

        val domain = workingUrl.substring(0, dividerIndex)
        val path = workingUrl.substring(dividerIndex + 1)

        return ParsedGitUrl(domain, path)
    }
}