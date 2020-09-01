/*
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

import com.microsoft.azure.CommonIcons;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotModule;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.DELETE_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;

public class WebAppNode extends WebAppBaseNode implements WebAppNodeView {
    private static final String DELETE_WEBAPP_PROMPT_MESSAGE = "This operation will delete the Web App: %s.\n"
        + "Are you sure you want to continue?";
    private static final String DELETE_WEBAPP_PROGRESS_MESSAGE = "Deleting Web App '%s'...";
    private static final String LABEL = "WebApp";

    private final WebAppNodePresenter<WebAppNode> webAppNodePresenter;
    protected String webAppName;
    protected String webAppId;
    protected Map<String, String> propertyMap;

    private NodeAction startAction;
    private NodeAction stopAction;

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

        startAction = new NodeAction(this, ACTION_START);
        startAction.setIconPath(CommonIcons.ACTION_START);
        startAction.addListener(createBackgroundActionListener("Starting Web App", this::startWebApp));

        stopAction = new NodeAction(this, ACTION_STOP);
        stopAction.setIconPath(CommonIcons.ACTION_STOP);
        stopAction.addListener(createBackgroundActionListener("Stopping Web App", this::stopWebApp));

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
        addAction(ACTION_RESTART, CommonIcons.ACTION_RESTART, createBackgroundActionListener("Restarting Web App", () -> restartWebApp()));
        addAction(ACTION_DELETE, CommonIcons.ACTION_DISCARD, new DeleteWebAppAction());
        addAction(ACTION_OPEN_IN_BROWSER, CommonIcons.ACTION_OPEN_IN_BROWSER, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getUIHelper().openInBrowser("http://" + hostName);
            }
        });
        addAction(ACTION_SHOW_PROPERTY, CommonIcons.ACTION_OPEN_PREFERENCES, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getUIHelper().openWebAppPropertyView(WebAppNode.this);
            }
        });

        super.loadActions();
    }

    @Override
    public List<NodeAction> getNodeActions() {
        boolean isRunning = state == WebAppBaseState.RUNNING;

        NodeAction stopAction = getNodeActionByName(ACTION_STOP);
        NodeAction startAction = getNodeActionByName(ACTION_START);

        if (isRunning && stopAction == null) {
            nodeActions.remove(getNodeActionByName(ACTION_START));
            nodeActions.add(0, this.stopAction);
        } else if (!isRunning && startAction == null) {
            nodeActions.remove(getNodeActionByName(ACTION_STOP));
            nodeActions.add(0, this.startAction);
        }

        return super.getNodeActions();
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
                    String.format(DELETE_WEBAPP_PROGRESS_MESSAGE, getWebAppName()));
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
