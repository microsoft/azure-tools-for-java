/**
 * Copyright (c) 2018-2019 JetBrains s.r.o.
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

import com.microsoft.azuretools.utils.AzureModel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel

object WebAppValidator : AppValidator("Web") {

    private const val WEB_APP_ALREADY_EXISTS = "Web App with name '%s' already exists."
    private const val CONNECTION_STRING_NAME_ALREADY_EXISTS = "Connection String with name '%s' already exists."

    // Please see for details -
    // https://docs.microsoft.com/en-us/azure/app-service/app-service-web-get-started-dotnet?toc=%2Fen-us%2Fdotnet%2Fapi%2Fazure_ref_toc%2Ftoc.json&bc=%2Fen-us%2Fdotnet%2Fazure_breadcrumb%2Ftoc.json&view=azure-dotnet#create-an-app-service-plan
    fun validateWebAppName(name: String): ValidationResult {
        val status = validateAppName(name)
        if (!status.isValid) return status

        return status.merge(checkWebAppExists(name))
    }

    fun checkWebAppExists(name: String): ValidationResult {
        val status = ValidationResult()
        if (isWebAppExist(name)) return status.setInvalid(String.format(WEB_APP_ALREADY_EXISTS, name))
        return status
    }

    fun checkConnectionStringNameExistence(name: String, webAppId: String): ValidationResult {
        val status = checkConnectionStringNameIsSet(name)
        if (!status.isValid) return status

        val resourceGroupToWebAppMap = AzureModel.getInstance().resourceGroupToWebAppMap ?: return status

        val webApp = resourceGroupToWebAppMap
                .flatMap { it.value }
                .firstOrNull { it.id() == webAppId } ?: return status

        if (AzureDotNetWebAppMvpModel.checkConnectionStringNameExists(webApp, name))
            status.setInvalid(String.format(CONNECTION_STRING_NAME_ALREADY_EXISTS, name))

        return status
    }

    private fun isWebAppExist(webAppName: String): Boolean {
        val resourceGroupToWebAppMap = AzureModel.getInstance().resourceGroupToWebAppMap ?: return false

        val webApps = resourceGroupToWebAppMap.flatMap { it.value }
        return webApps.any { it.name().equals(webAppName, true) }
    }
}
