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

package com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.base

import com.microsoft.azure.management.appservice.AppServicePlan
import com.microsoft.azure.management.appservice.WebAppBase
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter
import com.microsoft.azuretools.core.mvp.ui.functionapp.FunctionAppProperty
import com.microsoft.tooling.msservices.components.DefaultLoader
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.FunctionAppPropertyMvpView

import rx.Observable

abstract class FunctionAppBasePropertyViewPresenter<V : FunctionAppPropertyMvpView> : MvpPresenter<V>() {

    companion object {
        const val KEY_NAME = "name"
        const val KEY_TYPE = "type"
        const val KEY_RESOURCE_GROUP = "resourceGroup"
        const val KEY_LOCATION = "location"
        const val KEY_SUBSCRIPTION_ID = "subscription"
        const val KEY_STATUS = "status"
        const val KEY_APP_SERVICE_PLAN = "servicePlan"
        const val KEY_URL = "url"
        const val KEY_PRICING_TIER = "pricingTier"
        const val KEY_OPERATING_SYSTEM = "operatingSystem"
        const val KEY_APP_SETTING = "appSetting"

        private const val CANNOT_GET_FUNCTION_APP_PROPERTY = "An exception occurred when getting the application settings."
    }

    fun onLoadFunctionAppProperty(sid: String, appId: String) {
        Observable.fromCallable {
            val azure = AuthMethodManager.getInstance().getAzureClient(sid)
            val appBase = getWebAppBase(sid, appId)
            val plan = azure.appServices().appServicePlans().getById(appBase.appServicePlanId())
            generateProperty(appBase, plan)
        }.subscribeOn(schedulerProvider.io())
                .subscribe({ property ->
                    DefaultLoader.getIdeHelper().invokeLater {
                        if (isViewDetached) return@invokeLater
                        mvpView.showProperty(property)
                    }
                }, { e -> errorHandler(e as Exception) })
    }

    protected fun generateProperty(appBase: WebAppBase, plan: AppServicePlan): FunctionAppProperty {
        val appSettingsMap = HashMap<String, String>()
        val appSetting = appBase.appSettings

        for (key in appSetting.keys) {
            val setting = appSetting[key]
            if (setting != null) {
                appSettingsMap[setting.key()] = setting.value()
            }
        }

        val propertyMap = HashMap<String, Any>()
        propertyMap[KEY_NAME] = appBase.name()
        propertyMap[KEY_TYPE] = appBase.type()
        propertyMap[KEY_RESOURCE_GROUP] = appBase.resourceGroupName()
        propertyMap[KEY_LOCATION] = appBase.regionName()
        propertyMap[KEY_SUBSCRIPTION_ID] = appBase.manager().subscriptionId()
        propertyMap[KEY_STATUS] = appBase.state()
        propertyMap[KEY_APP_SERVICE_PLAN] = plan.name()
        propertyMap[KEY_URL] = appBase.defaultHostName()
        propertyMap[KEY_PRICING_TIER] = plan.pricingTier().toString()
        propertyMap[KEY_OPERATING_SYSTEM] = appBase.operatingSystem()
        propertyMap[KEY_APP_SETTING] = appSettingsMap

        return FunctionAppProperty(propertyMap)
    }

    protected abstract fun getWebAppBase(subscriptionId: String, appId: String): WebAppBase

    protected abstract fun updateAppSettings(subscriptionId: String, appId: String, name: String, toUpdate: Map<String, String>, toRemove: Set<String>)

    protected abstract fun getPublishingProfile(subscriptionId: String, appId: String, filePath: String): Boolean

    fun onUpdateFunctionAppProperty(sid: String,
                                    webAppId: String,
                                    name: String,
                                    cacheSettings: Map<String, String>,
                                    editedSettings: Map<String, String>) {

        Observable.fromCallable {
            val toRemove = HashSet<String>()
            for (key in cacheSettings.keys) {
                if (!editedSettings.containsKey(key)) {
                    toRemove.add(key)
                }
            }
            updateAppSettings(sid, webAppId, name, editedSettings, toRemove)
            true
        }.subscribeOn(schedulerProvider.io())
                .subscribe({ property ->
                    DefaultLoader.getIdeHelper().invokeLater {
                        if (isViewDetached) return@invokeLater
                        mvpView.showPropertyUpdateResult(true)
                    }
                }, { e ->
                    errorHandler(e as Exception)
                    if (isViewDetached) return@subscribe
                    mvpView.showPropertyUpdateResult(false)
                })
    }

    fun onGetPublishingProfileXmlWithSecrets(sid: String, webAppId: String, filePath: String) {
        Observable.fromCallable { getPublishingProfile(sid, webAppId, filePath) }
                .subscribeOn(schedulerProvider.io()).subscribe({ res ->
                    DefaultLoader.getIdeHelper().invokeLater {
                        if (isViewDetached) return@invokeLater
                        mvpView.showGetPublishingProfileResult(res!!)
                    }
                }, { e ->
                    errorHandler(e as Exception)
                    if (isViewDetached) return@subscribe
                    mvpView.showGetPublishingProfileResult(false)
                })
    }

    protected fun errorHandler(e: Exception) {
        DefaultLoader.getIdeHelper().invokeLater {
            if (isViewDetached)
                return@invokeLater

            mvpView.onErrorWithException(CANNOT_GET_FUNCTION_APP_PROPERTY, e)
        }
    }
}
