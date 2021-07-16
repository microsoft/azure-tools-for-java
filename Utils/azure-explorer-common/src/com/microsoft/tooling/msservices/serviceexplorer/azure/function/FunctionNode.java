/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.function;

import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.utils.Utils;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureText;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.WrappedTelemetryNodeActionListener;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.FUNCTION;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.TRIGGER_FUNCTION;

public class FunctionNode extends Node implements TelemetryProperties {

    private static final String SUB_FUNCTION_ICON_PATH = "azure-function-trigger-small.png";
    private static final String HTTP_TRIGGER_URL = "https://%s/api/%s";
    private static final String HTTP_TRIGGER_URL_WITH_CODE = "https://%s/api/%s?code=%s";

    private final IFunctionApp functionApp;
    private final FunctionEntity functionEntity;

    public FunctionNode(@Nonnull FunctionEntity functionEnvelope, @Nonnull IFunctionApp functionApp, @Nonnull FunctionsNode parent) {
        super(functionEnvelope.getTriggerId(), functionEnvelope.getName(), parent, SUB_FUNCTION_ICON_PATH);
        this.functionEntity = functionEnvelope;
        this.functionApp = functionApp;
    }

    @Override
    protected void loadActions() {
        addAction("Trigger Function", new WrappedTelemetryNodeActionListener(FUNCTION, TRIGGER_FUNCTION, new NodeActionListener() {
            @Override
            @AzureOperation(name = "function|trigger.start", type = AzureOperation.Type.ACTION)
            protected void actionPerformed(NodeActionEvent e) {
                final AzureText title = AzureOperationBundle.title("function|trigger.start");
                AzureTaskManager.getInstance().runInBackground(new AzureTask<>(getProject(), title, false, () -> trigger()));
            }
        }));
        // todo: find whether there is sdk to enable/disable trigger
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, Utils.getSubscriptionId(functionApp.id()));
        properties.put(AppInsightsConstants.Region, functionApp.entity().getRegion().getName());
        return properties;
    }

    @AzureOperation(
        name = "function|trigger.start.detail",
        params = {"this.functionApp.name()"},
        type = AzureOperation.Type.SERVICE
    )
    private void trigger() {
        final FunctionEntity.BindingEntity trigger = functionEntity.getTrigger();
        final String triggerType = Optional.ofNullable(trigger)
                .map(functionTrigger -> functionTrigger.getProperty("type")).orElse(null);
        if (StringUtils.isEmpty(triggerType)) {
            final String error = String.format("failed to get trigger type of function[%s].", functionApp.name());
            final String action = "confirm trigger type is configured.";
            throw new AzureToolkitRuntimeException(error, action);
        }
        switch (triggerType.toLowerCase()) {
            case "httptrigger":
                triggerHttpTrigger(trigger);
                break;
            case "timertrigger":
                functionApp.triggerFunction(this.name, new Object()); // no input for timer trigger
                break;
            default:
                final String input = DefaultLoader.getUIHelper().showInputDialog(tree.getParent(), "Please set the input value: ",
                        String.format("Trigger function %s", this.name), null);
                functionApp.triggerFunction(this.name, new TriggerRequest(input));
                break;
        }
    }

    @AzureOperation(
        name = "function|trigger.start_http",
        params = {"this.functionApp.name()"},
        type = AzureOperation.Type.TASK
    )
    private void triggerHttpTrigger(FunctionEntity.BindingEntity binding) {
        final AuthorizationLevel authLevel = EnumUtils.getEnumIgnoreCase(AuthorizationLevel.class, binding.getProperty("authLevel"));
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
        return String.format(HTTP_TRIGGER_URL, functionApp.hostName(), this.name);
    }

    private String getFunctionHttpTriggerUrl() {
        // Linux function app doesn't support list function keys, use master key as workaround.
        if (functionApp.getRuntime().getOperatingSystem() != OperatingSystem.WINDOWS) {
            return getAdminHttpTriggerUrl();
        }
        final String key = functionApp.listFunctionKeys(this.name).values().stream().filter(StringUtils::isNotBlank)
                .findFirst().orElse(functionApp.getMasterKey());
        return String.format(HTTP_TRIGGER_URL_WITH_CODE, functionApp.hostName(), this.name, key);
    }

    private String getAdminHttpTriggerUrl() {
        return String.format(HTTP_TRIGGER_URL_WITH_CODE, functionApp.hostName(), this.name, functionApp.getMasterKey());
    }

    @RequiredArgsConstructor
    static class TriggerRequest {
        private final String input;
    }
}
