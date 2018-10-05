package com.microsoft.intellij.configuration

import com.intellij.application.options.OptionsContainingConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.microsoft.intellij.configuration.ui.AzureAppServicesConfigurationPanel

class AzureRiderConfigurable :
        SearchableConfigurable.Parent.Abstract(), OptionsContainingConfigurable {

    companion object {
        private const val AZURE_CONFIGURATION_ID = "com.intellij"
        private const val AZURE_CONFIGURATION_NAME = "Azure"
    }

    private var myPanels = listOf<Configurable>()

    override fun getId() = AZURE_CONFIGURATION_ID

    override fun getDisplayName() = AZURE_CONFIGURATION_NAME

    override fun buildConfigurables(): Array<Configurable> {
        val panels = listOf<Configurable>(AzureRiderAbstractConfigurable(AzureAppServicesConfigurationPanel()))
        myPanels = panels
        return panels.toTypedArray()
    }

    override fun processListOptions() = hashSetOf<String>()
}