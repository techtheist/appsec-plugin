package io.whitespots.appsecplugin.services

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import git4idea.repo.GitRepositoryManager
import io.whitespots.appsecplugin.api.AssetApi
import io.whitespots.appsecplugin.api.AssetQueryParams
import io.whitespots.appsecplugin.api.FindingApi
import io.whitespots.appsecplugin.api.FindingsQueryParams
import io.whitespots.appsecplugin.exceptions.FindingsException
import io.whitespots.appsecplugin.models.AssetType
import io.whitespots.appsecplugin.models.Finding
import io.whitespots.appsecplugin.models.Severity
import io.whitespots.appsecplugin.models.TriageStatus
import io.whitespots.appsecplugin.utils.GitUtils

class FindingsService(private val project: Project) {
    companion object {
        private val LOG = logger<FindingsService>()
    }

    suspend fun refreshFindings(onStatusUpdate: suspend (String) -> Unit): List<Finding> {
        onStatusUpdate("Looking for Git repository...")
        val repoUrl = getProjectRepositoryUrl()
            ?: throw FindingsException("Could not find a Git repository with a remote URL in this project.")

        onStatusUpdate("Parsing Git repository URL...")
        val parsedUrl = GitUtils.parse(repoUrl)
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

        val assetValues = assets.map { it.value }.distinct()
        LOG.info("Found ${assets.size} assets with values: $assetValues")
        onStatusUpdate("Loading findings for ${assets.size} assets...")

        val settings = AppSecPluginSettings.instance.state
        val enabledSeverities = settings.enabledSeverities.mapNotNull { severityName ->
            try {
                Severity.valueOf(severityName)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        val enabledTriageStatuses = settings.enabledTriageStatuses.mapNotNull { statusName ->
            try {
                TriageStatus.valueOf(statusName)
            } catch (_: IllegalArgumentException) {
                null
            }
        }

        val allFindings = mutableListOf<Finding>()
        LOG.info("Fetching findings for asset values: $assetValues")
        onStatusUpdate("Loading findings for asset values: $assetValues...")

        val productFindings = FindingApi.getAllFindings(
            FindingsQueryParams(
                severityIn = enabledSeverities.ifEmpty { null },
                triageStatusIn = enabledTriageStatuses.ifEmpty { null },
                assetsIn = mapOf("0" to assetValues)
            ),
            maxFindings = settings.maxFindings
        )

        LOG.info("Found ${productFindings.size} findings for asset values: $assetValues")
        allFindings.addAll(productFindings)

        if (allFindings.size >= settings.maxFindings) {
            LOG.info("Reached maximum findings limit of ${settings.maxFindings}")
        }

        val finalFindings = allFindings.take(settings.maxFindings)
        LOG.info("Total findings retrieved: ${finalFindings.size} from ${assetValues.size} asset values")

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

