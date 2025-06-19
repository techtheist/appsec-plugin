package io.whitespots.appsecplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import io.whitespots.appsecplugin.exceptions.ApiClientConfigurationException
import io.whitespots.appsecplugin.exceptions.FindingsException
import io.whitespots.appsecplugin.highlighting.FindingHighlightService
import io.whitespots.appsecplugin.models.Finding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Service(Service.Level.PROJECT)
class ProjectFindingsService(private val project: Project, private val coroutineScope: CoroutineScope) {
    companion object {
        private val LOG = logger<ProjectFindingsService>()

        fun getInstance(project: Project): ProjectFindingsService {
            return project.getService(ProjectFindingsService::class.java)
        }
    }

    private val _findingsState = MutableStateFlow<FindingsState>(FindingsState.NotLoaded)
    val findingsState: StateFlow<FindingsState> = _findingsState.asStateFlow()

    private val _statusMessage = MutableStateFlow<String>("Ready")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val refreshScope = coroutineScope.childScope("FindingsRefresh")

    private var refreshJob: Job? = null

    init {
        LOG.info("ProjectFindingsService initialized for project: ${project.name}")
        subscribeToRefreshEvents()
    }

    private fun subscribeToRefreshEvents() {
        val messageBusConnection = project.messageBus.connect(coroutineScope)
        messageBusConnection.subscribe(FindingsRefreshTopics.REFRESH_TOPIC, object : FindingsRefreshListener {
            override fun onRefreshRequested() {
                LOG.info("Received refresh request from message bus")
                refreshFindings()
            }
        })
    }

    fun startAutoRefresh() {
        if (refreshJob?.isActive == true) {
            LOG.debug("Auto refresh already in progress")
            return
        }

        refreshFindings()
    }

    fun refreshFindings() {
        refreshJob?.cancel()

        refreshJob = refreshScope.launch {
            try {
                _findingsState.value = FindingsState.Loading
                _statusMessage.value = "Loading findings..."

                val findingsService = FindingsService(project)
                val findings = findingsService.refreshFindings { status ->
                    _statusMessage.value = status
                }

                _findingsState.value = FindingsState.Loaded(findings)
                _statusMessage.value = "Loaded ${findings.size} findings"

                updateHighlighting(findings)

                LOG.info("Successfully loaded ${findings.size} findings")

            } catch (e: ApiClientConfigurationException) {
                val message = "Plugin not configured. Go to Settings > Tools > Whitespots AppSec"
                _findingsState.value = FindingsState.Error(message)
                _statusMessage.value = message
                LOG.warn("API configuration error", e)

            } catch (e: FindingsException) {
                val message = e.message ?: "Failed to load findings"
                _findingsState.value = FindingsState.Error(message)
                _statusMessage.value = message
                LOG.warn("Findings error", e)

            } catch (e: CancellationException) {
                LOG.debug("Findings refresh was cancelled")
                throw e

            } catch (e: Exception) {
                val message = "An error occurred: ${e.message}"
                _findingsState.value = FindingsState.Error(message)
                _statusMessage.value = message
                LOG.error("Unexpected error during findings refresh", e)
            }
        }
    }

    private fun updateHighlighting(findings: List<Finding>) {
        try {
            val highlightService = FindingHighlightService.getInstance(project)
            highlightService.updateFindings(findings)
            LOG.debug("Updated highlighting for ${findings.size} findings")
        } catch (e: Exception) {
            LOG.warn("Failed to update highlighting", e)
        }
    }

    fun isConfigured(): Boolean {
        val settings = AppSecPluginSettings.instance.state
        return settings.apiUrl.isNotBlank() && settings.apiToken.isNotBlank()
    }
}

sealed class FindingsState {
    object NotLoaded : FindingsState()
    object Loading : FindingsState()
    data class Loaded(val findings: List<Finding>) : FindingsState()
    data class Error(val message: String) : FindingsState()
}
