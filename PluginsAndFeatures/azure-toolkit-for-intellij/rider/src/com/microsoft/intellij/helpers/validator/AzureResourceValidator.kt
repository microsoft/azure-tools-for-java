/**
 * Copyright (c) 2018 JetBrains s.r.o.
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

/**
 * Please see for validation details -
 * https://docs.microsoft.com/en-us/azure/architecture/best-practices/naming-conventions
 */
open class AzureResourceValidator {

    fun checkValueIsSet(value: String?, message: String): ValidationResult =
            checkValueIsSet(failCondition = { value.isNullOrEmpty() }, message = message)

    fun checkValueIsSet(value: CharArray?, message: String): ValidationResult =
            checkValueIsSet(failCondition = { value == null || value.isEmpty() }, message = message)

    fun checkValueIsSet(value: Any?, message: String): ValidationResult =
            checkValueIsSet(failCondition = { value == null }, message = message)

    fun validateResourceNameRegex(name: String,
                                  nameRegex: Regex,
                                  nameInvalidCharsMessage: String): ValidationResult {

        val status = ValidationResult()

        val matches = nameRegex.findAll(name)
        if (matches.count() > 0) {
            val invalidChars = matches.map { it.value }.distinct().joinToString("', '", "'", "'")
            status.setInvalid(String.format(nameInvalidCharsMessage, invalidChars))
        }

        return status
    }

    fun checkNameMaxLength(name: String, maxLength: Int, errorMessage: String): ValidationResult {
        val status = ValidationResult()
        if (name.length > maxLength) return status.setInvalid(errorMessage)
        return status
    }

    fun checkNameMinLength(name: String, minLength: Int, errorMessage: String): ValidationResult {
        val status = ValidationResult()
        if (name.length < minLength) return status.setInvalid(errorMessage)
        return status
    }

    private fun checkValueIsSet(failCondition: () -> Boolean, message: String): ValidationResult {
        val status = ValidationResult()
        if (failCondition()) status.setInvalid(message)
        return status
    }
}
