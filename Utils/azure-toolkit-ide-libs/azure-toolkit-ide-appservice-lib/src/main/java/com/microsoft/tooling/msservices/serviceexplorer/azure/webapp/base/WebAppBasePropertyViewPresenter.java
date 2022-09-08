/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.core.mvp.ui.webapp.WebAppProperty;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppBasePropertyMvpView;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    public static final String KEY_JAVA_CONTAINER_VERSION = "javaContainerVersion";

    public void onLoadWebAppProperty(AppServiceAppBase<?, ?, ?> app) {
        final WebAppProperty property = Objects.isNull(app) || app.isDraftForCreating() ? new WebAppProperty(new HashMap<>()) :
                generateProperty(app, Objects.requireNonNull(app.getAppServicePlan()));
        AzureTaskManager.getInstance().runLater(() -> Optional.ofNullable(getMvpView()).ifPresent(v -> v.showProperty(property)));
    }

    public void onLoadWebAppProperty(@Nonnull final String sid, @Nonnull final String appId, @Nullable final String slotName) {
        final String appName = ResourceId.fromString(appId).name();
        final AzureString title = AzureString.format("load properties of App Service '{0}'", appName);
        AzureTaskManager.getInstance().runInBackground(title, () -> onLoadWebAppProperty(getWebAppBase(sid, appId, slotName)));
    }

    protected WebAppProperty generateProperty(@Nonnull final AppServiceAppBase<?, ?, ?> appService, @Nonnull final AppServicePlan plan) {
        final Map<String, String> appSettingsMap = appService.getAppSettings();
        final Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(KEY_NAME, appService.getName());
        propertyMap.put(KEY_RESOURCE_GRP, appService.getResourceGroupName());
        propertyMap.put(KEY_LOCATION, Optional.ofNullable(appService.getRegion()).map(Region::getLabel).orElse("N/A"));
        propertyMap.put(KEY_SUB_ID, appService.getSubscriptionId());
        propertyMap.put(KEY_STATUS, StringUtils.capitalize(StringUtils.lowerCase(appService.getStatus())));
        propertyMap.put(KEY_PLAN, plan.getName());
        propertyMap.put(KEY_URL, appService.getHostName());
        final PricingTier pricingTier = plan.getPricingTier();
        propertyMap.put(KEY_PRICING, String.format("%s_%s", pricingTier.getTier(), pricingTier.getSize()));
        final Runtime runtime = appService.getRuntime();
        final JavaVersion javaVersion = Optional.ofNullable(runtime).map(Runtime::getJavaVersion).orElse(null);
        if (javaVersion != null && ObjectUtils.notEqual(javaVersion, JavaVersion.OFF)) {
            propertyMap.put(KEY_JAVA_VERSION, javaVersion.getValue());
            propertyMap.put(KEY_JAVA_CONTAINER, runtime.getWebContainer().getValue());
        }
        propertyMap.put(KEY_OPERATING_SYS, Optional.ofNullable(runtime).map(Runtime::getOperatingSystem).orElse(null));
        propertyMap.put(KEY_APP_SETTING, appSettingsMap);

        return new WebAppProperty(propertyMap);
    }

    protected abstract AppServiceAppBase<?, ?, ?> getWebAppBase(@Nonnull String sid, @Nonnull String appId,
                                                                @Nullable String slotName);

    protected abstract void updateAppSettings(@Nonnull String sid, @Nonnull String webAppId, @Nullable String name,
                                              @Nonnull Map<String, String> toUpdate, @Nonnull Set<String> toRemove) throws Exception;

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
        final AppServiceAppBase<?, ?, ?> resource = getWebAppBase(sid, webAppId, name);
        try (final InputStream inputStream = resource.listPublishingProfileXmlWithSecrets();
             final OutputStream outputStream = Files.newOutputStream(file.toPath())) {
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
            .subscribe(property -> AzureTaskManager.getInstance().runLater(() -> {
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
            .subscribeOn(getSchedulerProvider().io()).subscribe(res -> AzureTaskManager.getInstance().runLater(() -> {
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
        telemetryMap.put("operationName", actionName);
        telemetryMap.put("serviceName", "WebApp");
        AzureTelemeter.log(AzureTelemetry.Type.OP_END, telemetryMap);
    }
}
