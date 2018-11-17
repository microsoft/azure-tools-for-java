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

package com.microsoft.intellij.component

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.Label
import com.intellij.util.ui.UIUtil
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.intellij.component.extension.createDefaultRenderer
import com.microsoft.intellij.component.extension.getSelectedValue
import com.microsoft.intellij.helpers.validator.SubscriptionValidator
import net.miginfocom.swing.MigLayout
import javax.swing.JPanel

class AzureSubscriptionsSelector : JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]")), AzureComponent {

    companion object {
        private const val EMPTY_SUBSCRIPTION_MESSAGE = "No existing Azure Subscriptions"
    }

    private val lblSubscription = Label("Subscription", UIUtil.ComponentStyle.REGULAR, UIUtil.FontColor.NORMAL, false)
    val cbSubscription = ComboBox<Subscription>()

    var lastSelectedSubscriptionId = ""
    var listenerAction: () -> Unit = {}

    init {
        initSubscriptionComboBox()

        add(lblSubscription)
        add(cbSubscription, "growx")
    }

    override fun validateComponent(): ValidationInfo? {
        val status = SubscriptionValidator.validateSubscription(cbSubscription.getSelectedValue())
        if (status.isValid) return null
        return ValidationInfo(status.errors.first(), cbSubscription)
    }

    private fun initSubscriptionComboBox() {
        cbSubscription.renderer = cbSubscription.createDefaultRenderer(EMPTY_SUBSCRIPTION_MESSAGE) { it.displayName() }

        cbSubscription.addActionListener {
            val subscription = cbSubscription.getSelectedValue() ?: return@addActionListener
            val selectedSid = subscription.subscriptionId()
            if (lastSelectedSubscriptionId == selectedSid) return@addActionListener
            lastSelectedSubscriptionId = selectedSid

            listenerAction()
        }
    }
}