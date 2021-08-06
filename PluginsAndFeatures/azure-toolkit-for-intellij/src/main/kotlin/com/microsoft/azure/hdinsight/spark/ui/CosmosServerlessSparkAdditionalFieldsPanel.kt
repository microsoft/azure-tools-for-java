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

import com.intellij.uiDesigner.core.GridConstraints
import com.microsoft.intellij.forms.dsl.panel
import com.microsoft.intellij.ui.components.JsonEnvPropertiesField
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class CosmosServerlessSparkAdditionalFieldsPanel: JPanel() {
    private val sparkEventsTip = "Spark events root path"
    private val extendedPropertiesTip = "special properties that will allow choosing/targeting of features (runtime, gp version etc) on server side"

    private val sparkEventsPrompt = JLabel("Spark events").apply {
        toolTipText = sparkEventsTip
    }
    val sparkEventsDirectoryPrefixField = JLabel("adl://*.azuredatalakestore.net/").apply {
        toolTipText = sparkEventsTip
    }
    val sparkEventsDirectoryField = JTextField("spark-events/").apply {
        name = "sparkEventsDirectoryField"
        toolTipText = sparkEventsTip
    }
    private var sparkEventsDirectory = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
        add(sparkEventsDirectoryPrefixField)
        add(sparkEventsDirectoryField)
    }

    private val extendedPropertiesPrompt = JLabel("Extended properties").apply {
        toolTipText = extendedPropertiesTip
    }
    val extendedPropertiesField = JsonEnvPropertiesField().apply {
        name = "extendedPropertiesField"
        toolTipText = extendedPropertiesTip
    }

    init {
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
                    indent = 8
                }
            }
            row { c(sparkEventsPrompt);         c(sparkEventsDirectory) }
            row { c(extendedPropertiesPrompt);  c(extendedPropertiesField) }
        }

        layout = formBuilder.createGridLayoutManager()
        formBuilder.allComponentConstraints.forEach { (component, gridConstrains) -> add(component, gridConstrains) }
        border = BorderFactory.createEmptyBorder(0, 8, 5, 8)
    }

}