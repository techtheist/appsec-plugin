package io.whitespots.appsecplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import io.whitespots.appsecplugin.api.AutoValidatorApi
import io.whitespots.appsecplugin.api.AutoValidatorInstruction
import io.whitespots.appsecplugin.api.AutoValidatorRuleRequest
import io.whitespots.appsecplugin.api.QueryParamsAutovalidatorRule
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

    suspend fun rejectFindingForever(finding: Finding): Result<RuleCreationResult> {
        return try {
            LOG.info("Starting reject finding forever process for finding ${finding.id}")

            val queryParams = QueryParamsAutovalidatorRule(
                actionChoices = 0,
                search = "\"${finding.name}\" \"${finding.filePath ?: ""}\""
            )

            val rulesResult = AutoValidatorApi.getRules(queryParams)

            if (rulesResult.isFailure) {
                LOG.warn("Failed to check existing validator rules: ${rulesResult.exceptionOrNull()?.message}")
            } else {
                val rules = rulesResult.getOrNull()
                if (rules != null) {
                    val filtered = rules.results.filter { rule ->
                        val nameMatch = rule.instructions.any { 
                            it.field == "Finding__name" && it.value == finding.name 
                        }
                        val filePathMatch = finding.filePath?.let { filePath ->
                            rule.instructions.any { 
                                it.field == "Finding__file_path" && it.value == filePath 
                            }
                        } ?: true

                        nameMatch && filePathMatch
                    }

                    if (filtered.isNotEmpty()) {
                        LOG.info("Found ${filtered.size} existing rule(s) for this finding")
                        return Result.success(RuleCreationResult.ExistingRulesFound(filtered.size, queryParams))
                    }
                }
            }

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
                issuesAutoCreateOnVerify = null,
                affectedProductsCluster = null,
                readOnly = false
            )

            val createResult = AutoValidatorApi.createRule(rule)

            if (createResult.isSuccess) {
                val ruleId = createResult.getOrThrow()
                LOG.info("Successfully created auto-validator rule with ID: $ruleId for finding ${finding.id}")
                Result.success(RuleCreationResult.RuleCreated(ruleId))
            } else {
                throw createResult.exceptionOrNull() ?: Exception("Failed to create auto-validator rule")
            }
        } catch (e: Exception) {
            LOG.error("Failed to create auto-validator rule for finding ${finding.id}", e)
            Result.failure(e)
        }
    }

    sealed class RuleCreationResult {
        data class RuleCreated(val ruleId: Long) : RuleCreationResult()
        data class ExistingRulesFound(val count: Int, val queryParams: io.whitespots.appsecplugin.api.QueryParamsAutovalidatorRule) : RuleCreationResult()
    }
}
