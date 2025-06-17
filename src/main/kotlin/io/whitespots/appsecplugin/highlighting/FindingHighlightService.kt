package io.whitespots.appsecplugin.highlighting

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.JBColor
import io.whitespots.appsecplugin.models.Finding
import io.whitespots.appsecplugin.models.Severity
import io.whitespots.appsecplugin.services.AppSecPluginSettings
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class FindingHighlightService(private val project: Project) {
    companion object {
        private val LOG = Logger.getInstance(FindingHighlightService::class.java)

        fun getInstance(project: Project): FindingHighlightService {
            return project.getService(FindingHighlightService::class.java)
        }
    }

    private val fileHighlighters = ConcurrentHashMap<VirtualFile, MutableList<RangeHighlighter>>()
    private val findingsByFile = ConcurrentHashMap<String, MutableList<Finding>>()

    fun updateFindings(findings: List<Finding>) {
        LOG.info("Updating ${findings.size} findings across project")
        clearAllHighlights()
        findingsByFile.clear()

        val validFindings = findings.filter { !it.filePath.isNullOrBlank() && it.line != null }
        if (LOG.isDebugEnabled) {
            LOG.debug("Found ${validFindings.size} valid findings with file paths and line numbers")
            validFindings.forEach { finding ->
                LOG.debug("Adding finding for file: ${finding.filePath}, line: ${finding.line}")
            }
        }

        validFindings.forEach { finding ->
            val filePath = finding.filePath!!
            findingsByFile.computeIfAbsent(filePath) { mutableListOf() }.add(finding)
        }

        LOG.info("Grouped findings into ${findingsByFile.size} files")
        if (LOG.isDebugEnabled) {
            findingsByFile.forEach { (filePath, findings) ->
                LOG.debug("File: $filePath has ${findings.size} findings")
            }
        }

        ToolWindowManager.getInstance(project).invokeLater {
            val fileEditorManager = FileEditorManager.getInstance(project)
            val allEditors = fileEditorManager.allEditors
            LOG.info("Applying highlights to ${allEditors.size} open editors")

            var highlightedFiles = 0
            allEditors.forEach { fileEditor ->
                if (fileEditor is TextEditor) {
                    val virtualFile = fileEditor.file
                    val relativePath = getRelativeFilePath(virtualFile)
                    LOG.debug("Processing editor file: ${virtualFile.path}, relative path: $relativePath")

                    if (relativePath != null && findingsByFile.containsKey(relativePath)) {
                        LOG.debug("Applying highlights to file: $relativePath")
                        highlightFindingsInEditor(fileEditor.editor, virtualFile, findingsByFile[relativePath]!!)
                        highlightedFiles++
                    } else if (LOG.isDebugEnabled) {
                        LOG.debug("No findings for file: $relativePath")
                    }
                }
            }
            LOG.info("Applied highlights to $highlightedFiles files")
        }
    }

    fun highlightFindingsInEditor(editor: Editor, virtualFile: VirtualFile, findings: List<Finding>) {
        val highlightingEnabled = AppSecPluginSettings.instance.state.highlightFindings
        LOG.debug("Highlighting ${findings.size} findings in ${virtualFile.name}, enabled: $highlightingEnabled")

        if (!highlightingEnabled) {
            LOG.debug("Highlighting is disabled in settings")
            return
        }

        val document = editor.document
        val markupModel = editor.markupModel
        val highlighters = mutableListOf<RangeHighlighter>()
        LOG.debug("Document has ${document.lineCount} lines")

        var successfulHighlights = 0
        findings.forEach { finding ->
            val line = finding.line
            LOG.debug("Processing finding: ${finding.name} at line $line")
            if (line != null && line > 0 && line <= document.lineCount) {
                val lineStartOffset = document.getLineStartOffset(line - 1)
                val lineEndOffset = document.getLineEndOffset(line - 1)
                LOG.debug("Adding highlighter for line $line (offset $lineStartOffset-$lineEndOffset)")

                val textAttributes = getTextAttributesForSeverity(finding.severity)

                val highlighter = markupModel.addRangeHighlighter(
                    lineStartOffset,
                    lineEndOffset,
                    HighlighterLayer.WARNING,
                    textAttributes,
                    HighlighterTargetArea.LINES_IN_RANGE
                )

                highlighter.putUserData(FindingHighlightGutterIconRenderer.FINDING_KEY, finding)
                highlighter.gutterIconRenderer = FindingHighlightGutterIconRenderer(finding)

                highlighters.add(highlighter)
                successfulHighlights++
                LOG.debug("Successfully added highlighter for finding: ${finding.name}")
            } else {
                LOG.warn("Invalid line number $line for finding: ${finding.name} (document has ${document.lineCount} lines)")
            }
        }

        fileHighlighters[virtualFile] = highlighters
        LOG.info("Added ${successfulHighlights}/${findings.size} highlighters for ${virtualFile.name}")
    }

    fun clearHighlightsForFile(virtualFile: VirtualFile) {
        fileHighlighters[virtualFile]?.forEach { highlighter ->
            highlighter.dispose()
        }
        fileHighlighters.remove(virtualFile)
    }

    private fun clearAllHighlights() {
        fileHighlighters.values.forEach { highlighters ->
            highlighters.forEach { it.dispose() }
        }
        fileHighlighters.clear()
    }

    fun getFindingsForFile(virtualFile: VirtualFile): List<Finding> {
        val relativePath = getRelativeFilePath(virtualFile)
        return if (relativePath != null) {
            findingsByFile[relativePath] ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun getRelativeFilePath(virtualFile: VirtualFile): String? {
        val projectBaseVirtualFile = project.getBaseDirectories()

        val relativePath = VfsUtil.getRelativePath(virtualFile, projectBaseVirtualFile.first())
        return relativePath
    }

    private fun getTextAttributesForSeverity(severity: Severity): TextAttributes {
        val color = when (severity) {
            Severity.CRITICAL -> JBColor(Color(255, 0, 0), Color(255, 100, 100)) // Red
            Severity.HIGH -> JBColor(Color(255, 165, 0), Color(255, 200, 100)) // Orange
            Severity.MEDIUM -> JBColor(Color(255, 255, 0), Color(255, 255, 150)) // Yellow
            Severity.LOW -> JBColor(Color(0, 255, 0), Color(150, 255, 150)) // Green
            Severity.INFO -> JBColor(Color(0, 0, 255), Color(100, 100, 255)) // Blue
        }

        return TextAttributes().apply {
            effectType = EffectType.WAVE_UNDERSCORE
            effectColor = color
            errorStripeColor = color
        }
    }
}
