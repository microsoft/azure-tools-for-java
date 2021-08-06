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
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.spark.ui

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.util.TipUIUtil
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.components.JBScrollPane
import com.intellij.uiDesigner.core.GridConstraints.*
import com.intellij.util.ui.JBUI
import com.microsoft.intellij.forms.dsl.panel
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED

class ConsoleViewWithMessageBars(project: Project)
    : ConsoleViewImpl(project, GlobalSearchScope.allScope(project), true, true) {
    companion object {
        private val CONSOLE_VIEW_HTML_PERSISTENT_MESSAGE_ATTRIBUTE = TextAttributesKey.createTextAttributesKey(
                "CONSOLE_VIEW_HTML_PERSISTENT_MESSAGE_ATTRIBUTE")

        val CONSOLE_VIEW_HTML_PERSISTENT_MESSAGE_TYPE = ConsoleViewContentType(
                "CONSOLE_VIEW_HTML_PERSISTENT_MESSAGE", CONSOLE_VIEW_HTML_PERSISTENT_MESSAGE_ATTRIBUTE)
    }

    private val messagesPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    private val scrollMessagesPanel = JBScrollPane(messagesPanel, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER)

    private val mainPanel: JPanel by lazy {
        val formBuilder = panel {
            row {
                c(super.getComponent()) {
                    hSizePolicy = SIZEPOLICY_WANT_GROW or SIZEPOLICY_CAN_SHRINK
                    fill = FILL_BOTH
                    minimumSize = Dimension(480, -1)

                }
            }
            row {
                c(scrollMessagesPanel) {
                    hSizePolicy = SIZEPOLICY_WANT_GROW
                    vSizePolicy = SIZEPOLICY_FIXED
                    fill = FILL_HORIZONTAL
                    maximumSize = Dimension(-1, 30)
                }
            }
        }

        formBuilder.buildPanel()
    }

    override fun getComponent(): JComponent {
        return mainPanel
    }

    override fun print(text: String, contentType: ConsoleViewContentType) {
        when (contentType) {
            CONSOLE_VIEW_HTML_PERSISTENT_MESSAGE_TYPE ->
                printPersistentHtmlMessage(text)
            else ->
                super.print(text, contentType)
        }
    }

    fun printPersistentHtmlMessage(html: String) {
        val messageView = TipUIUtil.createBrowser().apply {
            component.border = JBUI.Borders.empty(8, 12)
            this.text = html
        }

        messagesPanel.add(messageView.component, 0);
    }
}