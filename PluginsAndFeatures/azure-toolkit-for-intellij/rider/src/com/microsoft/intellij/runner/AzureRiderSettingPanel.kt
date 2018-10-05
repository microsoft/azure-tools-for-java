package com.microsoft.intellij.runner

import com.intellij.openapi.project.Project
import javax.swing.JPanel

abstract class AzureRiderSettingPanel<T : AzureRunConfigurationBase<*>>(protected val project: Project) {

    abstract val panelName: String

    abstract val mainPanel: JPanel

    fun reset(configuration: T) = resetFromConfig(configuration)

    protected abstract fun resetFromConfig(configuration: T)

    abstract fun apply(configuration: T)

    abstract fun disposeEditor()
}
