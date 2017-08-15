package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import java.util.List;

public class WebAppModule extends AzureRefreshableNode {
    private static final String REDIS_SERVICE_MODULE_ID = WebAppModule.class.getName();
    private static final String ICON_PATH = "WebApp_16.png";
    private static final String BASE_MODULE_NAME = "Web Apps";
    private final WebAppModulePresenter<WebAppModule> webAppModulePresenter;

    /**
     * Create the node containing all the Web App resources.
     *
     * @param parent The parent node of this node
     */
    public WebAppModule(Node parent) {
        super(REDIS_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
        webAppModulePresenter = new WebAppModulePresenter<>();
        webAppModulePresenter.onAttachView(WebAppModule.this);
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        webAppModulePresenter.onModuleRefresh();

    }

    public void renderWebApps(List<ResourceEx<WebApp>> winapps, List<ResourceEx<SiteInner>> linuxapps) {
        winapps.forEach(app -> {
            addChildNode(new WinWebAppNode(this, app, ICON_PATH));
        });
        linuxapps.forEach(app -> {
            addChildNode(new LinuxWebAppNode(this, app, ICON_PATH));
        });
    }
}
