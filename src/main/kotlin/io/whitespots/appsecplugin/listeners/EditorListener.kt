package io.whitespots.appsecplugin.listeners

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import io.whitespots.appsecplugin.highlighting.FindingHighlightService

class EditorListener(private val project: Project) : FileEditorManagerListener {

    companion object {
        private val LOG = Logger.getInstance(EditorListener::class.java)
    }

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        LOG.debug("File opened: ${file.name}")

        val highlightService = FindingHighlightService.getInstance(project)
        val findings = highlightService.getFindingsForFile(file)

        if (findings.isNotEmpty()) {
            LOG.info("Highlighting ${findings.size} findings in opened file: ${file.name}")

            ToolWindowManager.getInstance(project).invokeLater {
                val fileEditor = source.getSelectedEditor(file)
                LOG.debug("Selected editor type: ${fileEditor?.javaClass?.simpleName}")
                if (fileEditor is TextEditor) {
                    highlightService.highlightFindingsInEditor(fileEditor.editor, file, findings)
                } else {
                    LOG.warn("Editor is not a TextEditor: ${fileEditor?.javaClass?.simpleName}")
                }
            }
        } else {
            LOG.debug("No findings for file: ${file.name}")
        }
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        LOG.debug("File closed: ${file.path}")

        val highlightService = FindingHighlightService.getInstance(project)
        highlightService.clearHighlightsForFile(file)
    }
}
