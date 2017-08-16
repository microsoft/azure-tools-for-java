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

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.util.List;
import java.util.Map;

public abstract class WebAppNode extends Node implements TelemetryProperties, WebAppVirtualInterface {
    private static final String ACTION_START = "Start";
    private static final String ACTION_STOP = "Stop";
    private static final String ACTION_RESTART = "Restart";
    private static final String RUN_STATUS = "Running";
    private static final String WEB_RUN_ICON = "WebAppRunning_16.png";
    private static final String WEB_STOP_ICON = "WebAppStopped_16.png";
    protected String subscriptionId;

    public WebAppNode(String id, String name, Node parent, String iconPath, boolean delayActionLoading) {
        super(id, name, parent, iconPath, delayActionLoading);
    }

    public abstract WebApp getWebApp();

    @Override
    public Map<String, String> toProperties() {
        return null;
    }

    @Override
    public List<NodeAction> getNodeActions() {
        boolean running = RUN_STATUS.equals(getRunState());
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
                        "Stopping Web " + "App...",
                        () -> {
                            stopWebApp();
                            setIconPath(WEB_STOP_ICON);
                        });
            }
        });
        addAction("Start", new NodeActionListener() {
            @Override
            public void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getIdeHelper().runInBackground(null, "Starting Web App", false, true,
                        "Starting Web " + "App...", () -> {
                            startWebApp();
                            setIconPath(WEB_RUN_ICON);
                        });
            }
        });
        addAction(ACTION_RESTART, new NodeActionListener() {
            @Override
            public void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getIdeHelper().runInBackground(null, "Restarting Web App", false, true,
                        "Restarting Web" + " App...", () -> {
                            restartWebApp();
                            setIconPath(WEB_RUN_ICON);
                        });
            }
        });
        addAction("Delete", new DeleteWebAppAction());
        super.loadActions();
    }

    private class DeleteWebAppAction extends AzureNodeActionPromptListener {
        DeleteWebAppAction() {
            super(WebAppNode.this,
                    String.format("This operation will delete Web App %s.\nAre you sure you want to continue?",
                            getWebAppName()),
                    "Deleting Web App");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e) throws AzureCmdException {
            try {
                AzureWebAppMvpModel.getInstance().deleteWebApp(subscriptionId, getWebAppId());

                DefaultLoader.getIdeHelper().invokeLater(() -> {
                    // instruct parent node to remove this node
                    getParent().removeDirectChildNode(WebAppNode.this);
                });
            } catch (Exception ex) {
                DefaultLoader.getUIHelper().showException("An error occurred while attempting to delete the Web App ",
                        ex, "Azure Services Explorer - Error Deleting Web App on Linux", false, true);
            }
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
        }
    }
}
