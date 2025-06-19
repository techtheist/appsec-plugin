package io.whitespots.appsecplugin.utils

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import git4idea.config.GitConfigUtil
import git4idea.repo.GitRepositoryManager

object GitUtils {
    private val LOG = logger<GitUtils>()

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
}
