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

package org.jetbrains.mock

import com.microsoft.azure.PagedList
import com.microsoft.azure.management.resources.*
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.resources.implementation.SubscriptionInner

class SubscriptionMock(
        private val subscriptionId: String = "test-subscription-id",
        private val tenantId: String = "test-tenant-id",
        private val name: String = "TestSubscriptionName",
        private val state: SubscriptionState = SubscriptionState.ENABLED
) : Subscription {

    override fun state(): SubscriptionState = state

    override fun key(): String {
        TODO("Not yet implemented")
    }

    override fun inner(): SubscriptionInner {
        TODO("Not yet implemented")
    }

    override fun subscriptionId(): String = subscriptionId

    override fun tenantId(): String = tenantId

    override fun subscriptionPolicies(): SubscriptionPolicies {
        TODO("Not yet implemented")
    }

    override fun managedByTenants(): MutableList<ManagedByTenant> {
        TODO("Not yet implemented")
    }

    override fun listLocations(): PagedList<Location> {
        TODO("Not yet implemented")
    }

    override fun getLocationByRegion(p0: Region?): Location {
        TODO("Not yet implemented")
    }

    override fun displayName(): String = name
}
