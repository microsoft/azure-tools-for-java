/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServiceBaseEntity;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.core.mvp.ui.webapp.WebAppProperty;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppBasePropertyMvpView;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import rx.Observable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class WebAppBasePropertyViewPresenter<V extends WebAppBasePropertyMvpView> extends MvpPresenter<V> {
    public static final String KEY_NAME = "name";
    public static final String KEY_RESOURCE_GRP = "resourceGroup";
    public static final String KEY_LOCATION = "location";
    public static final String KEY_SUB_ID = "subscription";
    public static final String KEY_STATUS = "status";
    public static final String KEY_PLAN = "servicePlan";
    public static final String KEY_URL = "url";
    public static final String KEY_PRICING = "pricingTier";
    public static final String KEY_JAVA_VERSION = "javaVersion";
    public static final String KEY_JAVA_CONTAINER = "javaContainer";
    public static final String KEY_OPERATING_SYS = "operatingSystem";
    public static final String KEY_APP_SETTING = "appSetting";

    public <T extends AppServiceBaseEntity> void onLoadWebAppProperty(@Nonnull final String sid, @Nonnull final String webAppId, @Nullable final String name) {
        Mono.fromCallable(() -> getWebAppBase(sid, webAppId, name)).map(appService -> {
            if (!appService.exists()) {
                return new WebAppProperty(new HashMap<>());
            }
            final IAppServicePlan plan = Azure.az(AzureAppService.class).appServicePlan(appService.entity().getAppServicePlanId());

            return generateProperty(appService, plan);
        }).subscribeOn(Schedulers.boundedElastic()).subscribe(property -> DefaultLoader.getIdeHelper().invokeLater(() -> {
            if (isViewDetached()) {
                return;
            }
            getMvpView().showProperty(property);
        }));
    }

    protected <T extends AppServiceBaseEntity> WebAppProperty generateProperty(@Nonnull final IAppService<T> appService, @Nonnull final IAppServicePlan plan) {
        final AppServiceBaseEntity appServiceEntity = appService.entity();
        final AppServicePlanEntity planEntity = plan.entity();
        final Map<String, String> appSettingsMap = appServiceEntity.getAppSettings();
        final Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(KEY_NAME, appServiceEntity.getName());
        propertyMap.put(KEY_RESOURCE_GRP, appServiceEntity.getResourceGroup());
        propertyMap.put(KEY_LOCATION, appServiceEntity.getRegion().getLabel());
        propertyMap.put(KEY_SUB_ID, appService.subscriptionId());
        propertyMap.put(KEY_STATUS, appService.state());
        propertyMap.put(KEY_PLAN, plan.name());
        propertyMap.put(KEY_URL, appService.hostName());
        final PricingTier pricingTier = planEntity.getPricingTier();
        propertyMap.put(KEY_PRICING, String.format("%s_%s", pricingTier.getTier(), pricingTier.getSize()));
        final Runtime runtime = appService.getRuntime();
        final JavaVersion javaVersion = runtime.getJavaVersion();
        if (javaVersion != null && ObjectUtils.notEqual(javaVersion, JavaVersion.OFF)) {
            propertyMap.put(KEY_JAVA_VERSION, javaVersion.getValue());
            propertyMap.put(KEY_JAVA_CONTAINER, runtime.getWebContainer().getValue());
        }
        propertyMap.put(KEY_OPERATING_SYS, runtime.getOperatingSystem());
        propertyMap.put(KEY_APP_SETTING, appSettingsMap);

        return new WebAppProperty(propertyMap);
    }

    protected abstract <T extends AppServiceBaseEntity> IAppService<T> getWebAppBase(@Nonnull String sid, @Nonnull String webAppId,
                                                                                     @Nullable String name) throws Exception;

    protected abstract void updateAppSettings(@Nonnull String sid, @Nonnull String webAppId, @Nullable String name,
                                              @Nonnull Map toUpdate, @Nonnull Set toRemove) throws Exception;

    protected boolean getPublishingProfile(@Nonnull String sid, @Nonnull String webAppId, @Nullable String name,
                                           @Nonnull String filePath) throws Exception {
        final ResourceId resourceId = ResourceId.fromString(webAppId);
        final File file = new File(Paths.get(filePath, String.format("%s_%s.PublishSettings", resourceId.name(), System.currentTimeMillis())).toString());
        try {
            file.createNewFile();
        } catch (final IOException e) {
            AzureMessager.getMessager().warning("failed to create publishing profile xml file");
            return false;
        }
        final IAppService resource = getWebAppBase(sid, webAppId, name);
        try (final InputStream inputStream = resource.listPublishingProfileXmlWithSecrets();
             final OutputStream outputStream = new FileOutputStream(file)) {
            IOUtils.copy(inputStream, outputStream);
            return true;
        } catch (final IOException e) {
            AzureMessager.getMessager().warning("failed to get publishing profile xml");
            return false;
        }
    }

    public void onUpdateWebAppProperty(@Nonnull final String sid, @Nonnull final String webAppId,
                                       @Nullable final String name,
                                       @Nonnull final Map<String, String> cacheSettings,
                                       @Nonnull final Map<String, String> editedSettings) {
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
                if (isViewDetached()) {
                    return;
                }
                getMvpView().showPropertyUpdateResult(false);
                sendTelemetry("UpdateAppSettings", telemetryMap, false, e.getMessage());
            });
    }

    public void onGetPublishingProfileXmlWithSecrets(@Nonnull final String sid, @Nonnull final String webAppId,
                                                     @Nullable final String name, @Nonnull final String filePath) {
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
                if (isViewDetached()) {
                    return;
                }
                getMvpView().showGetPublishingProfileResult(false);
                sendTelemetry("DownloadPublishProfile", telemetryMap, false, e.getMessage());
            });
    }

    protected void sendTelemetry(@Nonnull final String actionName, @Nonnull final Map<String, String> telemetryMap,
                                 final boolean success, @Nullable final String errorMsg) {
        telemetryMap.put("Success", String.valueOf(success));
        if (!success) {
            telemetryMap.put("ErrorMsg", errorMsg);
        }
        AppInsightsClient.createByType(AppInsightsClient.EventType.Action, "WebApp", actionName, telemetryMap);
    }
}
