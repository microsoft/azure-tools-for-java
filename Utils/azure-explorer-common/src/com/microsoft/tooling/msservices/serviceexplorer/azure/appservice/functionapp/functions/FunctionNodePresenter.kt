/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.functions

import com.microsoft.azuretools.core.mvp.model.functionapp.AzureFunctionAppMvpModel
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter
import com.microsoft.tooling.msservices.components.DefaultLoader
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.FunctionAppNode
import org.slf4j.LoggerFactory
import java.util.logging.Logger

class FunctionNodePresenter<V : FunctionNodeView> : MvpPresenter<V>() {

    companion object {
        private val logger = LoggerFactory.getLogger(FunctionNodePresenter::class.java)
    }

    @Suppress("unused")
    fun onEnableFunction(subscriptionId: String, functionAppId: String, functionName: String) {
        val functionApp = AzureFunctionAppMvpModel.getFunctionAppById(subscriptionId, functionAppId)
        val settingName = "AzureWebJobs.$functionName.Disabled"
        val setting = functionApp.appSettings.values.singleOrNull { it.key().compareTo(settingName, true) == 0 }
        if (setting == null) {
            logger.error("Setting with name '$settingName' not found")
            return
        }

        logger.info("Setting with name '$settingName' has value: '${setting.value()}'")
        if (setting.value()?.toBoolean() == false)
            return

        functionApp.update().withoutAppSetting(settingName).apply()

        DefaultLoader.getIdeHelper().executeOnPooledThread { mvpView?.renderNode(true) }
    }

    @Suppress("unused")
    fun onDisableFunction(subscriptionId: String, functionAppId: String, functionName: String) {
        val functionApp = AzureFunctionAppMvpModel.getFunctionAppById(subscriptionId, functionAppId)
        val settingName = "AzureWebJobs.$functionName.Disabled"
        val setting = functionApp.appSettings.values.singleOrNull { it.key().compareTo(settingName, true) == 0 }
        if (setting != null) {
            logger.warn("Setting with name '$settingName' already exists and has value: '${setting.value()}'")

            if (setting.value()?.toBoolean() == true)
                return
        }

        functionApp.update().withAppSetting(settingName, "true").apply()

        DefaultLoader.getIdeHelper().executeOnPooledThread { mvpView?.renderNode(false) }
    }
}
