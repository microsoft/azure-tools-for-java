package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.AppSetting;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.core.mvp.ui.webapp.WebAppProperty;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import rx.Observable;

public class WebAppPropertyViewPresenter<V extends WebAppPropertyMvpView> extends MvpPresenter<V> {

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

    private static final String CANNOT_GET_WEB_APP_PROPERTY = "Cannot get Web App's property.";

    public void onLoadWebAppProperty(@NotNull String sid, @NotNull String resId) {
        Observable.fromCallable(() -> {
            WebApp app = AzureWebAppMvpModel.getInstance().getWebAppById(sid, resId);
            Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
            AppServicePlan plan = azure.appServices().appServicePlans().getById(app.appServicePlanId());
            return generateProperty(app, plan);
        }).subscribeOn(getSchedulerProvider().io())
                .subscribe(property -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                    if (isViewDetached()) {
                        return;
                    }
                    getMvpView().showProperty(property);
                }), e -> errorHandler(CANNOT_GET_WEB_APP_PROPERTY, (Exception) e));
    }

    private WebAppProperty generateProperty(WebApp app, AppServicePlan plan) {
        Map<String, String> appSettingsMap = new HashMap<>();
        Map<String, AppSetting> appSetting = app.appSettings();
        for (String key : app.appSettings().keySet()) {
            AppSetting setting = appSetting.get(key);
            if (setting != null) {
                appSettingsMap.put(setting.key(), setting.value());
            }
        }
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put(KEY_NAME, app.name());
        propertyMap.put(KEY_TYPE, app.type());
        propertyMap.put(KEY_RESOURCE_GRP, app.resourceGroupName());
        propertyMap.put(KEY_LOCATION, app.regionName());
        propertyMap.put(KEY_SUB_ID, app.manager().subscriptionId());
        propertyMap.put(KEY_STATUS, app.state());
        propertyMap.put(KEY_PLAN, plan.name());
        propertyMap.put(KEY_URL, app.defaultHostName());
        propertyMap.put(KEY_PRICING, plan.pricingTier().toString());
        String javaVersion = app.javaVersion().toString();
        if (!javaVersion.equals("null")) {
            propertyMap.put(KEY_JAVA_VERSION, app.javaVersion().toString());
            propertyMap.put(KEY_JAVA_CONTAINER, app.javaContainer());
            propertyMap.put(KEY_JAVA_CONTAINER_VERSION, app.javaContainerVersion());
        }
        propertyMap.put(KEY_OPERATING_SYS, app.operatingSystem());
        propertyMap.put(KEY_APP_SETTING, appSettingsMap);

        return new WebAppProperty(propertyMap);
    }

    private void errorHandler(String msg, Exception e) {
        DefaultLoader.getIdeHelper().invokeLater(() -> {
            if (isViewDetached()) {
                return;
            }
            getMvpView().onErrorWithException(msg, e);
        });
    }
}
