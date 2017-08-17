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

import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;

public class WebAppNode extends Node implements TelemetryProperties, WebAppVirtualInterface {
    static final String STATUS_RUNNING = "Running";
    static final String STATUS_STOPPED = "Stopped";
    private static final String ACTION_START = "Start";
    private static final String ACTION_STOP = "Stop";
    private static final String ACTION_DELETE = "Delete";
    private static final String ACTION_RESTART = "Restart";
    private static final String ACTION_OPEN_IN_BROWSER = "Open In Browser";
    private static final String WEB_RUN_ICON = "WebAppRunning_16.png";
    private static final String WEB_STOP_ICON = "WebAppStopped_16.png";
    private static final String DELETE_WEBAPP_PROMPT_MESSAGE = "This operation will delete Web App %s.\n"
            + "Are you sure you want to continue?";
    private static final String DELETE_WEBAPP_PROGRESS_MESSAGE = "Deleting Web App";

    protected String subscriptionId;
    protected String webAppName;
    protected String runState;
    protected String webAppId;
    protected Map<String, String> propertyMap;

    /**
     * Constructor.
     */
    public WebAppNode(WebAppModule parent, String subscriptionId, String webAppId, String webAppName, String
            runState, Map<String, String> propertyMap) {
        super(webAppId, webAppName, parent, STATUS_RUNNING.equals(runState) ? WEB_RUN_ICON : WEB_STOP_ICON, true);
        this.subscriptionId = subscriptionId;
        this.webAppId = webAppId;
        this.webAppName = webAppName;
        this.runState = runState;
        this.propertyMap = propertyMap;
        loadActions();
    }

    @Override
    public List<NodeAction> getNodeActions() {
        boolean running = STATUS_RUNNING.equals(getRunState());
        getNodeActionByName(ACTION_START).setEnabled(!running);
        getNodeActionByName(ACTION_STOP).setEnabled(running);
        getNodeActionByName(ACTION_RESTART).setEnabled(running);

        return super.getNodeActions();
    }

    @Override
    protected void loadActions() {
        addAction(ACTION_STOP, WEB_STOP_ICON, new NodeActionListener() {
            @Override
            public void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getIdeHelper().runInBackground(null, "Stopping Web App", false, true,
                        "Stopping Web " + "App...", () -> stopWebApp());
            }
        });
        addAction(ACTION_START, new NodeActionListener() {
            @Override
            public void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getIdeHelper().runInBackground(null, "Starting Web App", false, true,
                        "Starting Web " + "App...", () -> startWebApp());
            }
        });
        addAction(ACTION_RESTART, new NodeActionListener() {
            @Override
            public void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getIdeHelper().runInBackground(null, "Restarting Web App", false, true,
                        "Restarting Web" + " App...", () -> restartWebApp());
            }
        });

        addAction(ACTION_DELETE, new DeleteWebAppAction());

        // Open in browser action
        addAction(ACTION_OPEN_IN_BROWSER, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                String appServiceLink = String.format("http://%s.azurewebsites.net", getWebAppName());
                DefaultLoader.getUIHelper().openInBrowser(appServiceLink);
            }
        });

        super.loadActions();
    }

    @Override
    public String getRunState() {
        return this.runState;
    }

    @Override
    public void setRunState(String runState) {
        this.runState = runState;
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        properties.put(AppInsightsConstants.Region, this.propertyMap.get("regionName"));
        return properties;
    }

    @Override
    public String getSubscriptionId() {
        return this.subscriptionId;
    }

    @Override
    public String getWebAppId() {
        return this.webAppId;
    }

    @Override
    public String getWebAppName() {
        return this.webAppName;
    }

    @Override
    public void startWebApp() {
        try {
            WebAppModulePresenter.onStartWebApp(getSubscriptionId(), getWebAppId());
            setRunState(STATUS_RUNNING);
            setIconPath(WEB_RUN_ICON);
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: Error handling
        }
    }

    @Override
    public void restartWebApp() {
        try {
            WebAppModulePresenter.onRestartWebApp(getSubscriptionId(), getWebAppId());
            setRunState(STATUS_RUNNING);
            setIconPath(WEB_RUN_ICON);
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: Error handling
        }
    }

    @Override
    public void stopWebApp() {
        try {
            WebAppModulePresenter.onStopWebApp(getSubscriptionId(), getWebAppId());
            setRunState(STATUS_STOPPED);
            setIconPath(WEB_STOP_ICON);
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
        protected void azureNodeAction(NodeActionEvent e) throws AzureCmdException {
            getParent().removeNode(getSubscriptionId(), getWebAppId(), WebAppNode.this);
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
        }
    }
}
