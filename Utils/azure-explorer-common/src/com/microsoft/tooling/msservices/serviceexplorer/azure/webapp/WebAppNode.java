/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.DELETE_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.RESTART_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.START_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.STOP_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP_OPEN_INBROWSER;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP_SHOWPROP;

import com.microsoft.tooling.msservices.serviceexplorer.WrappedTelemetryNodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotModule;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState;

public class WebAppNode extends WebAppBaseNode implements WebAppNodeView {
    private static final String DELETE_WEBAPP_PROMPT_MESSAGE = "This operation will delete the Web App: %s.\n"
        + "Are you sure you want to continue?";
    private static final String DELETE_WEBAPP_PROGRESS_MESSAGE = "Deleting Web App";
    private static final String LABEL = "WebApp";

    private final WebAppNodePresenter<WebAppNode> webAppNodePresenter;
    protected String webAppName;
    protected String webAppId;
    protected Map<String, String> propertyMap;

    /**
     * Constructor.
     */
    public WebAppNode(WebAppModule parent, String subscriptionId, String webAppId, String webAppName,
                      String state, String hostName, String os, Map<String, String> propertyMap) {
        super(webAppId, webAppName, LABEL, parent, subscriptionId, hostName, os, state);
        this.webAppId = webAppId;
        this.webAppName = webAppName;
        this.propertyMap = propertyMap;
        webAppNodePresenter = new WebAppNodePresenter<>();
        webAppNodePresenter.onAttachView(WebAppNode.this);
        loadActions();
    }

    @Override
    protected void refreshItems() {
        webAppNodePresenter.onNodeRefresh();
    }

    @Override
    public void renderSubModules() {
        addChildNode(new DeploymentSlotModule(this, this.subscriptionId, this.webAppId));
    }

    @Override
    protected void loadActions() {
        addAction(ACTION_STOP, getIcon(this.os, this.label, WebAppBaseState.STOPPED),
            new WrappedTelemetryNodeActionListener(WEBAPP, STOP_WEBAPP,
                createBackgroundActionListener("Stopping Web App", () -> stopWebApp())));
        addAction(ACTION_START, new WrappedTelemetryNodeActionListener(WEBAPP, START_WEBAPP,
            createBackgroundActionListener("Starting Web App", () -> startWebApp())));
        addAction(ACTION_RESTART, new WrappedTelemetryNodeActionListener(WEBAPP, RESTART_WEBAPP,
            createBackgroundActionListener("Restarting Web App", () -> restartWebApp())));
        addAction(ACTION_DELETE, new DeleteWebAppAction());
        addAction(ACTION_OPEN_IN_BROWSER, new WrappedTelemetryNodeActionListener(WEBAPP, WEBAPP_OPEN_INBROWSER,
            new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    DefaultLoader.getUIHelper().openInBrowser("http://" + hostName);
                }
            }));
        addAction(ACTION_SHOW_PROPERTY, null, new WrappedTelemetryNodeActionListener(WEBAPP, WEBAPP_SHOWPROP,
            new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    DefaultLoader.getUIHelper().openWebAppPropertyView(WebAppNode.this);
                }
            }));
        super.loadActions();
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        properties.put(AppInsightsConstants.Region, this.propertyMap.get("regionName"));
        return properties;
    }

    public String getWebAppId() {
        return this.webAppId;
    }

    public String getWebAppName() {
        return this.webAppName;
    }

    public void startWebApp() {
        try {
            webAppNodePresenter.onStartWebApp(this.subscriptionId, this.webAppId);
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: Error handling
        }
    }

    public void restartWebApp() {
        try {
            webAppNodePresenter.onRestartWebApp(this.subscriptionId, this.webAppId);
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: Error handling
        }
    }

    public void stopWebApp() {
        try {
            webAppNodePresenter.onStopWebApp(this.subscriptionId, this.webAppId);
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: Error handling
        }
    }

    private class DeleteWebAppAction extends AzureNodeActionPromptListener {
        DeleteWebAppAction() {
            super(WebAppNode.this, String.format(DELETE_WEBAPP_PROMPT_MESSAGE, getWebAppName()),
                    DELETE_WEBAPP_PROGRESS_MESSAGE);
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e) {
            getParent().removeNode(getSubscriptionId(), getWebAppId(), WebAppNode.this);
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) {
        }

        @Override
        protected String getServiceName(NodeActionEvent event) {
            return WEBAPP;
        }

        @Override
        protected String getOperationName(NodeActionEvent event) {
            return DELETE_WEBAPP;
        }
    }
}
