package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceId;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.azuretools.utils.AzureUIRefreshListener;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
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
        createListener();
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        webAppModulePresenter.onModuleRefresh();

    }

    /**
     * Create nodes for both Windows and Linux Web Apps.
     *
     * @param winapps   list of Windows Web Apps
     * @param linuxapps list of Linux Web Apps
     */
    public void renderWebApps(List<ResourceEx<WebApp>> winapps, List<ResourceEx<SiteInner>> linuxapps) {
        winapps.forEach(app -> addChildNode(new WinWebAppNode(this, app)));
        linuxapps.forEach(app -> addChildNode(new LinuxWebAppNode(this, app)));
    }

    private void createListener() {
        String id = "WebAppModule";
        AzureUIRefreshListener listener = new AzureUIRefreshListener() {
            @Override
            public void run() {
                if (event.opsType == AzureUIRefreshEvent.EventType.SIGNIN || event.opsType == AzureUIRefreshEvent .EventType.SIGNOUT) {
                    removeAllChildNodes();
                } else if (event.object == null && (event.opsType == AzureUIRefreshEvent.EventType.UPDATE || event.opsType == AzureUIRefreshEvent.EventType.REMOVE)) {
                    if (hasChildNodes()) {
                        load(true);
                    }
                } else if (event.object == null && event.opsType == AzureUIRefreshEvent.EventType.REFRESH) {
                    load(true);
                } else if (event.object != null && event.object.getClass().toString().equals(WebAppUtils
                        .WebAppDetails.class.toString())) {
                    WebAppUtils.WebAppDetails webAppDetails = (WebAppUtils.WebAppDetails) event.object;
                    switch (event.opsType) {
                        case ADD:
                            DefaultLoader.getIdeHelper().invokeLater(() -> {
                                try {
                                    addChildNode(new WinWebAppNode(WebAppModule.this,
                                            new ResourceEx<>(webAppDetails.webApp, ResourceId.fromString(webAppDetails.webApp.id()).subscriptionId())));
                                } catch (Exception ex) {
                                    DefaultLoader.getUIHelper().logError("WebAppModule::createListener ADD", ex);
                                    ex.printStackTrace();
                                }
                            });
                            break;
                        case UPDATE:
                            break;
                        case REMOVE:
                            break;
                        default:
                            break;
                    }
                }
            }
        };
        AzureUIRefreshCore.addListener(id, listener);
    }
}
