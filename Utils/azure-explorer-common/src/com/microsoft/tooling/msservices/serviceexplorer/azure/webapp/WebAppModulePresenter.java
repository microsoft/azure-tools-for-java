package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class WebAppModulePresenter<V extends WebAppModule> extends MvpPresenter<WebAppModule> {
    public static void onStartWebApp(String sid, String id) throws IOException {
        AzureWebAppMvpModel.getInstance().startWebApp(sid, id);
    }

    public static void onRestartWebApp(String sid, String id) throws IOException {
        AzureWebAppMvpModel.getInstance().restartWebApp(sid, id);
    }

    public static void onStopWebApp(String sid, String id) throws IOException {
        AzureWebAppMvpModel.getInstance().stopWebApp(sid, id);
    }

    /**
     * Called from view when the view needs refresh.
     */
    public void onModuleRefresh() {
        List<ResourceEx<WebApp>> winApps = AzureWebAppMvpModel.getInstance().listWebApps(true);
        List<ResourceEx<SiteInner>> linuxApps = AzureWebAppMvpModel.getInstance().listAllWebAppsOnLinux(true);

        if (getMvpView() == null) {
            return;
        }

        winApps.forEach(app -> getMvpView().addChildNode(new WebAppNode(
                getMvpView(),
                app.getSubscriptionId(),
                app.getResource().id(),
                app.getResource().name(),
                app.getResource().state(),
                new HashMap<String, String>() {
                    {
                        put("regionName", app.getResource().regionName());
                    }
                }
        )));

        linuxApps.forEach(app -> getMvpView().addChildNode(new WebAppNode(
                getMvpView(),
                app.getSubscriptionId(),
                app.getResource().id(),
                app.getResource().name(),
                app.getResource().state(),
                new HashMap<String, String>() {
                    {
                        put("regionName", app.getResource().location());
                    }
                }
        )));
    }

    public void onDeleteWebApp(String sid, String id) throws IOException {
        AzureWebAppMvpModel.getInstance().deleteWebApp(sid, id);
    }
}
