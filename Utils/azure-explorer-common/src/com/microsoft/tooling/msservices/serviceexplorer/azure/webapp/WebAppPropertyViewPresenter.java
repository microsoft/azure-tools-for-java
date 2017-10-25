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

    private static final String CANNOT_GET_WEB_APP_PROPERTY = "Cannot get Web App's property.";

    public void onLoadWebAppProperty(@NotNull String sid, @NotNull String resId) {
        Observable.fromCallable(() -> {
            WebApp app = AzureWebAppMvpModel.getInstance().getWebAppById(sid, resId);
            Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
            AppServicePlan plan = azure.appServices().appServicePlans().getById(app.appServicePlanId());
            return generateProperty(app, plan);
        }).subscribeOn(getSchedulerProvider().io()).subscribe(property -> DefaultLoader.getIdeHelper().invokeLater(() -> {
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
        return new WebAppProperty(app.name(), app.type(), app.resourceGroupName(), app.regionName(),
                app.manager().subscriptionId(), app.state(), plan.name(), app.defaultHostName(),
                plan.pricingTier().toString(), app.javaVersion().toString(), app.javaContainer(),
                app.javaContainerVersion(), app.operatingSystem(), appSettingsMap);
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
