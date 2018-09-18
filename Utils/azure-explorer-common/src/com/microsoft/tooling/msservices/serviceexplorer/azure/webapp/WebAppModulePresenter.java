package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import java.io.IOException;

import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;

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
