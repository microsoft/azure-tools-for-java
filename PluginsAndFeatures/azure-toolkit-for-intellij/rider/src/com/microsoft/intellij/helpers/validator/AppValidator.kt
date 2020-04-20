/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

import org.jetbrains.plugins.azure.RiderAzureBundle.message

open class AppValidator(type: String) : AzureResourceValidator() {

    companion object {
        private const val APP_NAME_MIN_LENGTH = 2
        private const val APP_NAME_MAX_LENGTH = 60
    }

    private val appNameRegex = "[^\\p{L}0-9-]".toRegex()

    private val appNotDefined = message("run_config.publish.validation.app.not_defined", type)
    private val appNameNotDefined = message("run_config.publish.validation.app.name_not_defined", type)
    private val appNameCannotStartEndWithDash = message("run_config.publish.validation.app.name_cannot_start_end_with_dash", type)
    private val appNameInvalid = "${message("run_config.publish.validation.app.name_invalid", type)} %s"
    private val appNameLengthError = message("run_config.publish.validation.app.name_length_error", type, APP_NAME_MIN_LENGTH, APP_NAME_MAX_LENGTH)

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

        if (name.startsWith('-') || name.endsWith('-'))
            status.setInvalid(appNameCannotStartEndWithDash)

        return status
    }

    fun checkNameMaxLength(name: String) =
            checkNameMaxLength(name, APP_NAME_MAX_LENGTH, appNameLengthError)

    fun checkNameMinLength(name: String) =
            checkNameMinLength(name, APP_NAME_MIN_LENGTH, appNameLengthError)

    fun checkInvalidCharacters(name: String) =
            validateResourceNameRegex(name, appNameRegex, appNameInvalid)

    fun checkConnectionStringNameIsSet(name: String) =
            checkValueIsSet(name, message("run_config.publish.validation.connection_string.not_defined"))
}
