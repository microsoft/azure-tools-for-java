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

import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionEnvelope;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.WrappedTelemetryNodeActionListener;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.FUNCTION;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.TRIGGER_FUNCTION;

public class SubFunctionNode extends Node {

    private static final String SUB_FUNCTION_ICON_PATH = "azure-function-trigger-small.png";
    private static final String HTTP_TRIGGER_URL = "https://%s/api/%s";
    private static final String HTTP_TRIGGER_URL_WITH_CODE = "https://%s/api/%s?code=%s";
    private static final String NONE_HTTP_TRIGGER_URL = "https://%s/admin/functions/%s";

    private FunctionApp functionApp;
    private FunctionEnvelope functionEnvelope;
    private FunctionNode functionNode;

    public SubFunctionNode(FunctionEnvelope functionEnvelope, FunctionNode parent) {
        super(functionEnvelope.inner().id(), getFunctionTriggerName(functionEnvelope), parent, SUB_FUNCTION_ICON_PATH);
        this.functionEnvelope = functionEnvelope;
        this.functionApp = parent.getFunctionApp();
        this.functionNode = parent;
    }

    @Override
    protected void loadActions() {
        addAction("Trigger Function",
                new WrappedTelemetryNodeActionListener(FUNCTION, TRIGGER_FUNCTION, new NodeActionListener() {
                    @Override
                    protected void actionPerformed(NodeActionEvent e) {
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

    private void triggerHttpTrigger(Map binding) {
        try {
            final AuthorizationLevel authLevel = AuthorizationLevel.valueOf((String) binding.get("authLevel"));
            String targetUrl;
            switch (authLevel) {
                case ANONYMOUS:
                    targetUrl = getAnonymousHttpTriggerUrl();
                    break;
                case FUNCTION:
                    targetUrl = getFunctionHttpTriggerUrl();
                    break;
                case ADMIN:
                    targetUrl = getAdminHttpTriggerUrl();
                    break;
                default:
                    throw new IOException(String.format("Unsupported authorization level %s", authLevel));
            }
            DefaultLoader.getUIHelper().openInBrowser(targetUrl);
        } catch (IOException e) {
            DefaultLoader.getUIHelper().showError(this,
                    String.format("Failed to get function key, %s", e.getMessage()));
        }
    }

    private String getAnonymousHttpTriggerUrl() {
        return String.format(HTTP_TRIGGER_URL, functionApp.defaultHostName(), this.name);
    }

    private String getFunctionHttpTriggerUrl() throws IOException {
        // Linux function app doesn't support list function keys, use master key as workaround.
        if (functionApp.operatingSystem() == OperatingSystem.LINUX) {
            return getAdminHttpTriggerUrl();
        }
        final Map<String, String> keyMap = functionApp.listFunctionKeys(this.name);
        final String key = keyMap.values().stream().filter(StringUtils::isNotBlank)
                .findFirst().orElse(functionNode.getFunctionMasterKey());
        return String.format(HTTP_TRIGGER_URL_WITH_CODE, functionApp.defaultHostName(), this.name, key);
    }

    private String getAdminHttpTriggerUrl() throws IOException {
        return String.format(HTTP_TRIGGER_URL_WITH_CODE, functionApp.defaultHostName(), this.name,
                functionNode.getFunctionMasterKey());
    }

    private HttpPost getFunctionTriggerRequest() throws IOException {
        final String masterKey = functionNode.getFunctionMasterKey();
        final String targetUrl = String.format(NONE_HTTP_TRIGGER_URL, functionApp.defaultHostName(), this.name);
        final HttpPost request = new HttpPost(targetUrl);
        request.setHeader("x-functions-key", masterKey);
        request.setHeader("Content-Type", "application/json");
        return request;
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
}
