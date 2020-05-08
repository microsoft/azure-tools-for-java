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
import org.testng.annotations.Test

class AzureResourceValidatorTest {

    private val validator = AzureResourceValidator()

    //region String

    @Test
    fun testCheckValueIsSet_String_ValueIsSet() {
        val value: String? = "value"
        val validationResult = validator.checkValueIsSet(value, "String value is not set.")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckValueIsSet_String_ValueIsNotSet() {
        val value: String? = null
        val validationResult = validator.checkValueIsSet(value, "String value is not set.")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("String value is not set.")
    }

    @Test
    fun testCheckValueIsSet_String_ValueIsEmpty() {
        val value: String? = ""
        val validationResult = validator.checkValueIsSet(value, "String value is not set.")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("String value is not set.")
    }

    //endregion String

    //region Char array

    @Test
    fun testCheckValueIsSet_CharArray_ValueIsSet() {
        val value: CharArray? = "value".toCharArray()
        val validationResult = validator.checkValueIsSet(value, "CharArray value is not set.")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckValueIsSet_CharArray_ValueIsNotSet() {
        val value: CharArray? = null
        val validationResult = validator.checkValueIsSet(value, "CharArray value is not set.")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("CharArray value is not set.")
    }

    @Test
    fun testCheckValueIsSet_CharArray_ValueIsEmpty() {
        val value: CharArray? = CharArray(0)
        val validationResult = validator.checkValueIsSet(value, "CharArray value is not set.")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("CharArray value is not set.")
    }

    //endregion Char array

    //region Object

    class TestObject

    @Test
    fun testCheckValueIsSet_Object_ValueIsSet() {
        val value: TestObject? = TestObject()
        val validationResult = validator.checkValueIsSet(value, "TestObject value is not set.")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckValueIsSet_Object_ValueIsNotSet() {
        val value: TestObject? = null
        val validationResult = validator.checkValueIsSet(value, "TestObject value is not set.")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("TestObject value is not set.")
    }

    //endregion Object

    //region Resource name

    // TODO: Reverse regex logic
    @Test
    fun testValidateResourceNameRegex_NameDoesNotMatchErrorRegex() {
        val name = "my-valid-name"
        val nameRegex = Regex("invalid-name")
        val errorMessage = "Resource Name validation failed."
        val validationResult = validator.validateResourceNameRegex(name, nameRegex, errorMessage)

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testValidateResourceNameRegex_NameMatchErrorRegex() {
        val name = "invalid-name"
        val nameRegex = Regex("invalid-name")
        val errorMessage = "Resource Name validation failed."
        val validationResult = validator.validateResourceNameRegex(name, nameRegex, errorMessage)

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("Resource Name validation failed.")
    }

    //endregion Resource name

    //region Name Max length

    @Test
    fun testCheckNameMaxLength_EmptyName() {
        val validationResult =
                validator.checkNameMaxLength("", 8, "Name exceed max length.")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckNameMaxLength_BelowMax() {
        val validationResult =
                validator.checkNameMaxLength("test-name", 10, "Name exceed max length.")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckNameMaxLength_Max() {
        val validationResult =
                validator.checkNameMaxLength("test-name", 9, "Name exceed max length.")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckNameMaxLength_AboveMax() {
        val validationResult =
                validator.checkNameMaxLength("test-name", 8, "Name exceed max length.")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("Name exceed max length.")
    }

    //endregion Name Max length

    //region Name Min length

    @Test
    fun testCheckNameMinLength_EmptyName() {
        val validationResult =
                validator.checkNameMinLength("", 8, "Name is below min length.")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("Name is below min length.")
    }

    @Test
    fun testCheckNameMinLength_BelowMin() {
        val validationResult =
                validator.checkNameMinLength("test-name", 10, "Name is below min length.")

        validationResult.isValid.shouldBeFalse()
        validationResult.errors.size.shouldBe(1)
        validationResult.errors[0].shouldBe("Name is below min length.")
    }

    @Test
    fun testCheckNameMinLength_Min() {
        val validationResult =
                validator.checkNameMinLength("test-name", 9, "Name is below min length.")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    @Test
    fun testCheckNameMinLength_AboveMin() {
        val validationResult =
                validator.checkNameMinLength("test-name", 8, "Name is below min length.")

        validationResult.isValid.shouldBeTrue()
        validationResult.errors.shouldBeEmpty()
    }

    //endregion Name Min length
}