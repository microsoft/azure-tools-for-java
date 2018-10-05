package com.microsoft.intellij.configuration

import com.intellij.application.options.OptionsContainingConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.microsoft.intellij.configuration.ui.AzureRiderAbstractConfigurablePanel
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class AzureRiderAbstractConfigurable(private val panel: AzureRiderAbstractConfigurablePanel) :
        SearchableConfigurable, Configurable.NoScroll, OptionsContainingConfigurable {

    @Nls
    override fun getDisplayName(): String? {
        return panel.displayName
    }

    override fun getHelpTopic(): String? {
        return null
    }

    override fun processListOptions(): Set<String>? {
        return null
    }

    override fun createComponent(): JComponent? {
        return panel.panel
    }

    override fun apply() {
        panel.doOKAction()
    }

    override fun isModified() = true

    override fun getId(): String {
        return "preferences.sourceCode.$displayName"
    }

    override fun enableSearch(option: String?): Runnable? {
        return null
    }
}
