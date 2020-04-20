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

import com.microsoft.azure.management.appservice.FunctionApp
import com.microsoft.azuretools.core.mvp.model.functionapp.AzureFunctionAppMvpModel
import org.jetbrains.plugins.azure.RiderAzureBundle.message

object FunctionAppValidator : AppValidator(message("service.app_service.function_app")) {

    // Please see for details -
    // https://docs.microsoft.com/en-us/azure/app-service/app-service-web-get-started-dotnet?toc=%2Fen-us%2Fdotnet%2Fapi%2Fazure_ref_toc%2Ftoc.json&bc=%2Fen-us%2Fdotnet%2Fazure_breadcrumb%2Ftoc.json&view=azure-dotnet#create-an-app-service-plan
    fun validateFunctionAppName(subscriptionId: String, name: String): ValidationResult {
        val status = validateAppName(name)
        if (!status.isValid) return status

        return status.merge(checkFunctionAppExists(subscriptionId, name))
    }

    fun checkFunctionAppIsSet(app: FunctionApp?) =
            checkValueIsSet(app, message("run_config.publish.validation.function_app.not_defined"))

    fun checkFunctionAppIdIsSet(appId: String) =
            checkAppIdIsSet(appId)

    fun checkFunctionAppExists(subscriptionId: String, name: String): ValidationResult {
        val status = ValidationResult()
        if (isFunctionAppExist(subscriptionId, name))
            return status.setInvalid(message("run_config.publish.validation.function_app.name_already_exists", name))

        return status
    }

    fun checkConnectionStringNameExistence(subscriptionId: String, appId: String, connectionStringName: String): ValidationResult {
        val status = checkConnectionStringNameIsSet(connectionStringName)
        if (!status.isValid) return status

        if (AzureFunctionAppMvpModel.checkConnectionStringNameExists(subscriptionId, appId, connectionStringName))
            status.setInvalid(message("run_config.publish.validation.connection_string.name_already_exists", connectionStringName))

        return status
    }

    private fun isFunctionAppExist(subscriptionId: String, functionAppName: String) =
            AzureFunctionAppMvpModel.checkFunctionAppNameExists(subscriptionId, functionAppName)
}
