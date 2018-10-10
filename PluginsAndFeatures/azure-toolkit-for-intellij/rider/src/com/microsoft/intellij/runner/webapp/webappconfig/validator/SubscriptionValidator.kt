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

package com.microsoft.intellij.runner.webapp.webappconfig.validator

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.resources.SubscriptionState
import com.microsoft.intellij.runner.webapp.webappconfig.UiConstants

object SubscriptionValidator : ConfigurationValidator() {

    @Throws(RuntimeConfigurationError::class)
    fun validateSubscription(subscription: Subscription?): Subscription {
        val selectedSubscription = checkValueIsSet(subscription, UiConstants.SUBSCRIPTION_NOT_DEFINED)
        validateSubscriptionState(selectedSubscription)
        return selectedSubscription
    }

    @Throws(RuntimeConfigurationError::class)
    private fun validateSubscriptionState(subscription: Subscription) {
        val subscriptionState = subscription.state()

        if (subscriptionState == SubscriptionState.DISABLED)
            throw RuntimeConfigurationError(String.format(UiConstants.SUBSCRIPTION_DISABLED, subscription.displayName()))

        if (subscriptionState == SubscriptionState.DELETED)
            throw RuntimeConfigurationError(String.format(UiConstants.SUBSCRIPTION_DELETED, subscription.displayName()))
    }

    @Throws(RuntimeConfigurationError::class)
    private fun checkValueIsSet(value: Subscription?, message: String): Subscription {
        return value ?: throw RuntimeConfigurationError(message)
    }
}