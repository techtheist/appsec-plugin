# Whitespots Application Security Portal Jetbrains Plugin

<!-- Plugin description -->
![Build](https://github.com/Whitespots-OU/jetbrains-portal-extension/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/28070-whitespots-application-security-portal.svg)](https://plugins.jetbrains.com/plugin/28070-whitespots-application-security-portal)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/28070-whitespots-application-security-portal.svg)](https://plugins.jetbrains.com/plugin/28070-whitespots-application-security-portal)

**Description**: A Intellij platform plugin to integrate with the [Whitespots Application Security Portal](https://whitespots.io/) and display vulnerabilities related to the current repository.

---

## 🛡️ Overview

The **Whitespots Application Security Portal Plugin** brings vulnerability insights directly into your IntelliJ-based workspace. It connects to your instance of the **Whitespots Application Security Portal** and retrieves detailed information about security issues discovered in your current project repository.

This tool is ideal for developers who want to:

- See real-time vulnerability data in their code editor
- Quickly identify and fix security issues
- Stay in sync with the Portal’s scan results

---

## 🚀 Features

- 🔗 Connect your local project to a Whitespots Portal instance
- 📂 Automatically detect the current repository and fetch related vulnerabilities
- 🛠️ View issue descriptions, severity, file paths, and remediation tips
- 🧭 Navigate from the vulnerability list to affected code locations
- ♻️ Refresh data with a single click

---

## 🔌 Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "whitespots-application-security-portal"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/28070-whitespots-application-security-portal) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/28070-whitespots-application-security-portal/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/Whitespots-OU/jetbrains-portal-extension/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---

## 🛠️ Usage

1. Open a project folder in **Jetbrains IDE** (Check out compatibility in IDE or on the marketplace page)
2. Click on the **Whitespots Security** icon (🛡) in the sidebar to open the plugin panel
3. In the panel, click the **Settings** (⚙️) button
4. Enter the following details in the settings form:
    - **External Portal URL** – the External URL of your Whitespots Application Security Portal instance
    - **Auth API Token** – authorization API token from the Portal
5. Or, you can get the API token by using the **Login** button after setting and saving **External Portal URL** field

Once configured, the extension will automatically retrieve and display vulnerabilities related to the current repository.

---

## 🧪 Development & Contributions

This extension is developed and maintained by **Whitespots**.

For feature requests, feedback, or support inquiries, please contact us at [sales@whitespots.io](mailto:sales@whitespots.io).

---

## 📄 License

MIT License
© Whitespots

<!-- Plugin description end -->
---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
