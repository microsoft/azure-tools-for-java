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

package com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp

import com.microsoft.azure.management.appservice.FunctionApp
import com.microsoft.azuretools.core.mvp.model.functionapp.AzureFunctionAppMvpModel
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.base.FunctionAppBasePropertyViewPresenter

class FunctionAppPropertyViewPresenter : FunctionAppBasePropertyViewPresenter<FunctionAppPropertyMvpView>() {

    override fun getWebAppBase(subscriptionId: String, appId: String): FunctionApp =
            AzureFunctionAppMvpModel.getFunctionAppById(subscriptionId, appId)

    override fun updateAppSettings(subscriptionId: String,
                                   appId: String,
                                   name: String,
                                   toUpdate: Map<String, String>,
                                   toRemove: Set<String>) {
        AzureFunctionAppMvpModel.updateFunctionAppSettings(subscriptionId, appId, toUpdate, toRemove)
    }

    override fun getPublishingProfile(subscriptionId: String, appId: String, filePath: String) =
            AzureFunctionAppMvpModel.getPublishableProfileXmlWithSecrets(subscriptionId, appId, filePath)
}
