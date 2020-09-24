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

package com.microsoft.intellij.forms.dsl

import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridConstraints.*
import com.intellij.uiDesigner.core.GridLayoutManager
import java.awt.Component
import javax.swing.JPanel

fun panel(block: FormBuilder.() -> Unit): FormBuilder = FormBuilder().apply(block)

data class ColComponent(var component: Component? = null, var constraints: GridConstraints? = null)

class ColumnTemplate(val columnConstraints: MutableList<GridConstraints> = mutableListOf()) {
    fun col(block: GridConstraints.() -> Unit) {
        synchronized(columnConstraints) {
            columnConstraints.add(GridConstraints().apply(block).apply { column = columnConstraints.size })
        }
    }
}

class FormBuilder(var columnTemplate: ColumnTemplate? = null) {
    private val rows = mutableListOf<Row>()

    val allComponentConstraints: List<ColComponent>
        get() = synchronized(rows) {
            rows.flatMap { it.comps }
                    .filter { it.component != null }
        }
    val rowSize: Int
        get() = rows.size

    val colSize: Int
        get() = (allComponentConstraints.asSequence().map { it.constraints?.column ?: 0 }
                .maxOrNull() ?: -1 ) + 1

    inner class Row(val index: Int, val comps: MutableList<ColComponent> = mutableListOf()) {
        fun c(component: Component? = null, block: GridConstraints.() -> Unit = {}) {
            synchronized(comps) {
                val colComponent = ColComponent(
                        component,
                        (columnTemplate?.columnConstraints?.getOrNull(comps.size)?.clone() as? GridConstraints ?:
                                GridConstraints().apply {
                                    column = comps.size
                                    anchor = ANCHOR_WEST
                                    hSizePolicy = SIZEPOLICY_WANT_GROW
                                    fill = FILL_BOTH
                                })
                                .apply(block)
                                .apply { row = index })

                comps.add(colComponent)
            }
        }
    }

    fun columnTemplate(block: ColumnTemplate.() -> Unit) {
        columnTemplate = ColumnTemplate().apply(block)
    }

    fun row(block: Row.() -> Unit) {
        synchronized(rows) {
            rows.add(Row(rows.size).apply(block))
        }
    }

    fun createGridLayoutManager() = GridLayoutManager(rowSize, colSize)

    fun buildPanel(): JPanel = JPanel().apply {
        layout = createGridLayoutManager()

        allComponentConstraints.forEach { (component, gridConstrains) -> add(component, gridConstrains) }
        isFocusable = false
    }
}

/*
The Form DSL can be used as following codes:

    import com.intellij.uiDesigner.core.GridConstraints.*

    private val formBuilder = panel {
        columnTemplate {
            col {
                anchor = ANCHOR_WEST
            }
            col {
                anchor = ANCHOR_WEST
            }
            col {
                hSizePolicy = SIZEPOLICY_WANT_GROW
                fill = FILL_HORIZONTAL
            }
        }
        row {
            c(enableRemoteDebugCheckBox) { indent = 0 };    c(helpButton);   c(checkSshCertIndicator)
        }
        row {
            c(sshUserNameLabel) { indent = 1 };             c();             c(sshUserNameTextField)
        }
        row {
            c(sshAuthTypeLabel) { indent = 1 }
        }
        row {
            c(sshUsePasswordRadioButton) { indent = 2 };    c();             c(sshPasswordField)
        }
        row {
            c(sshUseKeyFileRadioButton) { indent = 2 };     c();             c(sshKeyFileTextField)
        }
    }

    // Add all components according to the layout plan
    layout = formBuilder.createGridLayoutManager()
    formBuilder.allComponentConstraints.forEach { (component, gridConstrains) -> add(component, gridConstrains) }
*/

