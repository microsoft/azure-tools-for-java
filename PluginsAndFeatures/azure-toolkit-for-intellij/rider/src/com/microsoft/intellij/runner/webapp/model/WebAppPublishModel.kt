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

package com.microsoft.intellij.runner.webapp.model

import com.jetbrains.rider.model.PublishableProjectModel
import com.microsoft.azure.management.appservice.*
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.intellij.helpers.defaults.AzureDefaults

class WebAppPublishModel {

    companion object {
        val defaultOperatingSystem = OperatingSystem.WINDOWS
        val defaultPricingTier: PricingTier = PricingTier.STANDARD_S1
        val defaultNetFrameworkVersion: NetFrameworkVersion = NetFrameworkVersion.fromString("4.7")
        val defaultRuntime = RuntimeStack("DOTNETCORE", "2.1")
    }

    var publishableProject: PublishableProjectModel? = null

    var subscription: Subscription? = null

    var isCreatingWebApp = false
    var webAppId = ""
    var webAppName = ""

    var isCreatingResourceGroup = false
    var resourceGroupName = ""

    var isCreatingAppServicePlan = false
    var appServicePlanId = ""
    var appServicePlanName = ""
    var operatingSystem = defaultOperatingSystem
    var location = AzureDefaults.location.name()
    var pricingTier = defaultPricingTier

    var netFrameworkVersion = defaultNetFrameworkVersion
    var netCoreRuntime = defaultRuntime

    /**
     * Reset the model with values after creating a new instance
     */
    fun resetOnPublish(webApp: WebApp) {
        isCreatingWebApp = false
        webAppId = webApp.id()
        webAppName = ""

        isCreatingResourceGroup = false

        isCreatingAppServicePlan = false
        appServicePlanId = webApp.appServicePlanId()
        appServicePlanName = ""
    }
}