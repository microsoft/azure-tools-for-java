/**
 * Copyright (c) 2019 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.configuration.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.plugins.newui.TwoLineProgressIndicator
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.OpaquePanel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.microsoft.intellij.configuration.AzureRiderSettings
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsManager
import org.jetbrains.plugins.azure.functions.projectTemplating.FunctionsCoreToolsTemplateManager
import java.awt.CardLayout
import java.io.File
import javax.swing.JLabel
import javax.swing.JPanel

class AzureFunctionsConfigurationPanel: AzureRiderAbstractConfigurablePanel {

    companion object {
        const val DISPLAY_NAME = "Functions"

        private val DEFAULT_TOP_INSET = JBUI.scale(8)

        private const val PATH_TO_CORE_TOOLS = "Azure Functions Core Tools path:"
        private const val CURRENT_VERSION = "Current version:"
        private const val LATEST_VERSION = "Latest available version:"
        private const val UNKNOWN = "<unknown>"

        private const val PATH_TO_CORE_TOOLS_DESCRIPTION = "Path to Azure Functions Core Tools"
        private const val INSTALL_LATEST = "Download latest version..."
        private const val ALLOW_PRERELEASE = "Allow versions marked as pre-release"

        private const val CARD_BUTTON = "button"
        private const val CARD_PROGRESS = "progress"
    }

    private val properties = PropertiesComponent.getInstance()

    private val coreToolsPathField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
                "",
                PATH_TO_CORE_TOOLS_DESCRIPTION,
                null,
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        )
    }

    private val currentVersionLabel = JLabel(UNKNOWN)
    private val latestVersionLabel = JLabel(UNKNOWN)
    private val allowPrereleaseToggle = JBCheckBox(ALLOW_PRERELEASE)

    private val releaseInfoLink = HyperlinkLabel().apply {
        setIcon(AllIcons.General.Information)
        setHyperlinkText("Release information is retrieved from the ", "Azure Functions Core Tools GitHub repository", ".")
        setHyperlinkTarget("https://github.com/Azure/azure-functions-core-tools/releases")
    }

    private val installButton = LinkLabel<Any>(INSTALL_LATEST, null) { _, _ -> installLatestCoreTools() }
            .apply {
                isEnabled = false
            }

    private val wrapperLayout = CardLayout()
    private val installActionPanel = OpaquePanel(wrapperLayout)
            .apply {
                add(installButton, CARD_BUTTON)
                add(TwoLineProgressIndicator().component, CARD_PROGRESS)
            }

    init {
        coreToolsPathField.text = properties.getValue(
                AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_PATH,
                "")

        allowPrereleaseToggle.isSelected = properties.getBoolean(
                AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_ALLOW_PRERELEASE,
                false)

        allowPrereleaseToggle.addItemListener {
            updateVersionLabels()
        }

        updateVersionLabels()
    }

    private fun installLatestCoreTools()  {
        val installIndicator = TwoLineProgressIndicator()
        installIndicator.setCancelRunnable {
            if (installIndicator.isRunning) installIndicator.stop()
            wrapperLayout.show(installActionPanel, CARD_BUTTON)
        }

        installActionPanel.add(installIndicator.component, CARD_PROGRESS)
        wrapperLayout.show(installActionPanel, CARD_PROGRESS)

        FunctionsCoreToolsManager.downloadLatestRelease(allowPrereleaseToggle.isSelected, installIndicator) {
            FunctionsCoreToolsTemplateManager.tryReload()

            UIUtil.invokeAndWaitIfNeeded(Runnable {
                coreToolsPathField.text = it
                installButton.isEnabled = false
                wrapperLayout.show(installActionPanel, CARD_BUTTON)
            })

            updateVersionLabels()
        }
    }

    private fun updateVersionLabels() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val local = FunctionsCoreToolsManager.determineVersion(coreToolsPathField.text)
            val remote = FunctionsCoreToolsManager.determineLatestRemote(allowPrereleaseToggle.isSelected)

            UIUtil.invokeAndWaitIfNeeded(Runnable {
                currentVersionLabel.text = local?.version ?: UNKNOWN
                latestVersionLabel.text = remote?.version ?: UNKNOWN

                installButton.isEnabled = local == null || remote != null && local < remote
            })
        }
    }

    override val panel = FormBuilder
            .createFormBuilder()
            .addLabeledComponent(PATH_TO_CORE_TOOLS, coreToolsPathField)
            .addLabeledComponent(CURRENT_VERSION, currentVersionLabel)
            .addLabeledComponent(LATEST_VERSION, latestVersionLabel, DEFAULT_TOP_INSET)
            .addComponentToRightColumn(releaseInfoLink, DEFAULT_TOP_INSET)
            .addComponentToRightColumn(allowPrereleaseToggle, DEFAULT_TOP_INSET)
            .addComponentToRightColumn(installActionPanel)
            .addComponentFillVertically(JPanel(), 0)
            .panel

    override val displayName = DISPLAY_NAME

    override fun doOKAction() {
        if (coreToolsPathField.text != "" && File(coreToolsPathField.text).exists()) {
            properties.setValue(
                    AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_PATH,
                    coreToolsPathField.text)
        }

        properties.setValue(
                AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_ALLOW_PRERELEASE,
                allowPrereleaseToggle.isSelected)
    }
}