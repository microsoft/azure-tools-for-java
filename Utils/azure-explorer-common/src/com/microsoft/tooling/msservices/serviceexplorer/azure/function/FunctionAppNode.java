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

package com.microsoft.tooling.msservices.serviceexplorer.azure.function;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryParameter;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file.AppServiceLogFilesRootNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file.AppServiceUserFilesRootNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState;
import com.microsoft.tooling.msservices.serviceexplorer.listener.ActionBackgroundable;
import com.microsoft.tooling.msservices.serviceexplorer.listener.ActionPromptable;
import com.microsoft.tooling.msservices.serviceexplorer.listener.ActionTelemetrable;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class FunctionAppNode extends WebAppBaseNode implements FunctionAppNodeView {
    private static final Logger LOGGER = Logger.getLogger(FunctionAppNode.class.getName());
    private static final String DELETE_FUNCTION_PROMPT_MESSAGE = "This operation will delete the Function App: %s.\n" +
            "Are you sure you want to continue?";
    private static final String DELETE_FUNCTION_PROGRESS_MESSAGE = "Deleting Function App";
    private static final String FUNCTION_LABEL = "Function";

    private FunctionApp functionApp;

    public FunctionAppNode(AzureRefreshableNode parent, String subscriptionId, FunctionApp functionApp) {
        super(functionApp.id(), functionApp.name(), FUNCTION_LABEL, parent, subscriptionId,
                functionApp.defaultHostName(), functionApp.operatingSystem().toString(), functionApp.state());
        this.functionApp = functionApp;
        loadActions();
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        boolean running = WebAppBaseState.RUNNING.equals(state);
        boolean updating = WebAppBaseState.UPDATING.equals(state);
        return running ? AzureIconSymbol.FunctionApp.RUNNING : updating ? AzureIconSymbol.FunctionApp.UPDATING : AzureIconSymbol.FunctionApp.STOPPED;
    }

    @Override
    protected void refreshItems() {
        this.renderSubModules();
    }

    @Override
    public void renderSubModules() {
        addChildNode(new FunctionsNode(this, this.functionApp));
        addChildNode(new AppServiceUserFilesRootNode(this, this.subscriptionId, this.functionApp));
        addChildNode(new AppServiceLogFilesRootNode(this, this.subscriptionId, this.functionApp));
    }

    @Override
    protected void loadActions() {
        addAction(new StartAction().asGenericListener(AzureActionEnum.START));
        addAction(new StopAction().asGenericListener(AzureActionEnum.STOP));
        addAction(new RestartAction().asGenericListener(AzureActionEnum.RESTART));
        addAction(new DeleteAction().asGenericListener(AzureActionEnum.DELETE));
        addAction(new OpenInPortalAction().asGenericListener(AzureActionEnum.OPEN_IN_PORTAL));
        addAction(new ShowPropertiesAction().asGenericListener(AzureActionEnum.SHOW_PROPERTIES));
        super.loadActions();
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        properties.put(AppInsightsConstants.Region, getRegion());
        return properties;
    }

    public FunctionApp getFunctionApp() {
        return functionApp;
    }

    public String getFunctionAppId() {
        return this.functionApp.id();
    }

    public String getFunctionAppName() {
        return this.functionApp.name();
    }

    public String getRegion() {
        return this.functionApp.regionName();
    }

    // Delete action class
    private class DeleteAction extends NodeActionListener implements ActionBackgroundable, ActionPromptable, ActionTelemetrable {

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            FunctionAppNode.this.getParent()
                    .removeNode(FunctionAppNode.this.getSubscriptionId(), FunctionAppNode.this.getFunctionAppId(), FunctionAppNode.this);
        }

        @Override
        public String getPromptMessage() {
            return Node.getPromptMessage(AzureActionEnum.DELETE.getName().toLowerCase(), FunctionModule.MODULE_NAME, FunctionAppNode.this.name);
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.DELETE.getDoingName(), FunctionModule.MODULE_NAME, FunctionAppNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.FunctionApp.DELETE;
        }
    }

    private class StartAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @AzureOperation(value = "start function app", type = AzureOperation.Type.ACTION)
        @Override
        protected void actionPerformed(NodeActionEvent e) {
            AzureFunctionMvpModel.getInstance().startFunction(subscriptionId, FunctionAppNode.this.getFunctionAppId());
            FunctionApp target = AzureFunctionMvpModel.getInstance().getFunctionById(subscriptionId, FunctionAppNode.this.getFunctionAppId());
            FunctionAppNode.this.renderNode(WebAppBaseState.fromString(target.state()));
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.START.getDoingName(), FunctionModule.MODULE_NAME, FunctionAppNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.FunctionApp.START;
        }
    }

    private class StopAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @AzureOperation(value = "stop function app", type = AzureOperation.Type.ACTION)
        @Override
        protected void actionPerformed(NodeActionEvent e) {
            AzureFunctionMvpModel.getInstance().stopFunction(subscriptionId, FunctionAppNode.this.getFunctionAppId());
            FunctionApp target = AzureFunctionMvpModel.getInstance().getFunctionById(subscriptionId, FunctionAppNode.this.getFunctionAppId());
            FunctionAppNode.this.renderNode(WebAppBaseState.fromString(target.state()));
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.STOP.getDoingName(), FunctionModule.MODULE_NAME, FunctionAppNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.FunctionApp.STOP;
        }

    }

    // restart action class
    private class RestartAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @AzureOperation(value = "restart function app", type = AzureOperation.Type.ACTION)
        @Override
        protected void actionPerformed(NodeActionEvent e) {
            AzureFunctionMvpModel.getInstance().restartFunction(subscriptionId, FunctionAppNode.this.getFunctionAppId());
            FunctionApp target = AzureFunctionMvpModel.getInstance().getFunctionById(subscriptionId, FunctionAppNode.this.getFunctionAppId());
            FunctionAppNode.this.renderNode(WebAppBaseState.fromString(target.state()));
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.RESTART.getDoingName(), FunctionModule.MODULE_NAME, FunctionAppNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.FunctionApp.RESTART;
        }

    }

    // Open in browser action class
    private class OpenInPortalAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            FunctionAppNode.this.openResourcesInPortal(subscriptionId, FunctionAppNode.this.getFunctionAppId());
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.OPEN_IN_PORTAL.getDoingName(), FunctionModule.MODULE_NAME, FunctionAppNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.FunctionApp.OPEN_IN_PORTAL;
        }
    }

    // Show properties
    private class ShowPropertiesAction extends NodeActionListener implements ActionTelemetrable {

        @AzureOperation(value = "show properties of function app", type = AzureOperation.Type.ACTION)
        @Override
        protected void actionPerformed(NodeActionEvent e) {
            DefaultLoader.getUIHelper().openFunctionAppPropertyView(FunctionAppNode.this);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.FunctionApp.SHOW_PROPERTIES;
        }
    }
}
