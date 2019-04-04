/**
 * Copyright (c) 2018 JetBrains s.r.o.
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

package com.microsoft.intellij.configuration

import com.intellij.application.options.OptionsContainingConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.microsoft.intellij.AzureConfigurable.AZURE_CONFIGURABLE_PREFIX
import com.microsoft.intellij.configuration.ui.AzureRiderAbstractConfigurablePanel
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class AzureRiderAbstractConfigurable(private val panel: AzureRiderAbstractConfigurablePanel) :
        SearchableConfigurable, Configurable.NoScroll, OptionsContainingConfigurable {

    @Nls
    override fun getDisplayName(): String? {
        return panel.displayName
    }

    override fun getHelpTopic(): String? {
        return null
    }

    override fun processListOptions(): Set<String> {
        return emptySet()
    }

    override fun createComponent(): JComponent? {
        return panel.panel
    }

    override fun apply() {
        panel.doOKAction()
    }

    override fun isModified() = true

    override fun getId(): String {
        return AZURE_CONFIGURABLE_PREFIX + displayName
    }

    override fun enableSearch(option: String?): Runnable? {
        return null
    }
}
