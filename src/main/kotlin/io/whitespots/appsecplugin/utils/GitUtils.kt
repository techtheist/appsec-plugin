package io.whitespots.appsecplugin.utils

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import git4idea.config.GitConfigUtil
import git4idea.repo.GitRepositoryManager

object GitUtils {
    private val LOG = logger<GitUtils>()
    private const val SUFFIX = ".git"
    private val PREFIXES = listOf("git@", "https://", "ssh://", "git://")

    data class ParsedGitUrl(val domain: String, val path: String)

    fun getGitEmail(project: Project): String? {
        return try {
            val repositories = GitRepositoryManager.getInstance(project).repositories
            if (repositories.isEmpty()) {
                LOG.warn("No Git repositories found in the project.")
                return null
            }

            val repository = repositories.first()
            val email = GitConfigUtil.getValue(project, repository.root, "user.email")

            if (email.isNullOrBlank()) {
                LOG.warn("Git user email not configured")
                null
            } else {
                LOG.info("Found Git user email: $email")
                email
            }
        } catch (e: Exception) {
            LOG.error("Failed to get Git user email", e)
            null
        }
    }

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

        val domain = workingUrl.take(dividerIndex).substringBefore(":")
        val path = workingUrl.substring(dividerIndex + 1).substringBefore(".git")

        return ParsedGitUrl(domain, path)
    }
}
