package com.microsoft.intellij.configuration.ui

import javax.swing.JComponent

interface AzureRiderAbstractConfigurablePanel {

    val panel: JComponent

    val displayName: String

    fun doOKAction()
}