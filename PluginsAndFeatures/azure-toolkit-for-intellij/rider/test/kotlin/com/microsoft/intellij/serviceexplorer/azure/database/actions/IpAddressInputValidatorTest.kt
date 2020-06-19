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

package com.microsoft.intellij.serviceexplorer.azure.database.actions

import com.jetbrains.rider.test.asserts.shouldBe
import com.microsoft.intellij.helpers.validator.IpAddressInputValidator
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class IpAddressInputValidatorTest {

    @DataProvider(name = "validateIpV4AddressData")
    fun validateIpV4AddressData() = arrayOf(
            arrayOf("Zeros", "0.0.0.0", true),
            arrayOf("Localhost", "127.0.0.1", true),
            arrayOf("Max", "255.255.255.255", true),
            arrayOf("OneInvalid", "198.0.256.1", false),
            arrayOf("Empty", "...", false),
            arrayOf("Chars", "0.0.a.0", false),
            arrayOf("Negative", "0.0.-1.0", false),
            arrayOf("Symbol", "0.0.-.0", false),
            arrayOf("Trim", "127.0.1", false)
    )

    @Test(dataProvider = "validateIpV4AddressData")
    fun testValidateIpV4Address(name: String, address: String, isValid: Boolean) {
        val validator = IpAddressInputValidator.instance
        val isValidAddress = validator.validateIpV4Address(address)
        isValidAddress.shouldBe(isValid)
    }
}
