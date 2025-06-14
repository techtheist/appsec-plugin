package io.whitespots.appsecplugin.toolWindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.treeStructure.Tree
import com.intellij.ui.jcef.JBCefBrowser
import git4idea.repo.GitRepositoryManager
import io.whitespots.appsecplugin.api.*
import io.whitespots.appsecplugin.models.AssetType
import io.whitespots.appsecplugin.models.Finding
import io.whitespots.appsecplugin.services.FindingsRefreshListener
import io.whitespots.appsecplugin.services.FindingsRefreshTopics
import io.whitespots.appsecplugin.utils.GitUrlParser
import io.whitespots.appsecplugin.utils.MarkdownConverter
import io.whitespots.appsecplugin.utils.ThemeUtils
import kotlinx.coroutines.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.JComponent
import javax.swing.JSplitPane
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class AppSecToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val appSecToolWindow = AppSecToolWindow(project, toolWindow.disposable)
        val content = ContentFactory.getInstance().createContent(appSecToolWindow.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class AppSecToolWindow(private val project: Project, parentDisposable: Disposable) {
    companion object {
        private val LOG = Logger.getInstance(AppSecToolWindow::class.java)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    init {
        Disposer.register(parentDisposable) {
            scope.cancel()
        }
    }

    private val rootNode = DefaultMutableTreeNode("Findings")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel)
    private val descriptionBrowser = JBCefBrowser().apply {
        loadHTML(getEmptyStateHtml())
    }

    fun getContent(): JComponent {
        val treePanel = JBScrollPane(tree)
        val descriptionPanel = descriptionBrowser.component

        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, treePanel, descriptionPanel).apply {
            dividerLocation = 400
        }

        setupTreeListeners()
        subscribeToEvents()

        return panel {
            row {
                button("Refresh") {
                    refreshFindings()
                }
            }
            row {
                cell(splitPane).align(Align.FILL).resizableColumn()
            }.resizableRow()
        }.also {
            scope.launch {
                setLoadingState("Initializing...")
            }
        }
    }

    private fun subscribeToEvents() {
        val connection = project.messageBus.connect(scope)
        connection.subscribe(FindingsRefreshTopics.REFRESH_TOPIC, object : FindingsRefreshListener {
            override fun onRefreshRequested() {
                LOG.info("Received refresh request from message bus.")
                refreshFindings()
            }
        })
    }

    private fun refreshFindings() {
        scope.launch {
            try {
                setLoadingState("Looking for Git repository...")
                val repoUrl = getProjectRepositoryUrl()
                if (repoUrl == null) {
                    setLoadingState("Could not find a Git repository with a remote URL in this project.")
                    return@launch
                }

                setLoadingState("Parsing Git repository URL...")
                val parsedUrl = GitUrlParser.parse(repoUrl)
                if (parsedUrl == null) {
                    setLoadingState("Could not parse Git repository URL: $repoUrl")
                    return@launch
                }

                setLoadingState("Searching for asset: ${parsedUrl.domain}/${parsedUrl.path}...")
                val assets = AssetApi.getAssets(
                    AssetQueryParams(
                        asset_type = AssetType.REPOSITORY.value,
                        search = "${parsedUrl.domain} ${parsedUrl.path}"
                    )
                ).results

                if (assets.isEmpty()) {
                    setLoadingState("This repository is not registered as an asset in Whitespots.")
                    return@launch
                }

                val productID = assets.first().product
                LOG.info("Found matching asset for Product ID: $productID")

                setLoadingState("Loading findings for Product ID: $productID...")
                val findings = FindingApi.getFindings(FindingsQueryParams(product = productID)).results

                if (findings.isEmpty()) {
                    setLoadingState("No findings found for this repository.")
                    return@launch
                }

                updateTree(findings)
            } catch (e: ApiClientConfigurationException) {
                setLoadingState("Plugin not configured. Go to Settings/Preferences > Tools > Whitespots AppSec.")
                LOG.warn(e)
            } catch (e: Exception) {
                setLoadingState("An error occurred: ${e.message}")
                LOG.error("Failed to refresh findings", e)
            }
        }
    }

    private suspend fun updateTree(findings: List<Finding>) = withContext(Dispatchers.Main) {
        val root = DefaultMutableTreeNode("Findings (${findings.size} total)")
        val findingsByFile = findings.filter { !it.filePath.isNullOrBlank() }
            .groupBy { it.filePath!! }
            .toSortedMap()

        findingsByFile.forEach { (filePath, findingsInFile) ->
            val fileNode = FileTreeNode(filePath, findingsInFile.size)
            findingsInFile.sortedBy { it.line }.forEach { finding ->
                fileNode.add(FindingTreeNode(finding))
            }
            root.add(fileNode)
        }

        tree.model = DefaultTreeModel(root)
        for (i in 0 until tree.rowCount) {
            tree.expandRow(i)
        }
        clearDescription()
    }

    private suspend fun setLoadingState(message: String) = withContext(Dispatchers.Main) {
        rootNode.removeAllChildren()
        rootNode.userObject = "Status"
        rootNode.add(DefaultMutableTreeNode(message))
        treeModel.reload(rootNode)
        clearDescription()
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

    private fun setupTreeListeners() {
        tree.addTreeSelectionListener {
            val selectedNode = tree.lastSelectedPathComponent as? FindingTreeNode
            if (selectedNode != null) {
                displayFindingDescription(selectedNode.finding)
            }
        }

        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val path: TreePath = tree.getPathForLocation(e.x, e.y) ?: return
                    (path.lastPathComponent as? FindingTreeNode)?.let {
                        navigateToFinding(it.finding)
                    }
                }
            }
        })
    }

    private fun navigateToFinding(finding: Finding) {
        val projectBasePath = project.basePath ?: return
        val filePath = finding.filePath ?: return
        val line = finding.line?.let { if (it > 0) it - 1 else 0 } ?: 0
        val file = File(projectBasePath, filePath.removePrefix("/"))
        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file)

        if (virtualFile != null) {
            FileEditorManager.getInstance(project).openTextEditor(OpenFileDescriptor(project, virtualFile, line, 0), true)
        } else {
            LOG.warn("Could not find file in project: ${file.absolutePath}")
        }
    }

    private fun displayFindingDescription(finding: Finding) {
        val htmlContent = MarkdownConverter.toStyledHtml(finding.description)
        descriptionBrowser.loadHTML(htmlContent)
    }

    private fun clearDescription() {
        descriptionBrowser.loadHTML(getEmptyStateHtml())
    }

    private fun getEmptyStateHtml(): String {
        return ThemeUtils.createStyledHtmlTemplate(
            "<p>Select a finding to see its description.</p>",
            "AppSec Tool Window"
        )
    }

    class FileTreeNode(filePath: String, count: Int) : DefaultMutableTreeNode("$filePath ($count)")
    class FindingTreeNode(val finding: Finding) : DefaultMutableTreeNode() {
        override fun toString(): String {
            val lineInfo = finding.line?.let { "L$it" } ?: "L?"
            return "$lineInfo - ${finding.severity.name} - ${finding.name}"
        }
    }
}