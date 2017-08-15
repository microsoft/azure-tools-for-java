package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinuxWebAppNode extends WebAppNode {
    private static final String RUN_STATUS = "Running";
    private SiteInner siteInner;
    private WebApp webApp;

    public LinuxWebAppNode(WebAppModule parent, ResourceEx app, String icon) {
        super(((SiteInner) app.getResource()).id(), ((SiteInner) app.getResource()).name(), parent, icon, true);
        siteInner = (SiteInner) app.getResource();
        subscriptionId = app.getSubscriptionId();

        loadActions();
    }

    @Override
    public List<NodeAction> getNodeActions() {
        String state = webApp == null ? siteInner.state() : webApp.state();
        boolean running = RUN_STATUS.equals(state);
        getNodeActionByName(ACTION_START).setEnabled(!running);
        getNodeActionByName(ACTION_STOP).setEnabled(running);
        getNodeActionByName(ACTION_RESTART).setEnabled(running);

        return super.getNodeActions();
    }

    @Override
    public WebApp getWebApp() {
        if (webApp == null) {
            try {
                return AzureWebAppMvpModel.getInstance().getWebAppById(subscriptionId, siteInner.id());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return webApp;
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        properties.put(AppInsightsConstants.Region, this.siteInner.location());
        return properties;
    }

    @Override
    public String getWebAppId() {
        return siteInner.id();
    }

    @Override
    public String getWebAppName() {
        return siteInner.name();
    }


    @Override
    public void stopWebApp() {
        if (webApp == null) {
            try {
                webApp = AzureWebAppMvpModel.getInstance().getWebAppById(subscriptionId, siteInner.id());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (webApp != null) {
            webApp.stop();
        }
    }

    @Override
    public void startWebApp() {
        if (webApp == null) {
            try {
                webApp = AzureWebAppMvpModel.getInstance().getWebAppById(subscriptionId, siteInner.id());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (webApp != null) {
            webApp.start();
        }
    }

    @Override
    public void restartWebApp() {
        if (webApp == null) {
            try {
                webApp = AzureWebAppMvpModel.getInstance().getWebAppById(subscriptionId, siteInner.id());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (webApp != null) {
            webApp.restart();
        }
    }
}
