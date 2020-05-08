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

package com.microsoft.intellij.helpers.validator

import com.jetbrains.rider.test.asserts.shouldBe
import com.jetbrains.rider.test.asserts.shouldBeEmpty
import com.jetbrains.rider.test.asserts.shouldBeFalse
import com.jetbrains.rider.test.asserts.shouldBeTrue
import com.microsoft.azure.management.resources.SubscriptionState
import org.jetbrains.mock.SubscriptionMock
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class SubscriptionValidatorTest {

    //region Subscription

    @Test
    fun testCheckSubscriptionIsSet_IsSet() {
        val validationResult = SubscriptionValidator.checkSubscriptionIsSet(SubscriptionMock())

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckSubscriptionIsSet_IsNotSet() {
        val validationResult = SubscriptionValidator.checkSubscriptionIsSet(null)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.subscription.not_defined"))
    }

    //endregion Subscription

    //region Validate Subscription

    @DataProvider(name = "subscriptionValidStatusData")
    fun subscriptionValidStatusData() = arrayOf(
            arrayOf("Enabled", SubscriptionState.ENABLED),
            arrayOf("PassedDue", SubscriptionState.PAST_DUE),
            arrayOf("Warned", SubscriptionState.WARNED)
    )

    @Test(dataProvider = "subscriptionValidStatusData")
    fun testValidateSubscription_ValidState(name: String, subscriptionState: SubscriptionState) {
        val validationResult =
                SubscriptionValidator.validateSubscription(SubscriptionMock(state = subscriptionState))

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testValidateSubscription_State_Disabled() {
        val mockSubscription = SubscriptionMock(state = SubscriptionState.DISABLED)
        val validationResult = SubscriptionValidator.validateSubscription(mockSubscription)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.subscription.disabled", mockSubscription.displayName()))
    }

    @Test
    fun testValidateSubscription_State_Deleted() {
        val mockSubscription = SubscriptionMock(state = SubscriptionState.DELETED)
        val validationResult = SubscriptionValidator.validateSubscription(mockSubscription)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.subscription.deleted", mockSubscription.displayName()))
    }

    @Test
    fun testValidateSubscription_IsNotSet() {
        val validationResult = SubscriptionValidator.validateSubscription(null)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.subscription.not_defined"))
    }

    //endregion Validate Subscription
}
