package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.util.Map;

public abstract class WebAppNode extends Node implements TelemetryProperties, WebAppVirtualInterface {
    protected static final String ACTION_START = "Start";
    protected static final String ACTION_STOP = "Stop";
    protected static final String ACTION_RESTART = "Restart";
    protected static final String WEB_RUN_ICON = "WebAppRunning_16.png";
    protected static final String WEB_STOP_ICON = "WebAppStopped_16.png";
    protected String resourceGroupName;
    protected String subscriptionId;

    public WebAppNode(String id, String name, Node parent, String iconPath, boolean delayActionLoading) {
        super(id, name, parent, iconPath, delayActionLoading);
    }

    abstract public WebApp getWebApp();

    @Override
    public Map<String, String> toProperties() {
        return null;
    }

    @Override
    protected void loadActions() {
        addAction(ACTION_STOP, WEB_STOP_ICON, new NodeActionListener() {
            @Override
            public void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getIdeHelper().runInBackground(null, "Stopping Web App", false, true, "Stopping Web " +
                        "App...", new Runnable() {
                    @Override
                    public void run() {
                        stopWebApp();
                        setIconPath(WEB_STOP_ICON);
                    }
                });
            }
        });
        addAction("Start", new NodeActionListener() {
            @Override
            public void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getIdeHelper().runInBackground(null, "Starting Web App", false, true, "Starting Web " +
                        "App...", new Runnable() {
                    @Override
                    public void run() {
                        startWebApp();
                        setIconPath(WEB_RUN_ICON);
                    }
                });
            }
        });
        addAction(ACTION_RESTART, new NodeActionListener() {
            @Override
            public void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getIdeHelper().runInBackground(null, "Restarting Web App", false, true, "Restarting Web" +
                        " App...", new Runnable() {
                    @Override
                    public void run() {
                        restartWebApp();
                        setIconPath(WEB_RUN_ICON);
                    }
                });
            }
        });
        addAction("Delete", new DeleteWebAppAction());
        super.loadActions();
    }

    private class DeleteWebAppAction extends AzureNodeActionPromptListener {
        DeleteWebAppAction() {
            super(WebAppNode.this,
                    String.format("This operation will delete Web App on Linux %s.\nAre you sure you want to " +
                                    "continue?", "",
                            getWebAppName()),
                    "Deleting Web App on Linux");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e) throws AzureCmdException {
            try {
                AzureWebAppMvpModel.getInstance().deleteWebApp(subscriptionId, getWebAppId());

                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        // instruct parent node to remove this node
                        getParent().removeDirectChildNode(WebAppNode.this);
                    }
                });
            } catch (Exception ex) {
                DefaultLoader.getUIHelper().showException("An error occurred while attempting to delete the Web App " +
                                "on Linux",
                        ex, "Azure Services Explorer - Error Deleting Web App on Linux", false, true);
            }
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
        }
    }
}
