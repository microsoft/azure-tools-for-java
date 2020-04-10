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
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionEnvelope;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.WrappedTelemetryNodeActionListener;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.FUNCTION;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.TRIGGER_FUNCTION;

public class SubFunctionNode extends Node {

    private static final String SUB_FUNCTION_ICON_PATH = "azure-function-trigger-small.png";
    private static final String HTTP_TRIGGER_URL = "https://%s/api/%s";
    private static final String HTTP_TRIGGER_URL_WITH_CODE = "https://%s/api/%s?code=%s";
    private static final String NONE_HTTP_TRIGGER_URL = "https://%s/admin/functions/%s";
    private static final String DEFAULT_FUNCTION_KEY = "default";
    private static final String MASTER_FUNCTION_KEY = "_master";

    private String cachedMasterKey;
    private FunctionApp functionApp;
    private FunctionEnvelope functionEnvelope;

    public SubFunctionNode(FunctionEnvelope functionEnvelope, FunctionNode parent) {
        super(functionEnvelope.inner().id(), getFunctionTriggerName(functionEnvelope), parent, SUB_FUNCTION_ICON_PATH);
        this.functionEnvelope = functionEnvelope;
        this.functionApp = parent.getFunctionApp();
    }

    @Override
    protected void loadActions() {
        addAction("Trigger Function",
                new WrappedTelemetryNodeActionListener(FUNCTION, TRIGGER_FUNCTION, new NodeActionListener() {
                    @Override
                    protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                        try {
                            DefaultLoader.getIdeHelper().runInBackground(getProject(), "Triggering Function",
                                    false, false, null, () -> trigger());
                        } catch (Exception exception) {
                            DefaultLoader.getUIHelper().showError(SubFunctionNode.this, exception.getMessage());
                        }
                    }
                }));
        // todo: find whether there is sdk to enable/disable trigger
    }

    private void trigger() {
        final Map triggerBinding = getTriggerBinding();
        if (triggerBinding == null || !triggerBinding.containsKey("type")) {
            DefaultLoader.getUIHelper().showError(this, String.format("Failed to get trigger of function %s", name));
            return;
        }
        final String triggerType = (String) triggerBinding.get("type");
        switch (triggerType.toLowerCase()) {
            case "httptrigger":
                triggerHttpTrigger(triggerBinding);
                break;
            case "timertrigger":
                triggerTimerTrigger();
                break;
            case "eventhubtrigger":
                triggerEventHubTrigger();
                break;
            default:
                DefaultLoader.getUIHelper().showInfo(this, String.format("%s is not supported for now.",
                        StringUtils.capitalize(triggerType)));
                break;
        }

    }

    // Refers https://docs.microsoft.com/mt-mt/Azure/azure-functions/functions-manually-run-non-http
    private void triggerTimerTrigger() {
        try {
            final HttpPost request = getFunctionTriggerRequest();
            final StringEntity entity = new StringEntity("{}");
            request.setEntity(entity);
            HttpClients.createDefault().execute(request);
        } catch (IOException e) {
            DefaultLoader.getUIHelper().showError(this,
                    String.format("Failed to trigger function %s, %s", this.name, e.getMessage()));
        }
    }

    private void triggerEventHubTrigger() {
        try {
            final HttpPost request = getFunctionTriggerRequest();
            final String value = JOptionPane.showInputDialog(tree.getParent(), "Please input test value: ");
            final StringEntity entity = new StringEntity(String.format("{\"input\":\"'%s'\"}", value));
            request.setEntity(entity);
            HttpClients.createDefault().execute(request);
        } catch (IOException e) {
            DefaultLoader.getUIHelper().showError(this,
                    String.format("Failed to trigger function %s, %s", this.name, e.getMessage()));
        }
    }

    private HttpPost getFunctionTriggerRequest() throws IOException {
        final String masterKey = getFunctionMasterKey();
        final String targetUrl = String.format(NONE_HTTP_TRIGGER_URL, functionApp.defaultHostName(), this.name);
        final HttpPost request = new HttpPost(targetUrl);
        request.setHeader("x-functions-key", masterKey);
        request.setHeader("Content-Type", "application/json");
        return request;
    }

    // work around for API getMasterKey failed
    private synchronized String getFunctionMasterKey() throws IOException {
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

    private void triggerHttpTrigger(Map binding) {
        final String authLevel = (String) binding.get("authLevel");
        try {
            final String url = StringUtils.equalsIgnoreCase(authLevel, AuthorizationLevel.ANONYMOUS.toString()) ?
                    getHttpTriggerUrl() : getHttpTriggerUrlWithCode();
            DefaultLoader.getUIHelper().openInBrowser(url);
        } catch (IOException e) {
            DefaultLoader.getUIHelper().showError(this,
                    String.format("Failed to get function key, %s", e.getMessage()));
        }
    }

    private String getHttpTriggerUrl() {
        return String.format(HTTP_TRIGGER_URL, functionApp.defaultHostName(), this.name);
    }

    private String getHttpTriggerUrlWithCode() throws IOException {
        final Map<String, String> keyMap = functionApp.listFunctionKeys(this.name);
        final String key = keyMap.values().stream().filter(StringUtils::isNotBlank)
                .findFirst().orElse(getFunctionMasterKey());
        return String.format(HTTP_TRIGGER_URL_WITH_CODE, functionApp.defaultHostName(), this.name, key);

    }

    private Map getTriggerBinding() {
        try {
            final List bindings = (List) ((Map) functionEnvelope.config()).get("bindings");
            return (Map) bindings.stream()
                    .filter(object -> object instanceof Map &&
                            StringUtils.equalsIgnoreCase((CharSequence) ((Map) object).get("direction"), "in"))
                    .filter(object ->
                            StringUtils.containsIgnoreCase((CharSequence) ((Map) object).get("type"), "trigger"))
                    .findFirst().orElse(null);
        } catch (ClassCastException | NullPointerException e) {
            // In case function.json lacks some parameters
            return null;
        }
    }

    private static String getFunctionTriggerName(FunctionEnvelope functionEnvelope) {
        if (functionEnvelope == null) {
            return null;
        }
        final String fullName = functionEnvelope.inner().name();
        final String[] splitNames = fullName.split("/");
        return splitNames.length > 1 ? splitNames[1] : fullName;
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
}
