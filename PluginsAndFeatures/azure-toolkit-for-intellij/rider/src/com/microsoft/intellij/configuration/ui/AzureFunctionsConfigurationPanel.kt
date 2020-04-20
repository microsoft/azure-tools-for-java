/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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
import com.intellij.ui.layout.panel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.microsoft.intellij.configuration.AzureRiderSettings
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsManager
import org.jetbrains.plugins.azure.functions.projectTemplating.FunctionsCoreToolsTemplateManager
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.awt.CardLayout
import java.io.File
import javax.swing.JLabel
import javax.swing.JPanel

class AzureFunctionsConfigurationPanel: AzureRiderAbstractConfigurablePanel {

    companion object {
        private val DEFAULT_TOP_INSET = JBUI.scale(8)

        private const val CARD_BUTTON = "button"
        private const val CARD_PROGRESS = "progress"
    }

    private val unknownLabel: String = "<${message("common.unknown_lower_case")}>"

    private val properties: PropertiesComponent = PropertiesComponent.getInstance()

    private val coreToolsPathField: TextFieldWithBrowseButton =
            TextFieldWithBrowseButton().apply {
                addBrowseFolderListener(
                        "",
                        message("settings.app_services.function_app.core_tools.path_description"),
                        null,
                        FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                        TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
                )
            }

    private val currentVersionLabel = JLabel(unknownLabel)
    private val latestVersionLabel = JLabel(unknownLabel)
    private val allowPrereleaseToggle = JBCheckBox(message("settings.app_services.function_app.core_tools.allow_prerelease"))
    private val checkCoreToolsUpdateToggle = JBCheckBox(message("settings.app_services.function_app.core_tools.check_update"))

    private val releaseInfoLink = HyperlinkLabel().apply {
        setIcon(AllIcons.General.Information)
        setHyperlinkText(
                message("settings.app_services.function_app.core_tools.release_info_before_link_text"),
                message("settings.app_services.function_app.core_tools.release_info_link_text"),
                ".")
        setHyperlinkTarget("https://github.com/Azure/azure-functions-core-tools/releases")
    }

    private val installButton = LinkLabel<Any>(message("settings.app_services.function_app.core_tools.download_latest"), null) { _, _ -> installLatestCoreTools() }
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

        checkCoreToolsUpdateToggle.isSelected =
                properties.getBoolean(AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_CHECK_UPDATES, true)

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
            val remote = FunctionsCoreToolsManager.determineLatestRemote(allowPrerelease = allowPrereleaseToggle.isSelected)

            UIUtil.invokeAndWaitIfNeeded(Runnable {
                currentVersionLabel.text = local?.version ?: unknownLabel
                latestVersionLabel.text = remote?.version ?: unknownLabel

                installButton.isEnabled = local == null || remote != null && local < remote
            })
        }
    }

    override val panel: JPanel =
            panel {
                row {
                    val coreToolsPanel = FormBuilder
                            .createFormBuilder()
                            .addLabeledComponent(message("settings.app_services.function_app.core_tools.path"), coreToolsPathField)
                            .addLabeledComponent(message("settings.app_services.function_app.core_tools.current_version"), currentVersionLabel)
                            .addLabeledComponent(message("settings.app_services.function_app.core_tools.latest_version"), latestVersionLabel, DEFAULT_TOP_INSET)
                            .addComponentToRightColumn(releaseInfoLink, DEFAULT_TOP_INSET)
                            .addComponentToRightColumn(allowPrereleaseToggle, DEFAULT_TOP_INSET)
                            .addComponentToRightColumn(installActionPanel)
                            .panel
                    component(coreToolsPanel)
                }
                row {
                    component(checkCoreToolsUpdateToggle)
                            .comment(message("settings.app_services.function_app.core_tools.check_update_comment"))
                }
            }

    override val displayName: String = message("settings.app_services.function_app.name")

    override fun doOKAction() {
        if (coreToolsPathField.text != "" && File(coreToolsPathField.text).exists()) {
            properties.setValue(
                    AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_PATH,
                    coreToolsPathField.text)
        }

        properties.setValue(
                AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_ALLOW_PRERELEASE,
                allowPrereleaseToggle.isSelected)

        properties.setValue(
                AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_CHECK_UPDATES,
                checkCoreToolsUpdateToggle.isSelected)
    }
}
