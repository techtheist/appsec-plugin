package io.whitespots.appsecplugin.debug

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtil
import io.whitespots.appsecplugin.highlighting.FindingHighlightService
import io.whitespots.appsecplugin.models.Finding
import io.whitespots.appsecplugin.models.Severity
import io.whitespots.appsecplugin.models.TriageStatus

class HighlightDebugAction : AnAction("Test AppSec Highlighting") {

    companion object {
        private val LOG = Logger.getInstance(HighlightDebugAction::class.java)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        LOG.info("Debug action triggered for file: ${virtualFile.path}")

        val caretModel = editor.caretModel
        val currentLine = caretModel.logicalPosition.line + 1 // Convert to 1-based

        val testFinding = Finding(
            id = 999L,
            name = "Test Security Finding",
            description = "This is a test finding to verify highlighting functionality",
            filePath = getRelativeFilePath(project, virtualFile),
            line = currentLine,
            severity = Severity.HIGH,
            triageStatus = TriageStatus.VERIFIED,
            product = 1L,
            dateCreated = null,
            findingUrl = null,
            tags = listOf("test")
        )

        LOG.info("Created test finding for line $currentLine with path: ${testFinding.filePath}")

        val highlightService = FindingHighlightService.getInstance(project)

        val fileEditorManager = FileEditorManager.getInstance(project)
        val fileEditor = fileEditorManager.getSelectedEditor(virtualFile)

        if (fileEditor is TextEditor) {
            LOG.info("Applying test highlight")
            highlightService.highlightFindingsInEditor(fileEditor.editor, virtualFile, listOf(testFinding))
        } else {
            LOG.warn("Not a text editor: ${fileEditor?.javaClass?.simpleName}")
        }
    }

    private fun getRelativeFilePath(
        project: Project,
        virtualFile: VirtualFile
    ): String? {
        val projectBaseVirtualFile = project.baseDir ?: return null
        val relativePath = VfsUtil.getRelativePath(virtualFile, projectBaseVirtualFile)
        return relativePath
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = project != null && editor != null && virtualFile != null
    }
}
