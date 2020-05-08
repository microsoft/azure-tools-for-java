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

import com.jetbrains.rider.test.asserts.*
import org.testng.annotations.Test
import javax.swing.JPanel

class ValidationResultTest {

    @Test
    fun testValidationResult_Default() {
        val validationResult = ValidationResult()
        validationResult.errors.shouldBeEmpty()
        validationResult.isValid.shouldBeTrue()
    }

    @Test
    fun testSetInvalid_SingleError() {
        val validationResult = ValidationResult()
        validationResult.setInvalid("Error message.")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("Error message.")
    }

    @Test
    fun testSetInvalid_MultipleErrors() {
        val validationResult = ValidationResult()
        validationResult.setInvalid("Error message - 1.")
        validationResult.setInvalid("Error message - 2.")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(2)
        validationResult.errors[0].shouldBe("Error message - 1.")
        validationResult.errors[1].shouldBe("Error message - 2.")
    }

    @Test
    fun testSetInvalid_EmptyErrorMessage() {
        val validationResult = ValidationResult()
        validationResult.setInvalid("")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("")
    }

    @Test
    fun testMerge_ValidWithValid() {
        val validationResult = ValidationResult()
        val validationResultOther = ValidationResult()

        validationResult.merge(validationResultOther)

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testMerge_ValidWithInvalid() {
        val validationResult = ValidationResult()

        val validationResultOther = ValidationResult()
        validationResultOther.setInvalid("Other error message.")

        validationResult.merge(validationResultOther)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("Other error message.")
    }

    @Test
    fun testMerge_InvalidWithValid() {
        val validationResult = ValidationResult()
        validationResult.setInvalid("Error message.")

        val validationResultOther = ValidationResult()

        validationResult.merge(validationResultOther)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("Error message.")
    }

    @Test
    fun testMerge_InvalidWithInvalid() {
        val validationResult = ValidationResult()
        validationResult.setInvalid("Error message.")

        val validationResultOther = ValidationResult()
        validationResultOther.setInvalid("Other error message.")

        validationResult.merge(validationResultOther)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(2)
        validationResult.errors[0].shouldBe("Error message.")
        validationResult.errors[1].shouldBe("Other error message.")
    }

    @Test
    fun testToValidationInfo_Valid() {
        val validationResult = ValidationResult()
        val fakeComponent = JPanel()
        val validationInfo = validationResult.toValidationInfo(fakeComponent)

        validationInfo.shouldBeNull()
    }

    @Test
    fun testToValidationInfo_Invalid() {
        val validationResult = ValidationResult()
        validationResult.setInvalid("Error message.")
        validationResult.setInvalid("Other error message.")

        val fakeComponent = JPanel()
        val validationInfo = validationResult.toValidationInfo(fakeComponent)

        val validationInfoInstance = validationInfo.shouldNotBeNull()
        validationInfoInstance.component.shouldBe(fakeComponent)
        validationInfoInstance.message.shouldBe("Error message.")
        validationInfoInstance.warning.shouldBeFalse()
        validationInfoInstance.okEnabled.shouldBeFalse()
    }
}
