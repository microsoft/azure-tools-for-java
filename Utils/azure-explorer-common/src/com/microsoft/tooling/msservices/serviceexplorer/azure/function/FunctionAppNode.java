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
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.WrappedTelemetryNodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file.AppServiceLogFilesRootNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file.AppServiceUserFilesRootNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.DELETE_FUNCTION_APP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.FUNCTION;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.OPEN_INBROWSER_FUNCTION_APP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.RESTART_FUNCTION_APP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SHOWPROP_FUNCTION_APP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.START_FUNCTION_APP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.STOP_FUNCTION_APP;

public class FunctionAppNode extends WebAppBaseNode implements FunctionAppNodeView {
    private static final Logger LOGGER = Logger.getLogger(FunctionAppNode.class.getName());
    private static final String DELETE_FUNCTION_PROMPT_MESSAGE = "This operation will delete the Function App: %s.\n" +
            "Are you sure you want to continue?";
    private static final String DELETE_FUNCTION_PROGRESS_MESSAGE = "Deleting Function App";
    private static final String FUNCTION_LABEL = "Function";

    private final FunctionAppNodePresenter<FunctionAppNode> functionAppNodePresenter;
    private String functionAppName;
    private String functionAppId;
    private String region;
    private FunctionApp functionApp;

    /**
     * Constructor.
     */
    public FunctionAppNode(AzureRefreshableNode parent, String subscriptionId, FunctionApp functionApp) {
        super(functionApp.id(), functionApp.name(), FUNCTION_LABEL, parent, subscriptionId,
                functionApp.defaultHostName(), functionApp.operatingSystem().toString(), functionApp.state());
        this.functionApp = functionApp;
        this.functionAppId = functionApp.id();
        this.functionAppName = functionApp.name();
        this.region = functionApp.regionName();
        functionAppNodePresenter = new FunctionAppNodePresenter<>();
        functionAppNodePresenter.onAttachView(FunctionAppNode.this);
        loadActions();
    }

    @Override
    public String getIconPath() {
        return this.state == WebAppBaseState.STOPPED ? "azure-functions-stop.png" : "azure-functions-small.png";
    }

    @Override
    protected void refreshItems() {
        this.functionAppNodePresenter.onNodeRefresh();
    }

    @Override
    public void renderSubModules() {
        addChildNode(new FunctionsNode(this, this.functionApp));
        addChildNode(new AppServiceUserFilesRootNode(this, this.subscriptionId, this.functionApp));
        addChildNode(new AppServiceLogFilesRootNode(this, this.subscriptionId, this.functionApp));
    }

    @Override
    protected void loadActions() {
        addAction(ACTION_STOP, new WrappedTelemetryNodeActionListener(FUNCTION, STOP_FUNCTION_APP,
                createBackgroundActionListener("Stopping", () -> stopFunctionApp())));
        addAction(ACTION_START, new WrappedTelemetryNodeActionListener(FUNCTION, START_FUNCTION_APP,
                createBackgroundActionListener("Starting", () -> startFunctionApp())));
        addAction(ACTION_RESTART, new WrappedTelemetryNodeActionListener(FUNCTION, RESTART_FUNCTION_APP,
                createBackgroundActionListener("Restarting", () -> restartFunctionApp())));
        addAction(ACTION_DELETE, new DeleteFunctionAppAction());
        addAction("Open in portal", new WrappedTelemetryNodeActionListener(FUNCTION, OPEN_INBROWSER_FUNCTION_APP, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                openResourcesInPortal(subscriptionId, functionAppId);
            }
        }));
        addAction(ACTION_SHOW_PROPERTY, new WrappedTelemetryNodeActionListener(FUNCTION, SHOWPROP_FUNCTION_APP, new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                showProperties();
            }
        }));
        super.loadActions();
    }

    @AzureOperation(value = "show properties of web app", type = AzureOperation.Type.ACTION)
    private void showProperties() {
        DefaultLoader.getUIHelper().openFunctionAppPropertyView(FunctionAppNode.this);
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        properties.put(AppInsightsConstants.Region, region);
        return properties;
    }

    public FunctionApp getFunctionApp() {
        return functionApp;
    }

    public String getFunctionAppId() {
        return this.functionAppId;
    }

    public String getFunctionAppName() {
        return this.functionAppName;
    }

    @AzureOperation(value = "start function app", type = AzureOperation.Type.ACTION)
    public void startFunctionApp() {
        functionAppNodePresenter.onStartFunctionApp(this.subscriptionId, this.functionAppId);
    }

    @AzureOperation(value = "restart function app", type = AzureOperation.Type.ACTION)
    public void restartFunctionApp() {
        functionAppNodePresenter.onRestartFunctionApp(this.subscriptionId, this.functionAppId);
    }

    @AzureOperation(value = "stop function app", type = AzureOperation.Type.ACTION)
    public void stopFunctionApp() {
        functionAppNodePresenter.onStopFunctionApp(this.subscriptionId, this.functionAppId);
    }

    private class DeleteFunctionAppAction extends AzureNodeActionPromptListener {
        DeleteFunctionAppAction() {
            super(FunctionAppNode.this, String.format(DELETE_FUNCTION_PROMPT_MESSAGE, getFunctionAppName()),
                    DELETE_FUNCTION_PROGRESS_MESSAGE);
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e) {
            getParent().removeNode(getSubscriptionId(), getFunctionAppId(), FunctionAppNode.this);
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) {
        }

        @Override
        protected String getServiceName(NodeActionEvent event) {
            return FUNCTION;
        }

        @Override
        protected String getOperationName(NodeActionEvent event) {
            return DELETE_FUNCTION_APP;
        }
    }
}
