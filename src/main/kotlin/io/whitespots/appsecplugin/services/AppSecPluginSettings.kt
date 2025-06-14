package io.whitespots.appsecplugin.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import io.whitespots.appsecplugin.settings.AppSecPluginSettingsState

@State(
    name = "io.whitespots.appsecplugin.settings.AppSecPluginSettingsState",
    storages = [Storage("WhitespotsAppSecPlugin.xml")]
)
class AppSecPluginSettings : PersistentStateComponent<AppSecPluginSettingsState> {

    private var internalState = AppSecPluginSettingsState()

    companion object {
        val instance: AppSecPluginSettings
            get() = service()
    }

    override fun getState(): AppSecPluginSettingsState = internalState

    override fun loadState(state: AppSecPluginSettingsState) {
        XmlSerializerUtil.copyBean(state, this.internalState)
    }
}