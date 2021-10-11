/**
 * Copyright (c) 2018-2021 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.configuration.ui

import com.intellij.ide.util.PropertiesComponent
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.layout.panel
import com.intellij.util.ui.FormBuilder
import com.microsoft.intellij.configuration.AzureRiderSettings
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import javax.swing.JCheckBox
import javax.swing.JPanel

class AzureAppServicesConfigurationPanel : AzureRiderAbstractConfigurablePanel {

    private val properties = PropertiesComponent.getInstance()

    private val checkBoxOpenInBrowser = JCheckBox(message("settings.app_services.open_in_browser_after_publish"))
    private val collectArtifactsTimeoutMinutesSpinner = JBIntSpinner(AzureRiderSettings.VALUE_COLLECT_ARTIFACTS_TIMEOUT_MINUTES_DEFAULT, 1, 60)

    init {
        checkBoxOpenInBrowser.isSelected = properties.getBoolean(
            AzureRiderSettings.PROPERTY_WEB_APP_OPEN_IN_BROWSER_NAME,
            AzureRiderSettings.OPEN_IN_BROWSER_AFTER_PUBLISH_DEFAULT_VALUE)

        collectArtifactsTimeoutMinutesSpinner.number = properties.getInt(
                AzureRiderSettings.PROPERTY_COLLECT_ARTIFACTS_TIMEOUT_MINUTES_NAME,
                AzureRiderSettings.VALUE_COLLECT_ARTIFACTS_TIMEOUT_MINUTES_DEFAULT)
    }

    override val panel: JPanel =
            panel {
                row {
                    val coreToolsPanel = FormBuilder
                            .createFormBuilder()
                            .addComponent(checkBoxOpenInBrowser)
                            .addSeparator()
                            .addLabeledComponent(message("settings.app_services.deploy_collect_artifacts_timeout"), collectArtifactsTimeoutMinutesSpinner)
                            .panel
                    component(coreToolsPanel)
                }
            }

    override val displayName: String = message("settings.app_services.name")

    override fun doOKAction() {
        properties.setValue(
                AzureRiderSettings.PROPERTY_WEB_APP_OPEN_IN_BROWSER_NAME,
                checkBoxOpenInBrowser.isSelected,
                AzureRiderSettings.OPEN_IN_BROWSER_AFTER_PUBLISH_DEFAULT_VALUE)

        properties.setValue(
                AzureRiderSettings.PROPERTY_COLLECT_ARTIFACTS_TIMEOUT_MINUTES_NAME,
                collectArtifactsTimeoutMinutesSpinner.number,
                AzureRiderSettings.VALUE_COLLECT_ARTIFACTS_TIMEOUT_MINUTES_DEFAULT)
    }
}