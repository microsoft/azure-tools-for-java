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

import com.microsoft.azure.management.appservice.*;
import com.microsoft.azuretools.core.mvp.model.appserviceplan.AzureAppServicePlanMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file.AppServiceLogFilesRootNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file.AppServiceUserFilesRootNode;
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
    public static final String SSH_INTO = "SSH into Web App (Preview)";
    public static final String PROFILE_FLIGHT_RECORDER = "Profile Flight Recorder";

    private final WebAppNodePresenter<WebAppNode> webAppNodePresenter;
    private final WebApp webapp;

    /**
     * Constructor.
     */
    public WebAppNode(WebAppModule parent, String subscriptionId, WebApp delegate) {
        super(delegate.id(), delegate.name(), LABEL, parent, subscriptionId, delegate.defaultHostName(),
                delegate.operatingSystem().toString(), delegate.state());
        this.webapp = delegate;
        webAppNodePresenter = new WebAppNodePresenter<>();
        webAppNodePresenter.onAttachView(WebAppNode.this);
        loadActions();
    }

    @Override
    protected NodeActionListener getStartActionListener() {
        return createBackgroundActionListener("Starting Web App", this::startWebApp);
    }

    @Override
    protected NodeActionListener getRestartActionListener() {
        return createBackgroundActionListener("Restarting Web App", this::restartWebApp);
    }

    @Override
    protected NodeActionListener getStopActionListener() {
        return createBackgroundActionListener("Stopping Web App", this::stopWebApp);
    }

    @Override
    protected NodeActionListener getShowPropertiesActionListener() {
        return new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getUIHelper().openWebAppPropertyView(WebAppNode.this);
            }
        };
    }

    @Override
    protected NodeActionListener getOpenInBrowserActionListener() {
        return new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getUIHelper().openInBrowser("http://" + hostName);
            }
        };
    }

    @Override
    protected NodeActionListener getDeleteActionListener() {
        return new DeleteWebAppAction();
    }

    @Override
    protected void refreshItems() {
        webAppNodePresenter.onNodeRefresh();
    }

    @Override
    public void renderSubModules() {
        boolean isDeploymentSlotSupported = isDeploymentSlotSupported(this.subscriptionId, this.webapp);
        addChildNode(new DeploymentSlotModule(this, this.subscriptionId, this.webapp, isDeploymentSlotSupported));
        addChildNode(new AppServiceUserFilesRootNode(this, this.subscriptionId, this.webapp));
        addChildNode(new AppServiceLogFilesRootNode(this, this.subscriptionId, this.webapp));
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

    public void startWebApp() {
        try {
            webAppNodePresenter.onStartWebApp(this.subscriptionId, this.webapp.id());
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: Error handling
        }
    }

    public void restartWebApp() {
        try {
            webAppNodePresenter.onRestartWebApp(this.subscriptionId, this.webapp.id());
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: Error handling
        }
    }

    public void stopWebApp() {
        try {
            webAppNodePresenter.onStopWebApp(this.subscriptionId, this.webapp.id());
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: Error handling
        }
    }

    @Override
    public List<NodeAction> getNodeActions() {
        boolean running = this.state == WebAppBaseState.RUNNING;
        NodeAction sshNodeAction = getNodeActionByName(SSH_INTO);

        List<NodeAction> nodeActions = super.getNodeActions();
        if (sshNodeAction != null && this.webapp.operatingSystem() != OperatingSystem.LINUX)
            nodeActions.remove(sshNodeAction);

        if (sshNodeAction != null)
            sshNodeAction.setEnabled(running);

        NodeAction profileNodeAction = getNodeActionByName(PROFILE_FLIGHT_RECORDER);
        if (profileNodeAction != null)
            // TODO: Ignore the linuxFxVersion check since it is a heavy call that make an extra request
            //  and cause context menu show delay or failure (if no internet). We could replace it with constructor parameter
            //  that will be executed on module refresh, but it will slow down showing items in the Web App module node
            //  because of an extra call for each Web App node.
            profileNodeAction.setEnabled(running);// && !StringUtils.containsIgnoreCase(this.webapp.linuxFxVersion(), "DOCKER|"));

        return nodeActions;
    }

    public WebApp getWebapp() {
        return webapp;
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
