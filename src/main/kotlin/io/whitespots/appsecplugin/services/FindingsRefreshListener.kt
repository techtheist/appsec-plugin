package io.whitespots.appsecplugin.services

import com.intellij.util.messages.Topic
import java.util.EventListener

interface FindingsRefreshListener : EventListener {
    fun onRefreshRequested()
}

object FindingsRefreshTopics {
    @Topic.ProjectLevel
    val REFRESH_TOPIC = Topic.create("Whitespots AppSec Findings Refresh", FindingsRefreshListener::class.java)
}