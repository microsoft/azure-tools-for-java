package com.microsoft.intellij.configuration.ui

import com.intellij.ide.util.PropertiesComponent
import com.microsoft.intellij.configuration.AzureRiderSettings
import javax.swing.*

class AzureAppServicesConfigurationPanel : AzureRiderAbstractConfigurablePanel {

    companion object {
        private const val DISPLAY_NAME = "App Services"
    }

    private val properties = PropertiesComponent.getInstance()

    private lateinit var pnlRoot: JPanel
    private lateinit var pnlWebAppPublishConfiguration: JPanel
    lateinit var checkBoxOpenInBrowser: JCheckBox

    init {
        val currentValue = properties.getBoolean(
                AzureRiderSettings.PROPERTY_WEB_APP_OPEN_IN_BROWSER_NAME,
                AzureRiderSettings.openInBrowserDefaultValue)

        checkBoxOpenInBrowser.isSelected = currentValue
    }

    override val panel = pnlRoot
    override val displayName = DISPLAY_NAME

    override fun doOKAction() =
            properties.setValue(
                    AzureRiderSettings.PROPERTY_WEB_APP_OPEN_IN_BROWSER_NAME,
                    checkBoxOpenInBrowser.isSelected,
                    AzureRiderSettings.openInBrowserDefaultValue)
}
