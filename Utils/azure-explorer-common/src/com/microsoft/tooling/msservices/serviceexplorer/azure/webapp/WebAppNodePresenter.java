package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import java.io.IOException;

public class WebAppNodePresenter<V extends WebAppNode> extends MvpPresenter<WebAppNode> {
    public void onStartWebApp(String subscriptionId, String webAppId) throws IOException {
        AzureWebAppMvpModel.getInstance().startWebApp(subscriptionId, webAppId);
        final WebAppNode view = getMvpView();
        if (view == null) {
            return;
        }
        view.setRunning();
    }

    public void onRestartWebApp(String subscriptionId, String webAppId) throws IOException {
        AzureWebAppMvpModel.getInstance().restartWebApp(subscriptionId, webAppId);
        final WebAppNode view = getMvpView();
        if (view == null) {
            return;
        }
        view.setRunning();
    }

    public void onStopWebApp(String subscriptionId, String webAppId) throws IOException {
        AzureWebAppMvpModel.getInstance().stopWebApp(subscriptionId, webAppId);
        final WebAppNode view = getMvpView();
        if (view == null) {
            return;
        }
        view.setStopped();
    }

    public void onNodeRefresh() {
        // todo
    }
}
