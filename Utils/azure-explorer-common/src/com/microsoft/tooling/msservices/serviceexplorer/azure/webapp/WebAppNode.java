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
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Groupable;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.Sortable;
import com.microsoft.tooling.msservices.serviceexplorer.WrappedTelemetryNodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file.AppServiceLogFilesRootNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file.AppServiceUserFilesRootNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotModule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.DELETE_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.RESTART_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.START_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.STOP_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP_OPEN_INBROWSER;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP_SHOWPROP;

public class WebAppNode extends WebAppBaseNode implements WebAppNodeView {
    private static final String DELETE_WEBAPP_PROMPT_MESSAGE = "This operation will delete the Web App: %s.\n"
        + "Are you sure you want to continue?";
    private static final String DELETE_WEBAPP_PROGRESS_MESSAGE = "Deleting Web App";
    private static final String LABEL = "WebApp";
    public static final String SSH_INTO = "SSH into Web App (Preview)";
    public static final String PROFILE_FLIGHT_RECORDER = "Profile Flight Recorder";

    private final WebAppNodePresenter<WebAppNode> webAppNodePresenter;
    private final WebApp webapp;

    public WebAppNode(WebAppModule parent, String subscriptionId, WebApp delegate) {
        super(delegate.id(), delegate.name(), LABEL, parent, subscriptionId, delegate.defaultHostName(),
              delegate.operatingSystem().toString(), delegate.state());
        this.webapp = delegate;
        webAppNodePresenter = new WebAppNodePresenter<>();
        webAppNodePresenter.onAttachView(WebAppNode.this);
        loadActions();
    }

    @Override
    @AzureOperation(value = "refresh content of web app", type = AzureOperation.Type.ACTION)
    protected void refreshItems() {
        webAppNodePresenter.onNodeRefresh();
    }

    @Override
    public void renderSubModules() {
        addChildNode(new DeploymentSlotModule(this, this.subscriptionId, this.webapp));
        addChildNode(new AppServiceUserFilesRootNode(this, this.subscriptionId, this.webapp));
        addChildNode(new AppServiceLogFilesRootNode(this, this.subscriptionId, this.webapp));
    }

    @Override
    protected void loadActions() {
        final NodeActionListener stopping_web_app = createBackgroundActionListener("Stopping Web App", this::stopWebApp);
        final WrappedTelemetryNodeActionListener actionListener = new WrappedTelemetryNodeActionListener(WEBAPP, STOP_WEBAPP, stopping_web_app);
        addAction(ACTION_STOP, getIcon(this.os, this.label, WebAppBaseState.STOPPED), actionListener, Groupable.MAINTENANCE_GROUP, Sortable.HIGH_PRIORITY);
        final NodeActionListener starting_web_app = createBackgroundActionListener("Starting Web App", this::startWebApp);
        addAction(ACTION_START, new WrappedTelemetryNodeActionListener(
                WEBAPP, START_WEBAPP, starting_web_app, Groupable.MAINTENANCE_GROUP, Sortable.HIGH_PRIORITY));
        final NodeActionListener restarting_web_app = createBackgroundActionListener("Restarting Web App", this::restartWebApp);
        addAction(ACTION_RESTART, new WrappedTelemetryNodeActionListener(WEBAPP, RESTART_WEBAPP, restarting_web_app, Groupable.MAINTENANCE_GROUP));
        addAction(ACTION_DELETE, new DeleteWebAppAction());
        final NodeActionListener openBrowserListener = new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                openInBrowser();
            }
        };
        addAction(ACTION_OPEN_IN_BROWSER, new WrappedTelemetryNodeActionListener(WEBAPP, WEBAPP_OPEN_INBROWSER, openBrowserListener));
        final NodeActionListener showPropListener = new NodeActionListener() {
            protected void actionPerformed(NodeActionEvent e) {
                showProperties();
            }
        };
        addAction(ACTION_SHOW_PROPERTY, null, new WrappedTelemetryNodeActionListener(WEBAPP, WEBAPP_SHOWPROP, showPropListener));
        super.loadActions();
    }

    @AzureOperation(value = "show properties of web app", type = AzureOperation.Type.ACTION)
    private void showProperties() {
        DefaultLoader.getUIHelper().openWebAppPropertyView(WebAppNode.this);
    }

    @AzureOperation(value = "open web app in local browser", type = AzureOperation.Type.ACTION)
    private void openInBrowser() {
        DefaultLoader.getUIHelper().openInBrowser("http://" + hostName);
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        properties.put(AppInsightsConstants.Region, this.webapp.regionName());
        return properties;
    }

    public String getWebAppId() {
        return this.webapp.id();
    }

    public String getWebAppName() {
        return this.webapp.name();
    }

    public String getFxVersion() {
        return this.webapp.linuxFxVersion();
    }

    @AzureOperation(value = "start web app", type = AzureOperation.Type.ACTION)
    public void startWebApp() {
        webAppNodePresenter.onStartWebApp(this.subscriptionId, this.webapp.id());
    }

    @AzureOperation(value = "restart web app", type = AzureOperation.Type.ACTION)
    public void restartWebApp() {
        webAppNodePresenter.onRestartWebApp(this.subscriptionId, this.webapp.id());
    }

    @AzureOperation(value = "stop web app", type = AzureOperation.Type.ACTION)
    public void stopWebApp() {
        webAppNodePresenter.onStopWebApp(this.subscriptionId, this.webapp.id());
    }

    @Override
    public List<NodeAction> getNodeActions() {
        boolean running = this.state == WebAppBaseState.RUNNING;
        getNodeActionByName(SSH_INTO).setEnabled(running);
        getNodeActionByName(PROFILE_FLIGHT_RECORDER).setEnabled(running);
        return super.getNodeActions();
    }

    public WebApp getWebapp() {
        return webapp;
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

        @Override
        public int getGroup() {
            return Groupable.MAINTENANCE_GROUP;
        }

        @Override
        public int getPriority() {
            return Sortable.LOW_PRIORITY;
        }
    }
}
