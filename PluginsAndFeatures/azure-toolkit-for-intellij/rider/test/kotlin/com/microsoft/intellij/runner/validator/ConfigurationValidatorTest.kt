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

package com.microsoft.intellij.runner.validator

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.jetbrains.rider.test.asserts.shouldBe
import com.jetbrains.rider.test.asserts.shouldBeEmpty
import com.jetbrains.rider.test.asserts.shouldBeFalse
import com.jetbrains.rider.test.asserts.shouldBeTrue
import com.microsoft.intellij.helpers.validator.ValidationResult
import org.testng.annotations.Test

class ConfigurationValidatorTest {

    @Test
    fun testCheckStatus_ValidStatus_NoException() {
        val validator = ConfigurationValidator()
        val status = ValidationResult()

        val validationResult = validator.checkStatus(status)

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Test message.")
    fun testCheckStatus_ValidStatus_SingleError() {
        val validator = ConfigurationValidator()
        val status = ValidationResult().setInvalid("Test message.")

        validator.checkStatus(status)
    }

    @Test(expectedExceptions = [RuntimeConfigurationError::class], expectedExceptionsMessageRegExp = "Test message.")
    fun testCheckStatus_ValidStatus_MultipleErrors() {
        val validator = ConfigurationValidator()
        val status = ValidationResult()
                .setInvalid("Test message.")
                .setInvalid("Other message.")

        status.isValid.shouldBeFalse()
        status.errors.size.shouldBe(2)

        validator.checkStatus(status)
    }
}
