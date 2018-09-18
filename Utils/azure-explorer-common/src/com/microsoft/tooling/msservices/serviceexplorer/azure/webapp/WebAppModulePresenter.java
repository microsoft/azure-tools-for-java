package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import rx.Observable;

public class WebAppModulePresenter<V extends WebAppModuleView> extends MvpPresenter<V> {
    /**
     * Called from view when the view needs refresh.
     */
    public void onModuleRefresh() {
        final WebAppModuleView view = getMvpView();
        if (view != null) {
            view.renderChildren(AzureWebAppMvpModel.getInstance().listAllWebApps(true));
        }
    }

    public void onDeleteWebApp(String sid, String id) throws IOException {
        AzureWebAppMvpModel.getInstance().deleteWebApp(sid, id);
    }
}
