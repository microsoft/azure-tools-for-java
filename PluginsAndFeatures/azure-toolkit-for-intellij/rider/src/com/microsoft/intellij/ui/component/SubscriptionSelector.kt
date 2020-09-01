/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.ui.component

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.Label
import com.intellij.util.ui.UIUtil
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.intellij.ui.extension.setDefaultRenderer
import com.microsoft.intellij.ui.extension.fillComboBox
import com.microsoft.intellij.ui.extension.getSelectedValue
import com.microsoft.intellij.helpers.validator.SubscriptionValidator
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import javax.swing.JPanel

class SubscriptionSelector :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]")),
        AzureComponent {

    private val lblSubscription = Label(
            message("run_config.publish.form.subscription.label"), UIUtil.ComponentStyle.REGULAR, UIUtil.FontColor.NORMAL, false)
    val cbSubscription = ComboBox<Subscription>()

    var lastSelectedSubscriptionId = ""
    var listenerAction: (Subscription) -> Unit = {}

    init {
        initSubscriptionComboBox()

        add(lblSubscription)
        add(cbSubscription, "growx")

        initComponentValidation()
    }

    override fun validateComponent(): List<ValidationInfo> {
        if (!isEnabled) return emptyList()
        return listOfNotNull(
                SubscriptionValidator.validateSubscription(cbSubscription.getSelectedValue()).toValidationInfo(cbSubscription))
    }

    fun fillSubscriptionComboBox(subscriptions: List<Subscription>, defaultSubscription: Subscription? = null) {
        cbSubscription.fillComboBox(subscriptions, defaultSubscription)

        if (subscriptions.isEmpty()) {
            lastSelectedSubscriptionId = ""
        }
    }

    private fun initSubscriptionComboBox() {
        cbSubscription.setDefaultRenderer(message("run_config.publish.form.subscription.empty_message")) { it.displayName() }

        cbSubscription.addActionListener {
            val subscription = cbSubscription.getSelectedValue() ?: return@addActionListener
            val selectedSid = subscription.subscriptionId()
            if (lastSelectedSubscriptionId == selectedSid) return@addActionListener
            lastSelectedSubscriptionId = selectedSid

            listenerAction(subscription)
        }
    }
}
