/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.identity.managed

import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.UIUtil
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail
import org.jetbrains.plugins.azure.RiderAzureBundle
import java.awt.Component
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

class SubscriptionDetailRenderer : ListCellRenderer<SubscriptionDetail> {
    private val titleLabel = SimpleColoredComponent()
    private val subscriptionIdLabel = SimpleColoredComponent()
    private val tenantIdLabel = SimpleColoredComponent()

    private val component = JPanel()
            .apply {
                background = UIUtil.getListBackground()
                border = BorderFactory.createEmptyBorder(0, JBUIScale.scale(UIUtil.getListCellHPadding()), 0, JBUIScale.scale(UIUtil.getListCellHPadding()))

                layout = GridLayout(3, 1)
                add(titleLabel)
                add(subscriptionIdLabel)
                add(tenantIdLabel)
            }

    override fun getListCellRendererComponent(list: JList<out SubscriptionDetail>?, value: SubscriptionDetail?, index: Int, selected: Boolean, hasFocus: Boolean): Component {
        titleLabel.clear()
        subscriptionIdLabel.clear()
        tenantIdLabel.clear()

        if (value != null) {
            titleLabel.append(value.subscriptionName)
            subscriptionIdLabel.append(RiderAzureBundle.message("settings.managedidentity.subscription_id", value.subscriptionId), SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES)
            tenantIdLabel.append(RiderAzureBundle.message("settings.managedidentity.tenant_id", value.tenantId), SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES)
        }

        return component
    }
}