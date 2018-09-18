package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import java.io.IOException;

import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import rx.Observable;

public class WebAppModulePresenter<V extends WebAppModuleView> extends MvpPresenter<V> {
    private static final String CANNOT_GET_WEB_APPS = "Cannot get web apps.";
    /**
     * Called from view when the view needs refresh.
     */
    public void onModuleRefresh() {
        Observable.fromCallable(() -> AzureWebAppMvpModel.getInstance().listAllWebApps(true))
            .subscribeOn(getSchedulerProvider().io())
            .subscribe(webApps -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (isViewDetached()) {
                    return;
                }
                getMvpView().renderChildren(webApps);
            }), e -> errorHandler(CANNOT_GET_WEB_APPS, (Exception) e));
    }

    public void onDeleteWebApp(String sid, String id) throws IOException {
        AzureWebAppMvpModel.getInstance().deleteWebApp(sid, id);
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
