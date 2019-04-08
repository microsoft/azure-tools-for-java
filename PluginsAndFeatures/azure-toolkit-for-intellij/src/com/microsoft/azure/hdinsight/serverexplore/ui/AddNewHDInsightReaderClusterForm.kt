/*
 * Copyright (c) Microsoft Corporation
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

package com.microsoft.azure.hdinsight.serverexplore.ui

import com.intellij.openapi.project.Project
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule
import org.apache.commons.lang3.StringUtils
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

class AddNewHDInsightReaderClusterForm(val project: Project, val module: HDInsightRootModule, val clusterName: String) :
    AddNewClusterForm(project, module) {
    init {
        clusterComboBox.isEnabled = false
        authComboBox.isEnabled = false

        window.addWindowListener(object : WindowAdapter() {
            override fun windowOpened(e: WindowEvent?) {
                clusterNameOrUrlField.text = clusterName
                clusterNameOrUrlField.isEditable = false
                super.windowOpened(e)
            }
        })
    }

    override fun validateBasicInputs() {
        validationErrorMessageField.text =
            if (StringUtils.isBlank(userNameField.text) || StringUtils.isBlank(passwordField.text)) {
                "Username and password can't be empty in Basic Authentication"
            } else {
                null
            }

        okAction.isEnabled = StringUtils.isEmpty(validationErrorMessageField.text)
    }
}