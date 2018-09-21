package com.microsoft.intellij.helpers.validator

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.resources.SubscriptionState

object SubscriptionValidator : ConfigurationValidator() {

    private const val SUBSCRIPTION_NOT_DEFINED = "Subscription not provided"
    private const val SUBSCRIPTION_DISABLED = "Subscription '%s' is disabled"
    private const val SUBSCRIPTION_DELETED = "Subscription '%s' is deleted"

    @Throws(RuntimeConfigurationError::class)
    fun validateSubscription(subscription: Subscription?): Subscription {
        val selectedSubscription = checkValueIsSet(subscription, SUBSCRIPTION_NOT_DEFINED)
        validateSubscriptionState(selectedSubscription)
        return selectedSubscription
    }

    @Throws(RuntimeConfigurationError::class)
    private fun validateSubscriptionState(subscription: Subscription) {
        val subscriptionState = subscription.state()

        if (subscriptionState == SubscriptionState.DISABLED)
            throw RuntimeConfigurationError(String.format(SUBSCRIPTION_DISABLED, subscription.displayName()))

        if (subscriptionState == SubscriptionState.DELETED)
            throw RuntimeConfigurationError(String.format(SUBSCRIPTION_DELETED, subscription.displayName()))
    }

    @Throws(RuntimeConfigurationError::class)
    private fun checkValueIsSet(value: Subscription?, message: String): Subscription {
        return value ?: throw RuntimeConfigurationError(message)
    }
}