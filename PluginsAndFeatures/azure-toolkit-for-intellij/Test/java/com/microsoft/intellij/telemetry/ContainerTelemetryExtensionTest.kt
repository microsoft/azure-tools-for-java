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

package com.microsoft.intellij.telemetry

import com.intellij.icons.AllIcons
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.ui.ComboboxWithBrowseButton
import com.intellij.ui.InplaceButton
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.ui.table.JBTable
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.util.execution.ParametersListUtil
import com.microsoft.azure.cosmosspark.common.JXHyperLinkWithUri
import com.microsoft.azure.hdinsight.spark.common.SubmissionTableModel
import com.microsoft.intellij.forms.dsl.panel
import com.microsoft.intellij.ui.components.JsonEnvPropertiesField
import org.junit.Test
import java.awt.BorderLayout
import java.awt.Container
import java.awt.Dimension
import javax.swing.*

class FormBuilderPanel {
    private val storageAccountField = JTextField().apply { name = "storageAccountField" }
    private val storageKeyField = ExpandableTextField().apply { name = "storageKeyField" }
    private val localArtifactTextField = TextFieldWithBrowseButton().apply {
        textField.name = "localArtifactTextFieldText"
        button.name = "localArtifactTextFieldButton"
    }
    private val storageContainerUI = ComboboxWithBrowseButton(JComboBox(arrayOf("item1", "item2", "item3"))).apply {
        comboBox.name = "storageContainerUICombo"
        button.name = "storageContainerUIButton"
    }
    private val linkClusterHyperLink = JXHyperLinkWithUri().apply { text = "Link the cluster"; name = "linkClusterLink" }
    private val extendedPropertiesField = JsonEnvPropertiesField().apply { name = "extendedPropertiesField" }
    private val enableRemoteDebugCheckBox = JCheckBox("Enable remote debug", true).apply { name = "enableRemoteDebugCheckBox" }
    private val helpButton = InplaceButton(IconButton("Help", AllIcons.Actions.Help)) {}.apply { name = "remoteDebugHelpButton" }
    private val ideaArtifactPrompt = JRadioButton("Artifact from IntelliJ project:", true).apply {
        name = "ideaArtifactRadioButton"
        isSelected = false
    }
    private val referencedJarsTextField = TextFieldWithBrowseButton(
            ExpandableTextField(ParametersListUtil.COLON_LINE_PARSER, ParametersListUtil.COLON_LINE_JOINER)).apply {
        textField.name = "referencedJarsTextFieldText"
        button.name = "referencedJarsTextFieldButton"
    }
    private val jobConfigurationTable = JBTable(SubmissionTableModel()).apply {
        name = "jobConfigurationTable"
        preferredScrollableViewportSize = Dimension(580, 100)

        surrendersFocusOnKeystroke = true
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        columnSelectionAllowed = true
        fillsViewportHeight = true
    }
    private val jobConfTableScrollPane = JBScrollPane(jobConfigurationTable).apply {
        minimumSize = jobConfigurationTable.preferredScrollableViewportSize
    }

    val component: JComponent by lazy {
        val formBuilder = panel {
            columnTemplate {
                col {
                    anchor = GridConstraints.ANCHOR_WEST
                }
                col {
                    anchor = GridConstraints.ANCHOR_WEST
                    hSizePolicy = GridConstraints.SIZEPOLICY_WANT_GROW
                    fill = GridConstraints.FILL_HORIZONTAL
                }
            }
            row {
                c(JLabel("ideaArtifactPrompt:"));   c(ideaArtifactPrompt)
            }

            row {
                c(JLabel("helpButton:"));   c(helpButton)
            }
            row {
                c(JLabel("storageAccountField:"));  c(storageAccountField)
            }
            row {
                c(JLabel("storageKeyField:"));  c(storageKeyField)
            }
            row {
                c(JLabel("storageContainerUI:"));   c(storageContainerUI)
            }
            row {
                c(JLabel("localArtifactTextField:"));   c(localArtifactTextField)
            }
            row {
                c(JLabel("linkClusterHyperLink:")); c(linkClusterHyperLink)
            }
            row {
                c(JLabel("extendedPropertiesField:"));  c(extendedPropertiesField)
            }
            row {
                c(JLabel("enableRemoteDebugCheckBox:"));    c(enableRemoteDebugCheckBox)
            }
            row {
                c(JLabel("referencedJarsTextField:"));  c(referencedJarsTextField)
            }
            row {
                c(JLabel("jobConfigurationTable"));  c(jobConfTableScrollPane)
            }
        }

        formBuilder.buildPanel()
    }
}

class IdeaTelemetryUtilsTest : LightProjectDescriptor() {
    override fun getSdk(): Sdk? {
        return IdeaTestUtil.getMockJdk18()
    }

    private fun addComponentsToPane(pane: Container) {
        pane.add(FormBuilderPanel().component, BorderLayout.NORTH)
        pane.add(JSeparator(), BorderLayout.CENTER)
    }

    @Test
    fun testTelemetryDialog() {
        // Create and set up the window
        val frame = JFrame("IdeaTelemetryUtilsTest").apply {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            setSize(1000, 1000)
        }

        // Set up the content pane
        addComponentsToPane(frame.contentPane)

        frame.addTelemetryListener("TestServiceName")

        // Display the window.
        frame.pack()
        frame.isVisible = true

        // FIXME: Need to figure out why we need this while(true) {} to prevent test code from stopping
        while (true) {
        }
    }
}