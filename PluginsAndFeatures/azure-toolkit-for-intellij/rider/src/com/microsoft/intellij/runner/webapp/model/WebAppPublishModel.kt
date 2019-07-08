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

package com.microsoft.intellij.runner.webapp.model

import com.intellij.ide.browsers.WebBrowserReferenceConverter
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.createLifetime
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.util.xmlb.annotations.Attribute
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.management.appservice.*
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.intellij.helpers.defaults.AzureDefaults
import org.jdom.Element

class WebAppPublishModel {

    companion object {
        val defaultOperatingSystem = OperatingSystem.WINDOWS
        val defaultPricingTier: PricingTier = PricingTier.STANDARD_S1
        val defaultNetFrameworkVersion: NetFrameworkVersion = NetFrameworkVersion.fromString("4.7")
        val defaultRuntime = RuntimeStack("DOTNETCORE", "2.1")

        private const val AZURE_WEB_APP_PROJECT                    = "AZURE_WEB_APP_PROJECT"
        private const val AZURE_WEB_APP_SUBSCRIPTION_ID            = "AZURE_WEB_APP_SUBSCRIPTION_ID"
        private const val AZURE_WEB_APP_IS_CREATE_APP              = "AZURE_WEB_APP_IS_CREATE_APP"
        private const val AZURE_WEB_APP_ID                         = "AZURE_WEB_APP_ID"
        private const val AZURE_WEB_APP_NAME                       = "AZURE_WEB_APP_NAME"
        private const val AZURE_WEB_APP_IS_CREATE_RESOURCE_GROUP   = "AZURE_WEB_APP_IS_CREATE_RESOURCE_GROUP"
        private const val AZURE_WEB_APP_RESOURCE_GROUP_NAME        = "AZURE_WEB_APP_RESOURCE_GROUP_NAME"
        private const val AZURE_WEB_APP_OPERATING_SYSTEM           = "AZURE_WEB_APP_OPERATING_SYSTEM"
        private const val AZURE_WEB_APP_IS_CREATE_APP_SERVICE_PLAN = "AZURE_WEB_APP_IS_CREATE_APP_SERVICE_PLAN"
        private const val AZURE_WEB_APP_SERVICE_PLAN_ID            = "AZURE_WEB_APP_SERVICE_PLAN_ID"
        private const val AZURE_WEB_APP_SERVICE_PLAN_NAME          = "AZURE_WEB_APP_SERVICE_PLAN_NAME"
        private const val AZURE_WEB_APP_LOCATION                   = "AZURE_WEB_APP_LOCATION"
        private const val AZURE_WEB_APP_PRICING_TIER               = "AZURE_WEB_APP_PRICING_TIER"
        private const val AZURE_WEB_APP_NET_FRAMEWORK              = "AZURE_WEB_APP_NET_FRAMEWORK"
        private const val AZURE_WEB_APP_NET_CORE_RUNTIME           = "AZURE_WEB_APP_NET_CORE_RUNTIME"
    }

    @Attribute(value = "name", converter = WebBrowserReferenceConverter::class)
    var publishableProject: PublishableProjectModel? = null

    var subscription: Subscription? = null

    var isCreatingNewApp = false
    var appId = ""
    var appName = ""

    var isCreatingResourceGroup = false
    var resourceGroupName = ""

    var isCreatingAppServicePlan = false
    var appServicePlanId = ""
    var appServicePlanName = ""
    var operatingSystem = defaultOperatingSystem
    var location = AzureDefaults.location
    var pricingTier = defaultPricingTier

    var netFrameworkVersion = defaultNetFrameworkVersion
    var netCoreRuntime = defaultRuntime

    /**
     * Reset the model with values after creating a new instance
     */
    fun resetOnPublish(webApp: WebApp) {
        isCreatingNewApp = false
        appId = webApp.id()
        appName = ""

        isCreatingResourceGroup = false
        resourceGroupName = ""

        isCreatingAppServicePlan = false
        appServicePlanName = ""
    }

