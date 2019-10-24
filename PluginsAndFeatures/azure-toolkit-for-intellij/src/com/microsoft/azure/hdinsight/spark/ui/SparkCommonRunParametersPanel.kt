/*
 * Copyright (c) Microsoft Corporation
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
 */

package com.microsoft.azure.hdinsight.spark.ui

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.impl.jar.JarFileSystemImpl
import com.intellij.packaging.impl.elements.ManifestFileUtil
import com.intellij.uiDesigner.core.GridConstraints
import com.microsoft.azure.hdinsight.common.DarkThemeManager
import com.microsoft.intellij.forms.dsl.panel
import com.microsoft.intellij.helpers.ManifestFileUtilsEx
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class SparkCommonRunParametersPanel(private val myProject: Project, private val wholePanel : SparkBatchJobConfigurable) {
    private val mainClassToolTip = "Application's Java/Spark main class"

    private val mainClassTextField: TextFieldWithBrowseButton = TextFieldWithBrowseButton().apply {
        textField.name = "mainClassTextFieldText"
        button.name = "mainClassTextFieldButton"
        toolTipText = mainClassToolTip

        // Button actions
        addActionListener {
            val selected = if (wholePanel.submissionContentPanel.localArtifactPrompt.isSelected)
                ManifestFileUtilsEx(myProject).selectMainClass(
                        JarFileSystemImpl().findFileByPath("${wholePanel.submissionContentPanel.localArtifactTextField.text}!/"))
            else
                ManifestFileUtil.selectMainClass(myProject, text)

            if (selected != null) {
                setText(selected.qualifiedName)
            }
        }
    }

    private val mainClassPrompt: JLabel = JLabel("Main class name").apply {
        toolTipText = mainClassToolTip
        labelFor = mainClassTextField
    }

    private val errorMessageLabel = JLabel("")
            .apply {
                foreground = DarkThemeManager.getInstance().errorMessageColor
                isVisible = true
            }

    private val submissionPanel: JPanel by lazy {
        val formBuilder = panel {
            columnTemplate {
                col {
                    anchor = GridConstraints.ANCHOR_WEST
                    fill = GridConstraints.FILL_NONE
                }
                col {
                    anchor = GridConstraints.ANCHOR_WEST
                    hSizePolicy = GridConstraints.SIZEPOLICY_WANT_GROW
                    fill = GridConstraints.FILL_HORIZONTAL
                }
            }
            row { c(mainClassPrompt); c(mainClassTextField) }
            row { c(); c(errorMessageLabel) }
        }

        formBuilder.buildPanel()
    }

    val component: JComponent
        get() = submissionPanel

    fun setMainClassName(mainClassName: String) {
        mainClassTextField.text = mainClassName;
    }

    fun getMainClassName(): String {
        return mainClassTextField.text
    }

    @Throws(ConfigurationException::class)
    fun validateInputs() {
        if(this.mainClassTextField.text.isNullOrBlank()) {
            this.errorMessageLabel.text = "Main class name could not be null."
        } else {
            this.errorMessageLabel.text = ""
        }
    }
}