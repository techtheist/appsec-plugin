package io.whitespots.appsecplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import io.whitespots.appsecplugin.api.AutoValidatorApi
import io.whitespots.appsecplugin.api.AutoValidatorInstruction
import io.whitespots.appsecplugin.api.AutoValidatorRuleRequest
import io.whitespots.appsecplugin.models.Finding
import io.whitespots.appsecplugin.utils.GitUtils

@Service(Service.Level.PROJECT)
class AutoValidatorService(private val project: Project) {
    companion object {
        private val LOG = logger<AutoValidatorService>()

        fun getInstance(project: Project): AutoValidatorService {
            return project.getService(AutoValidatorService::class.java)
        }
    }

    suspend fun rejectFindingForever(finding: Finding): Result<Unit> {
        return try {
            LOG.info("Creating auto-validator rule to reject finding ${finding.id} forever")

            val email = GitUtils.getGitEmail(project)
            val instructions = mutableListOf<AutoValidatorInstruction>()

            instructions.add(
                AutoValidatorInstruction(
                    field = "Finding__name",
                    value = finding.name,
                    negate = false,
                    regex = false
                )
            )

            finding.filePath?.let { filePath ->
                instructions.add(
                    AutoValidatorInstruction(
                        field = "Finding__file_path",
                        value = filePath,
                        negate = false,
                        regex = false
                    )
                )
            }

            val tags = mutableListOf("rejected_by_developer")
            email?.let { tags.add(it) }

            val rule = AutoValidatorRuleRequest(
                isActive = true,
                actionChoices = 0,
                instructions = instructions,
                tags = tags,
                groups = emptyList(),
                allowAllProducts = true,
                issuesAutoCreateOnVerify = false,
                affectedProductsCluster = null,
                readOnly = false
            )

            val success = AutoValidatorApi.createRule(rule)

            if (success) {
                LOG.info("Successfully created auto-validator rule for finding ${finding.id}")
                Result.success(Unit)
            } else {
                throw Exception("Failed to create auto-validator rule")
            }
        } catch (e: Exception) {
            LOG.error("Failed to create auto-validator rule for finding ${finding.id}", e)
            Result.failure(e)
        }
    }
}
