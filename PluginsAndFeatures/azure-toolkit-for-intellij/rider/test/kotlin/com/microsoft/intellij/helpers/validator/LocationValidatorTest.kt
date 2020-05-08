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
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import org.jetbrains.mock.LocationMock
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.testng.annotations.Test

class LocationValidatorTest {

    //region Location

    @Test
    fun testCheckLocationIsSet_IsSet_String() {
        val validationResult = LocationValidator.checkLocationIsSet("eastus2")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckLocationIsSet_IsSet_Region() {
        val validationResult = LocationValidator.checkLocationIsSet(Region.US_EAST2)

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckLocationIsSet_IsSet_Location() {
        val validationResult = LocationValidator.checkLocationIsSet(LocationMock())

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckLocationIsSet_IsNotSet_String() {
        val location: String? = null
        val validationResult = LocationValidator.checkLocationIsSet(location)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.location.not_defined"))
    }

    @Test
    fun testCheckLocationIsSet_IsNotSet_Region() {
        val location: Region? = null
        val validationResult = LocationValidator.checkLocationIsSet(location)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.location.not_defined"))
    }

    @Test
    fun testCheckLocationIsSet_IsNotSet_Location() {
        val location: Location? = null
        val validationResult = LocationValidator.checkLocationIsSet(location)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe(RiderAzureBundle.message("run_config.publish.validation.location.not_defined"))
    }

    //endregion Location
}