    fun readExternal(project: Project, element: Element) {

        val projectPath = JDOMExternalizerUtil.readField(element, AZURE_WEB_APP_PROJECT) ?: ""
        project.solution.publishableProjectsModel.publishableProjects.advise(project.createLifetime()) {
            if (it.newValueOpt?.projectFilePath == projectPath)
                publishableProject = it.newValueOpt
        }

        val subscriptionId = JDOMExternalizerUtil.readField(element, AZURE_WEB_APP_SUBSCRIPTION_ID) ?: ""
        subscription = AzureMvpModel.getInstance().selectedSubscriptions.find { it.subscriptionId() == subscriptionId }

        isCreatingNewApp = JDOMExternalizerUtil.readField(element, AZURE_WEB_APP_IS_CREATE_APP) == "1"
        appId = JDOMExternalizerUtil.readField(element, AZURE_WEB_APP_ID) ?: ""
        appName = JDOMExternalizerUtil.readField(element, AZURE_WEB_APP_NAME) ?: ""

        isCreatingResourceGroup = JDOMExternalizerUtil.readField(element, AZURE_WEB_APP_IS_CREATE_RESOURCE_GROUP) == "1"
        resourceGroupName = JDOMExternalizerUtil.readField(element, AZURE_WEB_APP_RESOURCE_GROUP_NAME) ?: ""

        val osString = JDOMExternalizerUtil.readField(element, AZURE_WEB_APP_OPERATING_SYSTEM) ?: defaultOperatingSystem.name
        operatingSystem = OperatingSystem.fromString(osString)

        isCreatingAppServicePlan = JDOMExternalizerUtil.readField(element, AZURE_WEB_APP_IS_CREATE_APP_SERVICE_PLAN) == "1"
        appServicePlanId = JDOMExternalizerUtil.readField(element, AZURE_WEB_APP_SERVICE_PLAN_ID) ?: ""
        appServicePlanName = JDOMExternalizerUtil.readField(element, AZURE_WEB_APP_SERVICE_PLAN_NAME) ?: ""

        val locationName = JDOMExternalizerUtil.readField(element, AZURE_WEB_APP_LOCATION) ?: AzureDefaults.location.name()
        location = Region.fromName(locationName)

        val pricingTierName = JDOMExternalizerUtil.readField(element, AZURE_WEB_APP_PRICING_TIER) ?: defaultPricingTier.toSkuDescription().name()
        val skuDescription = SkuDescription().withName(pricingTierName)
        pricingTier = PricingTier.fromSkuDescription(skuDescription)

        val netFrameworkName = JDOMExternalizerUtil.readField(element, AZURE_WEB_APP_NET_FRAMEWORK) ?: defaultNetFrameworkVersion.toString()
        netFrameworkVersion = NetFrameworkVersion.fromString(netFrameworkName)

        val coreRuntimeString = JDOMExternalizerUtil.readField(element, AZURE_WEB_APP_NET_CORE_RUNTIME) ?: defaultRuntime.toString()
        val runtimeArray = coreRuntimeString.split(" ")
        netCoreRuntime = if (runtimeArray.size != 2) defaultRuntime else RuntimeStack(runtimeArray[0], runtimeArray[1])
    }

    fun writeExternal(element: Element) {
        JDOMExternalizerUtil.writeField(element, AZURE_WEB_APP_PROJECT, publishableProject?.projectFilePath ?: "")

        JDOMExternalizerUtil.writeField(element, AZURE_WEB_APP_SUBSCRIPTION_ID, subscription?.subscriptionId() ?: "")

        JDOMExternalizerUtil.writeField(element, AZURE_WEB_APP_IS_CREATE_APP, if (isCreatingNewApp) "1" else "0")
        JDOMExternalizerUtil.writeField(element, AZURE_WEB_APP_ID, appId)
        JDOMExternalizerUtil.writeField(element, AZURE_WEB_APP_NAME, appName)

        JDOMExternalizerUtil.writeField(element, AZURE_WEB_APP_IS_CREATE_RESOURCE_GROUP, if (isCreatingResourceGroup) "1" else "0")
        JDOMExternalizerUtil.writeField(element, AZURE_WEB_APP_RESOURCE_GROUP_NAME, resourceGroupName)

        JDOMExternalizerUtil.writeField(element, AZURE_WEB_APP_OPERATING_SYSTEM, operatingSystem.name)

        JDOMExternalizerUtil.writeField(element, AZURE_WEB_APP_IS_CREATE_APP_SERVICE_PLAN, if (isCreatingAppServicePlan) "1" else "0")
        JDOMExternalizerUtil.writeField(element, AZURE_WEB_APP_SERVICE_PLAN_ID, appServicePlanId)
        JDOMExternalizerUtil.writeField(element, AZURE_WEB_APP_SERVICE_PLAN_NAME, appServicePlanName)
        JDOMExternalizerUtil.writeField(element, AZURE_WEB_APP_LOCATION, location.name())
        JDOMExternalizerUtil.writeField(element, AZURE_WEB_APP_PRICING_TIER, pricingTier.toSkuDescription().name())

        JDOMExternalizerUtil.writeField(element, AZURE_WEB_APP_NET_FRAMEWORK, netFrameworkVersion.toString())
        JDOMExternalizerUtil.writeField(element, AZURE_WEB_APP_NET_CORE_RUNTIME, netCoreRuntime.toString())
    }
}
