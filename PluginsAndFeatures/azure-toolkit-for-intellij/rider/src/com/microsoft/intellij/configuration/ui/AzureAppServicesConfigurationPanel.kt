/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

import com.intellij.ide.util.PropertiesComponent
import com.microsoft.intellij.configuration.AzureRiderSettings
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import javax.swing.JCheckBox
import javax.swing.JPanel

class AzureAppServicesConfigurationPanel : AzureRiderAbstractConfigurablePanel {

    private val properties = PropertiesComponent.getInstance()

    private val pnlRoot = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1"))
    private val checkBoxOpenInBrowser = JCheckBox(message("settings.app_services.open_in_browser_after_publish"))

    init {
        initOpenInBrowserCheckBox()

        pnlRoot.apply {
            add(checkBoxOpenInBrowser)
        }
    }

    override val panel: JPanel = pnlRoot

    override val displayName: String = message("settings.app_services.name")

    override fun doOKAction() =
            properties.setValue(
                    AzureRiderSettings.PROPERTY_WEB_APP_OPEN_IN_BROWSER_NAME,
                    checkBoxOpenInBrowser.isSelected,
                    AzureRiderSettings.OPEN_IN_BROWSER_AFTER_PUBLISH_DEFAULT_VALUE)

    private fun initOpenInBrowserCheckBox() {
        val currentValue = properties.getBoolean(
                AzureRiderSettings.PROPERTY_WEB_APP_OPEN_IN_BROWSER_NAME,
                AzureRiderSettings.OPEN_IN_BROWSER_AFTER_PUBLISH_DEFAULT_VALUE)

        checkBoxOpenInBrowser.isSelected = currentValue
    }
}