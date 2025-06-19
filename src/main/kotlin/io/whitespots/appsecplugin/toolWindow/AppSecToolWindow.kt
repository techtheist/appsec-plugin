package io.whitespots.appsecplugin.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import io.whitespots.appsecplugin.exceptions.ApiClientConfigurationException
import io.whitespots.appsecplugin.exceptions.FindingsException
import io.whitespots.appsecplugin.highlighting.FindingHighlightService
import io.whitespots.appsecplugin.models.Finding
import io.whitespots.appsecplugin.services.FindingsRefreshListener
import io.whitespots.appsecplugin.services.FindingsRefreshTopics
import io.whitespots.appsecplugin.services.FindingsService
import io.whitespots.appsecplugin.settings.AppSecPluginSettingsConfigurable
import io.whitespots.appsecplugin.utils.ThemeUtils
import kotlinx.coroutines.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.JComponent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class AppSecToolWindow(private val project: Project, private val parentDisposable: Disposable) {
    companion object {
        private val LOG = logger<AppSecToolWindow>()
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
    private val loadingIcon = JBLabel(AnimatedIcon.Default.INSTANCE).apply {
        preferredSize = JBUI.size(14)
    }
    private var refreshAction: AnAction? = null
    private var expandAllAction: AnAction? = null
    private var collapseAllAction: AnAction? = null
    private var toggleAction: AnAction? = null
    private var settingsAction: AnAction? = null
    private var toolbar: ActionToolbar? = null
    private var isDescriptionVisible = true
    private var splitPane: OnePixelSplitter? = null

    fun getContent(): JComponent {
        val treePanel = JBScrollPane(tree).apply {
            border = JBUI.Borders.empty()
            minimumSize = JBUI.size(200, 0)
        }

        val descriptionPanel = descriptionBrowser.component

        splitPane = OnePixelSplitter(true, 0.6f).apply {
            firstComponent = treePanel
            secondComponent = descriptionPanel
            setHonorComponentsMinimumSize(true)
            border = JBUI.Borders.emptyLeft(1)
        }

        createActions()
        createToolbar()
        setupTreeListeners()
        subscribeToEvents()

        return panel {
            row {
                cell(toolbar!!.component)
                cell(loadingIcon)
                    .apply {
                        visible(false)
                    }
            }
            row {
                cell(splitPane!!)
                    .align(Align.FILL)
            }.resizableRow()
        }.also {
            scope.launch {
                setLoadingState("Refresh to get findings")
            }
        }
    }

    private fun createActions() {
        refreshAction = object : AnAction("Refresh Findings", "Refresh findings", AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) {
                refreshFindings()
            }

            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
        }

        expandAllAction = object : AnAction("Expand All", "Expand all findings", AllIcons.Actions.Expandall) {
            override fun actionPerformed(e: AnActionEvent) {
                TreeUtil.expandAll(tree)
            }

            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
        }

        collapseAllAction = object : AnAction("Collapse All", "Collapse all findings", AllIcons.Actions.Collapseall) {
            override fun actionPerformed(e: AnActionEvent) {
                TreeUtil.collapseAll(tree, 0)
            }

            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
        }

        toggleAction = object : AnAction("Hide/Show Description", "Hide/Show description", AllIcons.Actions.ToggleVisibility) {
            override fun actionPerformed(e: AnActionEvent) {
                toggleDescriptionPanel()
            }

            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
        }

        settingsAction = object : AnAction("Settings", "Open plugin settings", AllIcons.General.Settings) {
            override fun actionPerformed(e: AnActionEvent) {
                openSettings()
            }

            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
        }
    }

    private fun createToolbar() {
        val actionGroup = DefaultActionGroup().apply {
            add(refreshAction!!)
            add(expandAllAction!!)
            add(collapseAllAction!!)
            add(toggleAction!!)
            addSeparator()
            add(settingsAction!!)
        }

        toolbar = ActionManager.getInstance().createActionToolbar(
            "AppSecToolWindow",
            actionGroup,
            true
        ).apply {
            targetComponent = splitPane
            component.border = JBUI.Borders.empty(4, 0, 4, 8)
        }
    }

    private fun subscribeToEvents() {
        val connection = project.messageBus.connect(parentDisposable)

        connection.subscribe(FindingsRefreshTopics.REFRESH_TOPIC, object : FindingsRefreshListener {
            override fun onRefreshRequested() {
                LOG.info("Received refresh request from message bus.")
                refreshFindings()
            }
        })
    }

    private fun toggleDescriptionPanel() {
        isDescriptionVisible = !isDescriptionVisible
        splitPane?.let { sp ->
            if (isDescriptionVisible) {
                sp.secondComponent = descriptionBrowser.component
                sp.proportion = 0.6f
            } else {
                sp.secondComponent = null
                sp.proportion = 1.0f
            }
            sp.revalidate()
            sp.repaint()
        }
    }

    private fun openSettings() {
        try {
            ShowSettingsUtil.getInstance().showSettingsDialog(
                    project,
                    AppSecPluginSettingsConfigurable::class.java
            )
        } catch (e: Exception) {
            LOG.warn("Failed to open plugin settings", e)
        }
    }

    private fun setLoadingIndicator(visible: Boolean) {
        ToolWindowManager.getInstance(project).invokeLater {
            loadingIcon.isVisible = visible
            refreshAction?.templatePresentation?.isEnabled = !visible
            toolbar?.updateActionsAsync()
        }
    }

    private fun refreshFindings() {
        scope.launch {
            setLoadingIndicator(true)
            try {
                val findingsService = FindingsService(project)
                val findings = findingsService.refreshFindings { status ->
                    setLoadingState(status)
                }
                updateTree(findings)
            } catch (e: ApiClientConfigurationException) {
                setLoadingState("Plugin not configured. Go to Settings/Preferences > Tools > Whitespots AppSec.")
                LOG.warn(e)
            } catch (e: FindingsException) {
                setLoadingState(e.message ?: "An error occurred while loading findings")
                LOG.warn(e)
            } catch (e: Exception) {
                setLoadingState("An error occurred: ${e.message}")
                LOG.error("Failed to refresh findings", e)
            } finally {
                setLoadingIndicator(false)
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
        TreeUtil.expandAll(tree)
        clearDescription()

        val highlightService = FindingHighlightService.getInstance(project)
        highlightService.updateFindings(findings)
    }

    private suspend fun setLoadingState(message: String) = withContext(Dispatchers.Main) {
        rootNode.removeAllChildren()
        rootNode.userObject = DefaultMutableTreeNode(message)
        treeModel.reload(rootNode)
        clearDescription()
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
            FileEditorManager.getInstance(project)
                    .openTextEditor(OpenFileDescriptor(project, virtualFile, line, 0), true)
        } else {
            LOG.warn("Could not find file in project: ${file.absolutePath}")
        }
    }

    private fun displayFindingDescription(finding: Finding) {
        ThemeUtils.prepareMarkdownPage(descriptionBrowser, finding, project)
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

    data class FileTreeNode(val filePath: String, val count: Int) : DefaultMutableTreeNode() {
        override fun toString(): String {
            val fileName = filePath.substringAfterLast('/')
            return "$fileName count: $count"
        }
    }

    data class FindingTreeNode(val finding: Finding) : DefaultMutableTreeNode() {
        override fun toString(): String {
            val lineInfo = finding.line?.let { "L$it" } ?: "L?"
            return "$lineInfo - ${finding.severity.name} - ${finding.name}"
        }
    }
}