package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import com.microsoft.azuretools.core.mvp.ui.base.MvpView;

public interface WebAppNodeView extends MvpView {
    void renderWebAppNode(WebAppState state);
}
