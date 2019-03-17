/**
 * Copyright (c) 2019 JetBrains s.r.o.
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

open class AppValidator(type: String) : AzureResourceValidator() {

    companion object {
        private const val CONNECTION_STRING_NAME_NOT_DEFINED = "Connection string not set."
        private const val APP_NAME_MIN_LENGTH = 2
        private const val APP_NAME_MAX_LENGTH = 60
    }

    private val appNotDefined = "Please select an existing $type App."

    private val appNameNotDefined = "$type App name not provided."
    private val appNameCannotStartEndWithDash = "$type App name cannot begin or end with '-' symbol."
    private val appNameInvalid = "$type App name cannot contain characters: %s."

    private val appNameRegex = "[^\\p{L}0-9-]".toRegex()
    private val appNameLengthError =
            "$type App name should be from $APP_NAME_MIN_LENGTH to $APP_NAME_MAX_LENGTH characters."

    // Please see for details -
    // https://docs.microsoft.com/en-us/azure/app-service/app-service-web-get-started-dotnet?toc=%2Fen-us%2Fdotnet%2Fapi%2Fazure_ref_toc%2Ftoc.json&bc=%2Fen-us%2Fdotnet%2Fazure_breadcrumb%2Ftoc.json&view=azure-dotnet#create-an-app-service-plan
    fun validateAppName(name: String): ValidationResult {

        val status = checkAppNameIsSet(name)
        if (!status.isValid) return status

        return status
                .merge(checkStartsEndsWithDash(name))
                .merge(checkNameMinLength(name))
                .merge(checkNameMaxLength(name))
                .merge(checkInvalidCharacters(name))
    }

    fun checkAppIdIsSet(webAppId: String?) =
            checkValueIsSet(webAppId, appNotDefined)

    fun checkAppNameIsSet(name: String) =
            checkValueIsSet(name, appNameNotDefined)

    fun checkStartsEndsWithDash(name: String): ValidationResult {
        val status = ValidationResult()
        if (name.startsWith('-') || name.endsWith('-')) status.setInvalid(appNameCannotStartEndWithDash)
        return status
    }

    fun checkNameMaxLength(name: String) =
            checkNameMaxLength(name, APP_NAME_MAX_LENGTH, appNameLengthError)

    fun checkNameMinLength(name: String) =
            checkNameMinLength(name, APP_NAME_MIN_LENGTH, appNameLengthError)

    fun checkInvalidCharacters(name: String) =
            validateResourceNameRegex(name, appNameRegex, appNameInvalid)

    fun checkConnectionStringNameIsSet(name: String) =
            checkValueIsSet(name, CONNECTION_STRING_NAME_NOT_DEFINED)
}
