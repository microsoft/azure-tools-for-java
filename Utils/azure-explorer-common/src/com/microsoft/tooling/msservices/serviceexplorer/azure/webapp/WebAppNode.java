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

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotModule;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

public class WebAppNode extends RefreshableNode implements TelemetryProperties, WebAppNodeView {
    private static final String ACTION_START = "Start";
    private static final String ACTION_STOP = "Stop";
    private static final String ACTION_DELETE = "Delete";
    private static final String ACTION_RESTART = "Restart";
    private static final String ACTION_OPEN_IN_BROWSER = "Open In Browser";
    private static final String ACTION_SHOW_PROPERTY = "Show Properties";
    private static final String ICON_RUNNING_POSTFIX = "WebAppRunning_16.png";
    private static final String ICON_STOPPED_POSTFIX = "WebAppStopped_16.png";
    private static final String DELETE_WEBAPP_PROMPT_MESSAGE = "This operation will delete Web App %s.\n"
        + "Are you sure you want to continue?";
    private static final String DELETE_WEBAPP_PROGRESS_MESSAGE = "Deleting Web App";
    private final WebAppNodePresenter<WebAppNode> webAppNodePresenter;

    @NotNull
    private DeploymentSlotModule deploymentSlotModule;

    protected String subscriptionId;
    protected String webAppName;
    protected WebAppState webAppState;
    protected String webAppId;
    protected String hostName;
    protected String webAppOS;
    protected Map<String, String> propertyMap;

    /**
     * Constructor.
     */
    public WebAppNode(WebAppModule parent, String subscriptionId, String webAppId, String webAppName,
                      String state, String hostName, String os, Map<String, String> propertyMap) {
        super(webAppId, webAppName, parent, getIcon(WebAppState.fromString(state), os), true);
        this.subscriptionId = subscriptionId;
        this.webAppId = webAppId;
        this.webAppName = webAppName;
        this.webAppState = WebAppState.fromString(state);
        this.hostName = hostName;
        this.webAppOS = StringUtils.capitalize(os.toLowerCase());
        this.propertyMap = propertyMap;
        webAppNodePresenter = new WebAppNodePresenter<>();
        webAppNodePresenter.onAttachView(WebAppNode.this);
        this.deploymentSlotModule = new DeploymentSlotModule(this, subscriptionId, webAppId);
        loadActions();
    }

    protected static String getIcon(final WebAppState state, final String os) {
        return StringUtils.capitalize(os.toLowerCase())
            + (state == WebAppState.RUNNING ? ICON_RUNNING_POSTFIX : ICON_STOPPED_POSTFIX);
    }

    @Override
    public List<NodeAction> getNodeActions() {
        boolean running = this.webAppState == WebAppState.RUNNING;
        getNodeActionByName(ACTION_START).setEnabled(!running);
        getNodeActionByName(ACTION_STOP).setEnabled(running);
        getNodeActionByName(ACTION_RESTART).setEnabled(running);

        return super.getNodeActions();
    }

    @Override
    protected void refreshItems() {
        webAppNodePresenter.onNodeRefresh();
    }

    @Override
    public void renderSubModules() {
        if (!isDirectChild(deploymentSlotModule)) {
            addChildNode(deploymentSlotModule);
        }
    }

    @Override
    protected void loadActions() {
        addAction(ACTION_STOP, getIcon(WebAppState.STOPPED, this.webAppOS), new NodeActionListener() {
            @Override
            public void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getIdeHelper().runInBackground(null, "Stopping Web App", false,
                    true, "Stopping Web App...", () -> stopWebApp());
            }
        });
        addAction(ACTION_START, new NodeActionListener() {
            @Override
            public void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getIdeHelper().runInBackground(null, "Starting Web App", false,
                    true, "Starting Web App...", () -> startWebApp());
            }
        });
        addAction(ACTION_RESTART, new NodeActionListener() {
            @Override
            public void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getIdeHelper().runInBackground(null, "Restarting Web App", false,
                    true, "Restarting Web App...", () -> restartWebApp());
            }
        });

        addAction(ACTION_DELETE, new DeleteWebAppAction());

        // Open in browser action
        addAction(ACTION_OPEN_IN_BROWSER, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                String appServiceLink = "http://" + hostName;
                DefaultLoader.getUIHelper().openInBrowser(appServiceLink);
            }
        });

        addAction(ACTION_SHOW_PROPERTY, null, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                DefaultLoader.getUIHelper().openWebAppPropertyView(WebAppNode.this);
            }
        });

        super.loadActions();
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        properties.put(AppInsightsConstants.Region, this.propertyMap.get("regionName"));
        return properties;
    }

    public String getSubscriptionId() {
        return this.subscriptionId;
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

    @Override
    public void renderWebAppNode(@NotNull WebAppState state) {
        switch (state) {
            case RUNNING:
                this.webAppState = state;
                this.setIconPath(getIcon(WebAppState.RUNNING, this.webAppOS));
                break;
            case STOPPED:
                this.webAppState = state;
                this.setIconPath(getIcon(WebAppState.STOPPED, this.webAppOS));
                break;
            default:
                break;
        }
    }

    private class DeleteWebAppAction extends AzureNodeActionPromptListener {
        DeleteWebAppAction() {
            super(WebAppNode.this, String.format(DELETE_WEBAPP_PROMPT_MESSAGE, getWebAppName()),
                    DELETE_WEBAPP_PROGRESS_MESSAGE);
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e) throws AzureCmdException {
            getParent().removeNode(getSubscriptionId(), getWebAppId(), WebAppNode.this);
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
        }
    }
}
