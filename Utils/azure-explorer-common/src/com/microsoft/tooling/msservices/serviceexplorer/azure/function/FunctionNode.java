/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2020-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.function;

import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionEnvelope;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperationTitle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.BasicActionBuilder;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FunctionNode extends Node {

    private static final String ICON_FUNCTION_APP_RUNNING = "FunctionApp/FunctionAppRunning.svg";
    private static final String ICON_FUNCTION_APP_STOPPED = "FunctionApp/FunctionAppStopped.svg";

    private static final String SUB_FUNCTION_ICON_PATH = "FunctionApp/Function.svg";
    private static final String HTTP_TRIGGER_URL = "https://%s/api/%s";
    private static final String HTTP_TRIGGER_URL_WITH_CODE = "https://%s/api/%s?code=%s";
    private static final String NONE_HTTP_TRIGGER_URL = "https://%s/admin/functions/%s";
    private FunctionApp functionApp;
    private FunctionEnvelope functionEnvelope;
    private FunctionAppNode functionAppNode;

    public FunctionNode(FunctionEnvelope functionEnvelope, FunctionsNode parent) {
        super(functionEnvelope.inner().id(), getFunctionTriggerName(functionEnvelope), parent, SUB_FUNCTION_ICON_PATH);
        this.functionEnvelope = functionEnvelope;
        this.functionApp = parent.getFunctionApp();
    }

    public FunctionNode(FunctionEnvelope functionEnvelope, FunctionAppNode parent) {
        super(functionEnvelope.inner().id(), getFunctionTriggerName(functionEnvelope), parent, SUB_FUNCTION_ICON_PATH);
        this.functionEnvelope = functionEnvelope;
        this.functionApp = parent.getFunctionApp();
        this.functionAppNode = parent;
    }

    private BasicActionBuilder initActionBuilder(Runnable runnable) {
        return new BasicActionBuilder(runnable)
                .withModuleName(FunctionModule.MODULE_NAME)
                .withInstanceName(name);
    }

    @Override
    protected void loadActions() {
        addAction("Trigger Function", initActionBuilder(this::triggerFunction).withAction(AzureActionEnum.START).build());
        // todo: find whether there is sdk to enable/disable trigger
    }

    @AzureOperation(name = "function|trigger.start", type = AzureOperation.Type.ACTION)
    private void triggerFunction() {
        final IAzureOperationTitle title = AzureOperationBundle.title("function|trigger.start");
        AzureTaskManager.getInstance().runInBackground(new AzureTask(getProject(), title, false, () -> trigger()));
    }

    @AzureOperation(
        name = "function|trigger.start.detail",
        params = {"@functionApp.name()"},
        type = AzureOperation.Type.SERVICE
    )
    private void trigger() {
        final Map triggerBinding = getTriggerBinding();
        if (triggerBinding == null || !triggerBinding.containsKey("type")) {
            final String error = String.format("failed to get trigger type of function[%s].", functionApp.name());
            final String action = "confirm trigger type is configured.";
            throw new AzureToolkitRuntimeException(error, action);
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
                final String error = String.format("unknown trigger type[%s]", triggerType);
                final String action = "only HttpTrigger, TimerTrigger, EventHubTrigger is supported for now.";
                throw new AzureToolkitRuntimeException(error, action);
        }

    }

    // Refers https://docs.microsoft.com/mt-mt/Azure/azure-functions/functions-manually-run-non-http
    @AzureOperation(
        name = "function|trigger.start_timer",
        params = {"@functionApp.name()"},
        type = AzureOperation.Type.TASK
    )
    private void triggerTimerTrigger() {
        try {
            final HttpPost request = getFunctionTriggerRequest();
            final StringEntity entity = new StringEntity("{}");
            request.setEntity(entity);
            HttpClients.createDefault().execute(request);
        } catch (IOException e) {
            final String error = String.format("failed to trigger function[%s] with TimerTrigger", functionApp.name());
            throw new AzureToolkitRuntimeException(error, e);
        }
    }

    @AzureOperation(
        name = "function|trigger.start_event",
        params = {"@functionApp.name()"},
        type = AzureOperation.Type.TASK
    )
    private void triggerEventHubTrigger() {
        try {
            final HttpPost request = getFunctionTriggerRequest();
            final String value = DefaultLoader.getUIHelper().showInputDialog(tree.getParent(), "Please input test value: ", "Trigger Event Hub", null);
            final StringEntity entity = new StringEntity(String.format("{\"input\":\"'%s'\"}", value));
            request.setEntity(entity);
            HttpClients.createDefault().execute(request);
        } catch (IOException e) {
            final String error = String.format("failed to trigger function[%s] with EventHubTrigger", functionApp.name());
            throw new AzureToolkitRuntimeException(error, e);
        }
    }

    @AzureOperation(
        name = "function|trigger.start_http",
        params = {"@functionApp.name()"},
        type = AzureOperation.Type.TASK
    )
    private void triggerHttpTrigger(Map binding) {
        final AuthorizationLevel authLevel = EnumUtils.getEnumIgnoreCase(AuthorizationLevel.class, (String) binding.get("authLevel"));
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
                final String format = String.format("Unsupported authorization level %s", authLevel);
                throw new AzureToolkitRuntimeException(format);
        }
        DefaultLoader.getUIHelper().openInBrowser(targetUrl);
    }

    private String getAnonymousHttpTriggerUrl() {
        return String.format(HTTP_TRIGGER_URL, functionApp.defaultHostName(), this.name);
    }

    private String getFunctionHttpTriggerUrl() {
        // Linux function app doesn't support list function keys, use master key as workaround.
        if (functionApp.operatingSystem() == OperatingSystem.LINUX) {
            return getAdminHttpTriggerUrl();
        }
        final Map<String, String> keyMap = functionApp.listFunctionKeys(this.name);
        final String key = keyMap.values().stream().filter(StringUtils::isNotBlank)
                .findFirst().orElse(functionApp.getMasterKey());
        return String.format(HTTP_TRIGGER_URL_WITH_CODE, functionApp.defaultHostName(), this.name, key);
    }

    private String getAdminHttpTriggerUrl() {
        return String.format(HTTP_TRIGGER_URL_WITH_CODE, functionApp.defaultHostName(), this.name,
                functionApp.getMasterKey());
    }

    private HttpPost getFunctionTriggerRequest() {
        final String masterKey = functionApp.getMasterKey();
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
