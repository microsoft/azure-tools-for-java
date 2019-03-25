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

import com.intellij.ide.BrowserUtil
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.FormBuilder
import com.microsoft.intellij.configuration.AzureRiderSettings
import java.io.File
import javax.swing.JPanel

class AzureFunctionsConfigurationPanel: AzureRiderAbstractConfigurablePanel {

    companion object {
        private const val DISPLAY_NAME = "Functions"
    }

    private val properties = PropertiesComponent.getInstance()

    private val coreToolsPathField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
                "",
                "Path to Azure Functions Core Tools",
                null,
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        )
    }

    private val downloadCoreToolsLabel = LinkLabel<Any>("Download latest version from GitHub...", null) {
        _, _ -> BrowserUtil.open("https://github.com/Azure/azure-functions-core-tools/releases")
    }

    init {
        coreToolsPathField.text = properties.getValue(
                AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_PATH,
                "")
    }

    override val panel = FormBuilder
            .createFormBuilder()
            .addLabeledComponent("Azure Functions Core Tools path:", coreToolsPathField)
            .addComponentToRightColumn(downloadCoreToolsLabel)
            .addComponentFillVertically(JPanel(), 0)
            .panel

    override val displayName = DISPLAY_NAME

    override fun doOKAction() {
        if (coreToolsPathField.text != "" && File(coreToolsPathField.text).exists()) {
            properties.setValue(
                    AzureRiderSettings.PROPERTY_FUNCTIONS_CORETOOLS_PATH,
                    coreToolsPathField.text)
        }
    }
}
