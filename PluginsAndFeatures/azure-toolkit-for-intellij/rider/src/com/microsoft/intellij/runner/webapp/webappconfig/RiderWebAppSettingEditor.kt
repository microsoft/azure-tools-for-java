package com.microsoft.intellij.runner.webapp.webappconfig

import com.intellij.openapi.project.Project
import com.microsoft.intellij.runner.AzureRiderSettingPanel
import com.microsoft.intellij.runner.AzureRiderSettingsEditor
import com.microsoft.intellij.runner.webapp.webappconfig.ui.RiderWebAppSettingPanel

class RiderWebAppSettingEditor(project: Project,
                               webAppConfiguration: RiderWebAppConfiguration)
    : AzureRiderSettingsEditor<RiderWebAppConfiguration>() {

    private val myPanel: RiderWebAppSettingPanel = RiderWebAppSettingPanel(project, webAppConfiguration)

    override val panel: AzureRiderSettingPanel<RiderWebAppConfiguration>
        get() = this.myPanel
}
