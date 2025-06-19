package io.whitespots.appsecplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import io.whitespots.appsecplugin.api.FindingApi
import io.whitespots.appsecplugin.api.TagRequest
import io.whitespots.appsecplugin.models.Finding
import io.whitespots.appsecplugin.models.TriageStatus
import io.whitespots.appsecplugin.utils.GitUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@Service(Service.Level.PROJECT)
class FindingRejectionService(private val project: Project) {
    companion object {
        private val LOG = logger<FindingRejectionService>()

        fun getInstance(project: Project): FindingRejectionService {
            return project.getService(FindingRejectionService::class.java)
        }
    }

    suspend fun rejectFinding(finding: Finding): Result<Unit> {
        return try {
            LOG.info("Rejecting finding ${finding.id}")

            val email = GitUtils.getGitEmail(project)

            coroutineScope {
                val statusResult = async { 
                    try {
                        FindingApi.setStatus(finding.id, TriageStatus.REJECTED, "Rejected by developer")
                    } catch (e: Exception) {
                        LOG.error("Failed to set status for finding ${finding.id}", e)
                        false
                    }
                }

                val tagTasks = mutableListOf(
                    async { 
                        try {
                            FindingApi.addTag(finding.id, TagRequest("rejected_by_developer"))
                        } catch (e: Exception) {
                            LOG.warn("Failed to add rejection tag to finding ${finding.id}", e)
                            false
                        }
                    }
                )

                if (!email.isNullOrBlank()) {
                    tagTasks.add(
                        async {
                            try {
                                FindingApi.addTag(finding.id, TagRequest(email))
                            } catch (e: Exception) {
                                LOG.warn("Failed to add email tag to finding ${finding.id}", e)
                                false
                            }
                        }
                    )
                }

                val statusSuccess = statusResult.await()
                if (!statusSuccess) {
                    throw Exception("Failed to change finding status to REJECTED")
                }

                val tagResults = tagTasks.awaitAll()
                val successfulTags = tagResults.count { it }
                LOG.info("Status change successful, added $successfulTags out of ${tagResults.size} tags")
            }

            LOG.info("Successfully rejected finding ${finding.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            LOG.error("Failed to reject finding ${finding.id}", e)
            Result.failure(e)
        }
    }
}
