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

import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.resources.implementation.LocationInner

class LocationMock(
        private val name: String = "eastus2",
        private val region: Region = Region.US_EAST2
): Location {

    override fun region(): Region = region

    override fun longitude(): String {
        TODO("Not yet implemented")
    }

    override fun key(): String {
        TODO("Not yet implemented")
    }

    override fun inner(): LocationInner {
        TODO("Not yet implemented")
    }

    override fun subscriptionId(): String {
        TODO("Not yet implemented")
    }

    override fun displayName(): String {
        TODO("Not yet implemented")
    }

    override fun latitude(): String {
        TODO("Not yet implemented")
    }

    override fun name(): String = name
}