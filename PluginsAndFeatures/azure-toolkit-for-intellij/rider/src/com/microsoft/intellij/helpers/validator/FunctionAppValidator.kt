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

import com.microsoft.azure.management.appservice.FunctionApp
import com.microsoft.azuretools.core.mvp.model.functionapp.AzureFunctionAppMvpModel

object FunctionAppValidator : AppValidator("Function") {

    private const val FUNCTION_APP_NOT_DEFINED = "Function App is not set."
    private const val FUNCTION_APP_ALREADY_EXISTS = "Function App with name '%s' already exists."
    private const val CONNECTION_STRING_NAME_ALREADY_EXISTS = "Connection String with name '%s' already exists."

    // Please see for details -
    // https://docs.microsoft.com/en-us/azure/app-service/app-service-web-get-started-dotnet?toc=%2Fen-us%2Fdotnet%2Fapi%2Fazure_ref_toc%2Ftoc.json&bc=%2Fen-us%2Fdotnet%2Fazure_breadcrumb%2Ftoc.json&view=azure-dotnet#create-an-app-service-plan
    fun validateFunctionAppName(subscriptionId: String, name: String): ValidationResult {
        val status = validateAppName(name)
        if (!status.isValid) return status

        return status.merge(checkFunctionAppExists(subscriptionId, name))
    }

    fun checkFunctionAppIsSet(app: FunctionApp?) =
            checkValueIsSet(app, FUNCTION_APP_NOT_DEFINED)

    fun checkFunctionAppIdIsSet(appId: String) =
            checkValueIsSet(appId, FUNCTION_APP_NOT_DEFINED)

    fun checkFunctionAppExists(subscriptionId: String, name: String): ValidationResult {
        val status = ValidationResult()
        if (isFunctionAppExist(subscriptionId, name)) return status.setInvalid(String.format(FUNCTION_APP_ALREADY_EXISTS, name))
        return status
    }

    fun checkConnectionStringNameExistence(subscriptionId: String, appId: String, connectionStringName: String): ValidationResult {
        val status = checkConnectionStringNameIsSet(connectionStringName)
        if (!status.isValid) return status

        if (AzureFunctionAppMvpModel.checkConnectionStringNameExists(subscriptionId, appId, connectionStringName))
            status.setInvalid(String.format(CONNECTION_STRING_NAME_ALREADY_EXISTS, connectionStringName))

        return status
    }

    private fun isFunctionAppExist(subscriptionId: String, functionAppName: String) =
            AzureFunctionAppMvpModel.checkFunctionAppNameExists(subscriptionId, functionAppName)
}
