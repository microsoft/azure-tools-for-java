package com.microsoft.intellij.runner

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import javax.swing.JComponent

abstract class AzureRiderSettingsEditor<T : AzureRunConfigurationBase<*>> : SettingsEditor<T>() {

    protected abstract val panel: AzureRiderSettingPanel<T>

    @Throws(ConfigurationException::class)
    override fun applyEditorTo(configuration: T) {
        panel.apply(configuration)
        configuration.checkConfiguration()
    }

    override fun resetEditorFrom(configuration: T) {
        configuration.isFirstTimeCreated = false
        panel.reset(configuration)
    }

    override fun createEditor(): JComponent {
        return panel.mainPanel
    }

    override fun disposeEditor() {
        panel.disposeEditor()
        super.disposeEditor()
    }
}
