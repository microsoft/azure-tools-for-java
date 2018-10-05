package com.microsoft.intellij.runner.webapp.webappconfig

import com.intellij.openapi.project.Project
import com.jetbrains.rider.protocol.IPermittedModalities
import com.jetbrains.rider.util.idea.lifetime
import com.microsoft.intellij.runner.AzureRiderSettingPanel
import com.microsoft.intellij.runner.AzureRiderSettingsEditor
import com.microsoft.intellij.runner.webapp.webappconfig.ui.RiderWebAppSettingPanel
import javax.swing.JComponent

class RiderWebAppSettingEditor(project: Project,
                               webAppConfiguration: RiderWebAppConfiguration)
    : AzureRiderSettingsEditor<RiderWebAppConfiguration>() {

    private val lifetimeDef = project.lifetime.createNestedDef()

    private val myPanel: RiderWebAppSettingPanel = RiderWebAppSettingPanel(lifetimeDef.lifetime, project, webAppConfiguration)

    override val panel: AzureRiderSettingPanel<RiderWebAppConfiguration>
        get() = myPanel

    override fun createEditor(): JComponent {
        IPermittedModalities.getInstance().allowPumpProtocolUnderCurrentModality()
        return super.createEditor()
    }

    override fun disposeEditor() {
        super.disposeEditor()
        lifetimeDef.terminate()
    }
}
