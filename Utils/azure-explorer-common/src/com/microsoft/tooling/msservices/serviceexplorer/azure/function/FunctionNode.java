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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionEnvelope;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.sdkmanage.AzureManager;
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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.*;

public class FunctionNode extends WebAppBaseNode implements FunctionNodeView {
    private static final Logger LOGGER = Logger.getLogger(FunctionNode.class.getName());
    private static final String DELETE_FUNCTION_PROMPT_MESSAGE = "This operation will delete the Function App: %s.\n" +
            "Are you sure you want to continue?";
    private static final String DELETE_FUNCTION_PROGRESS_MESSAGE = "Deleting Function App";
    private static final String FUNCTION_LABEL = "Function";
    private static final String FAILED_TO_START_FUNCTION_APP = "Failed to start function app %s";
    private static final String FAILED_TO_RESTART_FUNCTION_APP = "Failed to restart function app %s";
    private static final String FAILED_TO_STOP_FUNCTION_APP = "Failed to stop function app %s";
    private static final String FAILED_TO_LOAD_TRIGGERS = "Failed to load triggers of function %s";

    private final FunctionNodePresenter<FunctionNode> functionNodePresenter;
    private String functionAppName;
    private String functionAppId;
    private String region;
    private FunctionApp functionApp;
    private String cachedMasterKey;

    /**
     * Constructor.
     */
    public FunctionNode(AzureRefreshableNode parent, String subscriptionId, FunctionApp functionApp) {
        super(functionApp.id(), functionApp.name(), FUNCTION_LABEL, parent, subscriptionId,
                functionApp.defaultHostName(), functionApp.operatingSystem().toString(), functionApp.state());
        this.functionApp = functionApp;
        this.functionAppId = functionApp.id();
        this.functionAppName = functionApp.name();
        this.region = functionApp.regionName();
        functionNodePresenter = new FunctionNodePresenter<>();
        functionNodePresenter.onAttachView(FunctionNode.this);
        loadActions();
    }

    @Override
    public String getIconPath() {
        return this.state == WebAppBaseState.STOPPED ? "azure-functions-stop.png" : "azure-functions-small.png";
    }

    @Override
    protected void refreshItems() {
        try {
            functionNodePresenter.onRefreshFunctionNode(this.subscriptionId, this.functionAppId);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format(FAILED_TO_LOAD_TRIGGERS, functionAppName), e);
        }
    }

    @Override
    public void renderSubModules(List<FunctionEnvelope> functionEnvelopes) {
        if (functionEnvelopes.isEmpty()) {
            this.setName(this.functionAppName + " *(Empty)");
        } else {
            this.setName(this.functionAppName);
        }
        for (FunctionEnvelope functionEnvelope : functionEnvelopes) {
            addChildNode(new SubFunctionNode(functionEnvelope, this));
        }
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
        addAction("Open in portal", new WrappedTelemetryNodeActionListener(FUNCTION, OPEN_INBROWSER_FUNCTION_APP,
                new NodeActionListener() {
                    @Override
                    protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                        openResourcesInPortal(subscriptionId, functionAppId);
                    }
                }));
        addAction(ACTION_SHOW_PROPERTY, new WrappedTelemetryNodeActionListener(FUNCTION, SHOWPROP_FUNCTION_APP,
                new NodeActionListener() {
                    @Override
                    protected void actionPerformed(NodeActionEvent e) {
                        DefaultLoader.getUIHelper().openFunctionAppPropertyView(FunctionNode.this);
                    }
                }));
        super.loadActions();
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

    public void startFunctionApp() {
        try {
            functionNodePresenter.onStartFunctionApp(this.subscriptionId, this.functionAppId);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format(FAILED_TO_START_FUNCTION_APP, functionAppName), e);
        }
    }

    public void restartFunctionApp() {
        try {
            functionNodePresenter.onRestartFunctionApp(this.subscriptionId, this.functionAppId);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format(FAILED_TO_RESTART_FUNCTION_APP, functionAppName), e);
        }
    }

    public void stopFunctionApp() {
        try {
            functionNodePresenter.onStopFunctionApp(this.subscriptionId, this.functionAppId);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, String.format(FAILED_TO_STOP_FUNCTION_APP, functionAppName), e);
        }
    }

    // work around for API getMasterKey failed
    public synchronized String getFunctionMasterKey() throws IOException {
        if (StringUtils.isEmpty(cachedMasterKey)) {
            final AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            final String subscriptionId = getSegment(functionApp.id(), "subscriptions");
            final String resourceGroup = getSegment(functionApp.id(), "resourceGroups");
            final String tenant = azureManager.getTenantIdBySubscription(subscriptionId);
            final String authToken = azureManager.getAccessToken(tenant);
            final String targetUrl = String.format("https://management.azure.com/subscriptions/%s/resourceGroups/%s/" +
                            "providers/Microsoft.Web/sites/%s/host/default/listkeys?api-version=2019-08-01",
                    subscriptionId, resourceGroup, functionApp.name());

            final HttpPost request = new HttpPost(targetUrl);
            request.setHeader("Authorization", "Bearer " + authToken);
            CloseableHttpResponse response = HttpClients.createDefault().execute(request);
            JsonObject jsonObject = new Gson().fromJson(new InputStreamReader(response.getEntity().getContent()),
                    JsonObject.class);
            cachedMasterKey = jsonObject.get("masterKey").getAsString();
        }
        return cachedMasterKey;
    }

    // Todo: Extract this methods to common Utils
    private static String getSegment(String id, String segment) {
        if (StringUtils.isEmpty(id)) {
            return null;
        }
        final String[] attributes = id.split("/");
        int pos = ArrayUtils.indexOf(attributes, segment);
        if (pos >= 0) {
            return attributes[pos + 1];
        }
        return null;
    }

    private class DeleteFunctionAppAction extends AzureNodeActionPromptListener {
        DeleteFunctionAppAction() {
            super(FunctionNode.this, String.format(DELETE_FUNCTION_PROMPT_MESSAGE, getFunctionAppName()),
                    DELETE_FUNCTION_PROGRESS_MESSAGE);
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e) {
            getParent().removeNode(getSubscriptionId(), getFunctionAppId(), FunctionNode.this);
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
