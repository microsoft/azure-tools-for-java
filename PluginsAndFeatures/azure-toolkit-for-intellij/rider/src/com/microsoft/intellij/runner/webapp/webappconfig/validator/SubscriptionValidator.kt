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