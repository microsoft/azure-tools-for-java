/*
 * Copyright (c) Microsoft Corporation
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

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.AppSetting;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.core.mvp.ui.webapp.WebAppProperty;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppBasePropertyMvpView;

import rx.Observable;

public abstract class WebAppBasePropertyViewPresenter<V extends WebAppBasePropertyMvpView> extends MvpPresenter<V> {
    public static final String KEY_NAME = "name";
    public static final String KEY_TYPE = "type";
    public static final String KEY_RESOURCE_GRP = "resourceGroup";
    public static final String KEY_LOCATION = "location";
    public static final String KEY_SUB_ID = "subscription";
    public static final String KEY_STATUS = "status";
    public static final String KEY_PLAN = "servicePlan";
    public static final String KEY_URL = "url";
    public static final String KEY_PRICING = "pricingTier";
    public static final String KEY_JAVA_VERSION = "javaVersion";
    public static final String KEY_JAVA_CONTAINER = "javaContainer";
    public static final String KEY_JAVA_CONTAINER_VERSION = "javaContainerVersion";
    public static final String KEY_OPERATING_SYS = "operatingSystem";
    public static final String KEY_APP_SETTING = "appSetting";

    private static final String CANNOT_GET_WEB_APP_PROPERTY = "An exception occurred when getting the application settings.";

    public void onLoadWebAppProperty(final String sid, @NotNull final String webAppId, @Nullable final String name) {
        Observable.fromCallable(() -> {
            final Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
            final WebAppBase appBase = getWebAppBase(sid, webAppId, name);
            final AppServicePlan plan = azure.appServices().appServicePlans().getById(appBase.appServicePlanId());
            return generateProperty(appBase, plan);
        }).subscribeOn(getSchedulerProvider().io())
            .subscribe(property -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (isViewDetached()) {
                    return;
                }
                getMvpView().showProperty(property);
            }), e -> errorHandler((Exception) e));
    }

    protected WebAppProperty generateProperty(@NotNull final WebAppBase webAppBase, @NotNull final AppServicePlan plan) {
        final Map<String, String> appSettingsMap = new HashMap<>();
        final Map<String, AppSetting> appSetting = webAppBase.getAppSettings();
        for (final String key : appSetting.keySet()) {
            final AppSetting setting = appSetting.get(key);
            if (setting != null) {
                appSettingsMap.put(setting.key(), setting.value());
            }
        }
        final Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(KEY_NAME, webAppBase.name());
        propertyMap.put(KEY_TYPE, webAppBase.type());
        propertyMap.put(KEY_RESOURCE_GRP, webAppBase.resourceGroupName());
        propertyMap.put(KEY_LOCATION, webAppBase.regionName());
        propertyMap.put(KEY_SUB_ID, webAppBase.manager().subscriptionId());
        propertyMap.put(KEY_STATUS, webAppBase.state());
        propertyMap.put(KEY_PLAN, plan.name());
        propertyMap.put(KEY_URL, webAppBase.defaultHostName());
        propertyMap.put(KEY_PRICING, plan.pricingTier().toString());
        final String javaVersion = webAppBase.javaVersion().toString();
        if (!javaVersion.equals("null")) {
            propertyMap.put(KEY_JAVA_VERSION, webAppBase.javaVersion().toString());
            propertyMap.put(KEY_JAVA_CONTAINER, webAppBase.javaContainer());
            propertyMap.put(KEY_JAVA_CONTAINER_VERSION, webAppBase.javaContainerVersion());
        }
        propertyMap.put(KEY_OPERATING_SYS, webAppBase.operatingSystem());
        propertyMap.put(KEY_APP_SETTING, appSettingsMap);

        return new WebAppProperty(propertyMap);
    }

    protected abstract WebAppBase getWebAppBase(@NotNull String sid, @NotNull String webAppId,
                                                @Nullable String name) throws Exception;

    protected abstract void updateAppSettings(@NotNull String sid, @NotNull String webAppId, @Nullable String name,
                                              @NotNull Map toUpdate, @NotNull Set toRemove) throws Exception;

    protected abstract boolean getPublishingProfile(@NotNull String sid, @NotNull String webAppId, @Nullable String name,
                                                    @NotNull String filePath) throws Exception;

    public void onUpdateWebAppProperty(@NotNull final String sid, @NotNull final String webAppId,
                                       @Nullable final String name,
                                       @NotNull final Map<String, String> cacheSettings,
                                       @NotNull final Map<String, String> editedSettings) {
        final Map<String, String> telemetryMap = new HashMap<>();
        telemetryMap.put("SubscriptionId", sid);
        Observable.fromCallable(() -> {
            final Set<String> toRemove = new HashSet<>();
            for (String key : cacheSettings.keySet()) {
                if (!editedSettings.containsKey(key)) {
                    toRemove.add(key);
                }
            }
            updateAppSettings(sid, webAppId, name, editedSettings, toRemove);
            return true;
        }).subscribeOn(getSchedulerProvider().io())
            .subscribe(property -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (isViewDetached()) {
                    return;
                }
                getMvpView().showPropertyUpdateResult(true);
                sendTelemetry("UpdateAppSettings", telemetryMap, true, null);
            }), e -> {
                errorHandler((Exception) e);
                if (isViewDetached()) {
                    return;
                }
                getMvpView().showPropertyUpdateResult(false);
                sendTelemetry("UpdateAppSettings", telemetryMap, false, e.getMessage());
            });
    }

    public void onGetPublishingProfileXmlWithSecrets(@NotNull final String sid, @NotNull final String webAppId,
                                                     @Nullable final String name, @NotNull final String filePath) {
        final Map<String, String> telemetryMap = new HashMap<>();
        telemetryMap.put("SubscriptionId", sid);
        Observable.fromCallable(() -> getPublishingProfile(sid, webAppId, name, filePath))
            .subscribeOn(getSchedulerProvider().io()).subscribe(res -> DefaultLoader.getIdeHelper().invokeLater(() -> {
            if (isViewDetached()) {
                return;
            }
            getMvpView().showGetPublishingProfileResult(res);
            sendTelemetry("DownloadPublishProfile", telemetryMap, true, null);
        }), e -> {
            errorHandler((Exception) e);
            if (isViewDetached()) {
                return;
            }
            getMvpView().showGetPublishingProfileResult(false);
            sendTelemetry("DownloadPublishProfile", telemetryMap, false, e.getMessage());
        });
    }

    protected void errorHandler(final Exception e) {
        DefaultLoader.getIdeHelper().invokeLater(() -> {
            if (isViewDetached()) {
                return;
            }
            getMvpView().onErrorWithException(CANNOT_GET_WEB_APP_PROPERTY, e);
        });
    }

    protected void sendTelemetry(@NotNull final String actionName, @NotNull final Map<String, String> telemetryMap,
                                 final boolean success, @Nullable final String errorMsg) {
        telemetryMap.put("Success", String.valueOf(success));
        if (!success) {
            telemetryMap.put("ErrorMsg", errorMsg);
        }
        AppInsightsClient.createByType(AppInsightsClient.EventType.Action, "WebApp", actionName, telemetryMap);
    }
}
