package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import com.microsoft.azuretools.core.mvp.ui.base.MvpView;

public interface WebAppModuleView extends MvpView {
    void renderChildNode(String subscriptionId, String id, String name,
                         String state, String hostName, String regionName);
}
