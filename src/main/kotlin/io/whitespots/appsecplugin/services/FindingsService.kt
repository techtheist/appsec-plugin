package io.whitespots.appsecplugin.services

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import git4idea.repo.GitRepositoryManager
import io.whitespots.appsecplugin.api.*
import io.whitespots.appsecplugin.exceptions.FindingsException
import io.whitespots.appsecplugin.models.*
import io.whitespots.appsecplugin.utils.GitUrlParser

class FindingsService(private val project: Project) {
    companion object {
        private val LOG = logger<FindingsService>()
    }

    suspend fun refreshFindings(onStatusUpdate: suspend (String) -> Unit): List<Finding> {
        onStatusUpdate("Looking for Git repository...")
        val repoUrl = getProjectRepositoryUrl()
            ?: throw FindingsException("Could not find a Git repository with a remote URL in this project.")

        onStatusUpdate("Parsing Git repository URL...")
        val parsedUrl = GitUrlParser.parse(repoUrl)
            ?: throw FindingsException("Could not parse Git repository URL: $repoUrl")

        onStatusUpdate("Searching for assets: ${parsedUrl.domain}/${parsedUrl.path}...")
        val assets = AssetApi.getAssets(
            AssetQueryParams(
                asset_type = AssetType.REPOSITORY.value,
                search = "${parsedUrl.domain} ${parsedUrl.path}"
            )
        ).results

        if (assets.isEmpty()) {
            throw FindingsException("This repository is not registered as an asset in Whitespots.")
        }

        val productIds = assets.map { it.product }.distinct()
        LOG.info("Found ${assets.size} assets across ${productIds.size} products: $productIds")

        onStatusUpdate("Loading findings for ${productIds.size} products...")
        val settings = AppSecPluginSettings.instance.state

        val enabledSeverities = settings.enabledSeverities.mapNotNull { severityName ->
            try {
                Severity.valueOf(severityName)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        val enabledTriageStatuses = settings.enabledTriageStatuses.mapNotNull { statusName ->
            try {
                TriageStatus.valueOf(statusName)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        val allFindings = mutableListOf<Finding>()

        productIds.forEach { productId ->
            LOG.info("Fetching findings for Product ID: $productId")
            onStatusUpdate("Loading findings for Product ID: $productId...")

            val productFindings = FindingApi.getAllFindings(
                FindingsQueryParams(
                    product = productId,
                    severityIn = enabledSeverities.ifEmpty { null },
                    triageStatusIn = enabledTriageStatuses.ifEmpty { null }
                ),
                maxFindings = settings.maxFindings
            )

            LOG.info("Found ${productFindings.size} findings for Product ID: $productId")
            allFindings.addAll(productFindings)

            if (allFindings.size >= settings.maxFindings) {
                LOG.info("Reached maximum findings limit of ${settings.maxFindings}")
            }
        }

        val finalFindings = allFindings.take(settings.maxFindings)
        LOG.info("Total findings retrieved: ${finalFindings.size} from ${productIds.size} products")

        if (finalFindings.isEmpty()) {
            throw FindingsException("No findings found for this repository.")
        }

        return finalFindings
    }

    private fun getProjectRepositoryUrl(): String? {
        val repositories = GitRepositoryManager.getInstance(project).repositories
        if (repositories.isEmpty()) {
            LOG.warn("No Git repositories found in the project.")
            return null
        }
        val remoteUrl = repositories.firstNotNullOfOrNull { it.remotes.firstOrNull()?.firstUrl }
        if (remoteUrl != null) {
            LOG.info("Found repository URL: $remoteUrl")
        } else {
            LOG.warn("No remotes found for any repository in the project.")
        }
        return remoteUrl
    }
}

