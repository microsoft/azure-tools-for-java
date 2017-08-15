/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import com.microsoft.azure.Resource;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WinWebAppNode extends WebAppNode {
    private static final String RUN_STATUS = "Running";
    private WebApp webApp;

    public WinWebAppNode(WebAppModule parent, ResourceEx app, String icon) {
        super(((WebApp) app.getResource()).id(), ((WebApp) app.getResource()).name(), parent, icon, true);
        webApp = (WebApp) app.getResource();
        subscriptionId = app.getSubscriptionId();

        loadActions();
    }

    @Override
    public List<NodeAction> getNodeActions() {
        boolean running = RUN_STATUS.equals(webApp.inner().state());
        getNodeActionByName(ACTION_START).setEnabled(!running);
        getNodeActionByName(ACTION_STOP).setEnabled(running);
        getNodeActionByName(ACTION_RESTART).setEnabled(running);

        return super.getNodeActions();
    }

    @Override
    public WebApp getWebApp() {
        return webApp;
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        properties.put(AppInsightsConstants.Region, this.webApp.regionName());
        return properties;
    }

    @Override
    public String getWebAppId() {
        return webApp.id();
    }

    @Override
    public String getWebAppName() {
        return webApp.name();
    }

    @Override
    public void stopWebApp() {
        webApp.stop();
    }

    @Override
    public void startWebApp() {
        webApp.start();
    }

    @Override
    public void restartWebApp() {
        webApp.restart();
    }

    private class DeleteWebAppAction extends AzureNodeActionPromptListener {
        DeleteWebAppAction() {
            super(WinWebAppNode.this,
                    String.format("This operation will delete Web App %s.\nAre you sure you want to continue?", "",
                            webApp.name()),
                    "Deleting Web App");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e) throws AzureCmdException {
            try {
                AzureWebAppMvpModel.getInstance().deleteWebApp(subscriptionId, webApp.id());

                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        // instruct parent node to remove this node
                        getParent().removeDirectChildNode(WinWebAppNode.this);
                    }
                });
            } catch (Exception ex) {
                DefaultLoader.getUIHelper().showException("An error occurred while attempting to delete the Web App",
                        ex,
                        "Azure Services Explorer - Error Deleting Web App", false, true);
            }
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
        }
    }
}