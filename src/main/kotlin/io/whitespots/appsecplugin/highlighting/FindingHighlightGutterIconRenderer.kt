package io.whitespots.appsecplugin.highlighting

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.Key
import io.whitespots.appsecplugin.models.Finding
import io.whitespots.appsecplugin.models.Severity
import io.whitespots.appsecplugin.utils.FindingPopupManager
import javax.swing.Icon

class FindingHighlightGutterIconRenderer(private val finding: Finding) : GutterIconRenderer() {

    companion object {
        val FINDING_KEY = Key.create<Finding>("APPSEC_FINDING")
    }

    override fun getIcon(): Icon {
        return when (finding.severity) {
            Severity.CRITICAL -> AllIcons.General.Error
            Severity.HIGH -> AllIcons.General.Warning
            Severity.MEDIUM -> AllIcons.General.Information
            Severity.LOW -> AllIcons.General.Information
            Severity.INFO -> AllIcons.General.Information
        }
    }

    override fun getTooltipText(): String {
        return "${finding.severity.name} - ${finding.name}\nClick to view details"
    }

    override fun getClickAction(): AnAction {
        return object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                val project = e.project ?: return
                FindingPopupManager.showFindingPopup(project, finding, e.inputEvent)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FindingHighlightGutterIconRenderer) return false
        return finding.id == other.finding.id
    }

    override fun hashCode(): Int {
        return finding.id.hashCode()
    }
}
