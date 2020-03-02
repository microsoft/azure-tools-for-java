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
import com.microsoft.azure.management.appservice.FunctionEnvelope;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.WrappedTelemetryNodeActionListener;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.FUNCTION;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.TRIGGER_FUNCTION;

public class SubFunctionNode extends Node {

    private static final String SUB_FUNCTION_ICON_PATH = "azure-function-trigger-small.png";
    private static final String HTTP_TRIGGER_URL = "https://%s/api/%s";
    private static final String HTTP_TRIGGER_URL_WITH_CODE = "https://%s/api/%s?code=%s";
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
                            trigger();
                        } catch (RuntimeException exception) {
                            DefaultLoader.getUIHelper().showError(SubFunctionNode.this, exception.getMessage());
                        }
                    }
                }));
        // todo: find whether there is sdk to enable/disable trigger
    }

    private void trigger() {
        Map binding = getHTTPTriggerBinding();
        if (binding == null) {
            DefaultLoader.getUIHelper().showInfo(this, "Only HTTP Trigger is supported for now");
        }
        final String authLevel = (String) binding.get("authLevel");
        final String url = StringUtils.equalsIgnoreCase(authLevel, "ANONYMOUS") ? getHttpTriggerUrl() : getHttpTriggerUrlWithCode();
        DefaultLoader.getUIHelper().openInBrowser(url);
    }

    private String getHttpTriggerUrl() {
        return String.format(HTTP_TRIGGER_URL, functionApp.defaultHostName(), this.name);
    }

    private String getHttpTriggerUrlWithCode() {
        final String key = functionApp.getMasterKey();
        return String.format(HTTP_TRIGGER_URL_WITH_CODE, functionApp.defaultHostName(), this.name, key);
    }

    private Map getHTTPTriggerBinding() {
        try {
            final List bindings = (List) ((Map) functionEnvelope.config()).get("bindings");
            return (Map) bindings.stream()
                    .filter(object -> object instanceof Map && StringUtils.equalsIgnoreCase((CharSequence) ((Map) object).get("direction"), "in"))
                    .filter(object -> StringUtils.equalsIgnoreCase((CharSequence) ((Map) object).get("type"), "httpTrigger"))
                    .findFirst().orElse(null);
        } catch (ClassCastException | NullPointerException e) {
            // In case function.json lack some parameters
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
