package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import java.io.IOException;

public class WebAppNodePresenter<V extends WebAppNodeView> extends MvpPresenter<V> {
    public void onStartWebApp(String subscriptionId, String webAppId) throws IOException {
        AzureWebAppMvpModel.getInstance().startWebApp(subscriptionId, webAppId);
        final WebAppNodeView view = getMvpView();
        if (view == null) {
            return;
        }
        view.renderWebAppNode(WebAppState.RUNNING);
    }

    public void onRestartWebApp(String subscriptionId, String webAppId) throws IOException {
        AzureWebAppMvpModel.getInstance().restartWebApp(subscriptionId, webAppId);
        final WebAppNodeView view = getMvpView();
        if (view == null) {
            return;
        }
        view.renderWebAppNode(WebAppState.RUNNING);
    }

    public void onStopWebApp(String subscriptionId, String webAppId) throws IOException {
        AzureWebAppMvpModel.getInstance().stopWebApp(subscriptionId, webAppId);
        final WebAppNodeView view = getMvpView();
        if (view == null) {
            return;
        }
        view.renderWebAppNode(WebAppState.STOPPED);
    }

    public void onNodeRefresh() {
        final WebAppNodeView view = getMvpView();
        if (view != null) {
            view.renderSubModules();
        }
    }
}
