package io.whitespots.appsecplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent
import javax.swing.JPasswordField
import javax.swing.JTextField

class LoginDialog(project: Project?) : DialogWrapper(project) {
    private val usernameField = JTextField(30)
    private val passwordField = JPasswordField(30)

    var username: String
        get() = usernameField.text.trim()
        set(value) { usernameField.text = value }

    var password: String
        get() = String(passwordField.password)
        set(value) { passwordField.text = value }

    init {
        title = "Login to Whitespots Portal"
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Username:") {
            cell(usernameField)
                .focused()
                .comment("Enter your portal username")
        }
        row("Password:") {
            cell(passwordField)
                .comment("Enter your portal password")
        }
        row {
            comment("Your credentials will be used to obtain an authentication token")
        }
    }

    override fun doValidate(): ValidationInfo? {
        return when {
            username.isEmpty() -> ValidationInfo("Username cannot be empty", usernameField)
            password.isEmpty() -> ValidationInfo("Password cannot be empty", passwordField)
            else -> null
        }
    }

    override fun getPreferredFocusedComponent(): JComponent = usernameField
}
