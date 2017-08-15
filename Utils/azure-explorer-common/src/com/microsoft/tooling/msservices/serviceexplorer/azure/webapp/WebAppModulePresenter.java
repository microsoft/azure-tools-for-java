package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;

import java.util.List;

public class WebAppModulePresenter<V extends WebAppModule> extends MvpPresenter<WebAppModule> {
    /**
     * Called from view when the view needs refresh.
     */
    public void onModuleRefresh() {
        List<ResourceEx<WebApp>> winapps = AzureWebAppMvpModel.getInstance().listWebApps(true);
        List<ResourceEx<SiteInner>> linuxapps =
                AzureWebAppMvpModel.getInstance().listAllWebAppsOnLinux(true);

        if (getMvpView() == null) {
            return;
        }
       getMvpView().renderWebApps(winapps, linuxapps);

    }
}
