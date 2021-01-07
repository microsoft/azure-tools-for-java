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
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryParameter;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file.AppServiceLogFilesRootNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file.AppServiceUserFilesRootNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotModule;
import com.microsoft.tooling.msservices.serviceexplorer.listener.ActionBackgroundable;
import com.microsoft.tooling.msservices.serviceexplorer.listener.ActionPromptable;
import com.microsoft.tooling.msservices.serviceexplorer.listener.ActionTelemetrable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebAppNode extends WebAppBaseNode implements WebAppNodeView {
    private static final String LABEL = "WebApp";
    public static final String SSH_INTO = "SSH into Web App (Preview)";
    public static final String PROFILE_FLIGHT_RECORDER = "Profile Flight Recorder";

    private final WebApp webapp;

    public WebAppNode(WebAppModule parent, String subscriptionId, WebApp delegate) {
        super(delegate.id(), delegate.name(), LABEL, parent, subscriptionId, delegate.defaultHostName(),
              delegate.operatingSystem().toString(), delegate.state());
        this.webapp = delegate;
        loadActions();
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        boolean isLinux = OS_LINUX.equalsIgnoreCase(webapp.operatingSystem().toString());
        boolean running = WebAppBaseState.RUNNING.equals(state);
        boolean updating = WebAppBaseState.UPDATING.equals(state);
        if (isLinux) {
            return running ? AzureIconSymbol.WebApp.RUNNING_ON_LINUX :
                    updating ? AzureIconSymbol.WebApp.UPDATING_ON_LINUX : AzureIconSymbol.WebApp.STOPPED_ON_LINUX;
        } else {
            return running ? AzureIconSymbol.WebApp.RUNNING : updating ? AzureIconSymbol.WebApp.UPDATING : AzureIconSymbol.WebApp.STOPPED;
        }
    }

    @Override
    @AzureOperation(value = "refresh content of web app", type = AzureOperation.Type.ACTION)
    protected void refreshItems() {
        this.renderSubModules();
    }

    @Override
    public void renderSubModules() {
        addChildNode(new DeploymentSlotModule(this, this.subscriptionId, this.webapp));
        addChildNode(new AppServiceUserFilesRootNode(this, this.subscriptionId, this.webapp));
        addChildNode(new AppServiceLogFilesRootNode(this, this.subscriptionId, this.webapp));
    }

    @Override
    protected void loadActions() {
        addAction(new StopAction().asGenericListener(AzureActionEnum.STOP));
        addAction(new StartAction().asGenericListener(AzureActionEnum.START));
        addAction(new RestartAction().asGenericListener(AzureActionEnum.RESTART));
        addAction(new DeleteAction().asGenericListener(AzureActionEnum.DELETE));
        addAction(new OpenInBrowserAction().asGenericListener(AzureActionEnum.OPEN_IN_BROWSER));
        addAction(new ShowPropertiesAction().asGenericListener(AzureActionEnum.SHOW_PROPERTIES));
        super.loadActions();
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

    // Delete action class
    private class DeleteAction extends NodeActionListener implements ActionBackgroundable, ActionPromptable, ActionTelemetrable {

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            WebAppNode.this.getParent().removeNode(WebAppNode.this.getSubscriptionId(), WebAppNode.this.getId(), WebAppNode.this);
        }

        @Override
        public String getPromptMessage() {
            return Node.getPromptMessage(AzureActionEnum.DELETE.getName().toLowerCase(), WebAppModule.MODULE_NAME, WebAppNode.this.name);
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.DELETE.getDoingName(), WebAppModule.MODULE_NAME, WebAppNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.WebApp.DELETE;
        }
    }

    // Start action class
    private class StartAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @AzureOperation(value = "start web app", type = AzureOperation.Type.ACTION)
        @Override
        protected void actionPerformed(NodeActionEvent e) {
            AzureWebAppMvpModel.getInstance().startWebApp(WebAppNode.this.subscriptionId, WebAppNode.this.webapp.id());
            WebAppNode.this.renderNode(WebAppBaseState.RUNNING);
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.START.getDoingName(), WebAppModule.MODULE_NAME, WebAppNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.WebApp.START;
        }
    }

    // Stop action class
    private class StopAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @AzureOperation(value = "stop web app", type = AzureOperation.Type.ACTION)
        @Override
        protected void actionPerformed(NodeActionEvent e) {
            AzureWebAppMvpModel.getInstance().stopWebApp(WebAppNode.this.subscriptionId, WebAppNode.this.webapp.id());
            WebAppNode.this.renderNode(WebAppBaseState.STOPPED);
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.STOP.getDoingName(), WebAppModule.MODULE_NAME, WebAppNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.WebApp.STOP;
        }

    }

    // Restart action class
    private class RestartAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @AzureOperation(value = "restart web app", type = AzureOperation.Type.ACTION)
        @Override
        protected void actionPerformed(NodeActionEvent e) {
            AzureWebAppMvpModel.getInstance().restartWebApp(WebAppNode.this.subscriptionId, WebAppNode.this.webapp.id());
            WebAppNode.this.renderNode(WebAppBaseState.RUNNING);
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.RESTART.getDoingName(), WebAppModule.MODULE_NAME, WebAppNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.WebApp.RESTART;
        }

    }

    // Open in browser action class
    private class OpenInBrowserAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @AzureOperation(value = "open web app in local browser", type = AzureOperation.Type.ACTION)
        @Override
        protected void actionPerformed(NodeActionEvent e) {
            DefaultLoader.getUIHelper().openInBrowser("http://" + WebAppNode.this.hostName);
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.OPEN_IN_BROWSER.getDoingName(), MySQLModule.MODULE_NAME, WebAppNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.WebApp.OPEN_IN_BROWSER;
        }
    }

    // Show properties
    private class ShowPropertiesAction extends NodeActionListener implements ActionTelemetrable {

        @AzureOperation(value = "show properties of web app", type = AzureOperation.Type.ACTION)
        @Override
        protected void actionPerformed(NodeActionEvent e) {
            DefaultLoader.getUIHelper().openWebAppPropertyView(WebAppNode.this);

        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.WebApp.SHOW_PROPERTIES;
        }
    }

}
