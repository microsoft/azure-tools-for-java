/**
 * Copyright (c) 2018-2019 JetBrains s.r.o.
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

package com.microsoft.intellij.component.appservice

import com.intellij.util.ui.JBUI
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.intellij.component.AzureComponent
import net.miginfocom.swing.MigLayout
import java.awt.event.ActionListener
import javax.swing.JPanel
import javax.swing.JRadioButton

class OperatingSystemSelector :
        JPanel(MigLayout("novisualpadding, ins 0")),
        AzureComponent {

    companion object {
        private val indentionSize = JBUI.scale(17)
    }

    val isWindows: Boolean
        get() = rdoOperatingSystemWindows.isSelected

    val deployOperatingSystem: OperatingSystem
        get() = if (rdoOperatingSystemWindows.isSelected) OperatingSystem.WINDOWS else OperatingSystem.LINUX

    val rdoOperatingSystemWindows = JRadioButton("Windows", true)
    val rdoOperatingSystemLinux = JRadioButton("Linux")

    init {
        initOperatingSystemButtonGroup()

        add(rdoOperatingSystemWindows)
        add(rdoOperatingSystemLinux, "gapbefore $indentionSize")
    }

    private fun initOperatingSystemButtonGroup() {
        initButtonsGroup(hashMapOf(
                rdoOperatingSystemWindows to ActionListener { },
                rdoOperatingSystemLinux to ActionListener { }))
    }
}