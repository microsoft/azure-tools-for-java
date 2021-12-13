/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.ide.appservice.function.node;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureFunction;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.FunctionApp;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Optional;

public class TriggerFunctionInBrowserAction {
    private static final String HTTP_TRIGGER = "httptrigger";
    private static final String HTTP_TRIGGER_URL = "https://%s/api/%s";
    private static final String HTTP_TRIGGER_URL_WITH_CODE = "https://%s/api/%s?code=%s";

    private final FunctionApp functionApp;
    private final FunctionEntity functionEntity;
    private final FunctionEntity.BindingEntity trigger;

    public static boolean isApplyFor(@Nonnull final FunctionEntity functionEntity) {
        final String triggerType = Optional.ofNullable(functionEntity.getTrigger())
                .map(functionTrigger -> functionTrigger.getProperty("type")).orElse(null);
        return StringUtils.equalsIgnoreCase(triggerType, HTTP_TRIGGER);
    }

    public TriggerFunctionInBrowserAction(@Nonnull final FunctionEntity functionEntity) {
        final String functionId = Optional.ofNullable(functionEntity.getFunctionAppId()).orElseGet(() ->
                ResourceId.fromString(functionEntity.getTriggerId()).parent().id());
        this.functionApp = Azure.az(AzureFunction.class).get(functionId);
        this.functionEntity = functionEntity;
        this.trigger = functionEntity.getTrigger();
        final String triggerType = Optional.ofNullable(trigger)
                .map(functionTrigger -> functionTrigger.getProperty("type")).orElse(null);
        if (StringUtils.isEmpty(triggerType)) {
            final String error = String.format("failed to get trigger type of function[%s].", functionEntity.getName());
            final String action = "confirm trigger type is configured.";
            throw new AzureToolkitRuntimeException(error, action);
        }
        if (!StringUtils.equalsIgnoreCase(triggerType, HTTP_TRIGGER)) {
            final String error = String.format("trigger function in browser is only supported for function with http trigger.");
            final String action = "change to use `trigger function` for function without http trigger";
            throw new AzureToolkitRuntimeException(error, action);
        }
    }

    @AzureOperation(
            name = "function.trigger_func_http.app",
            params = {"this.functionApp.name()"},
            type = AzureOperation.Type.TASK
    )
    public void trigger() {
        final AuthorizationLevel authLevel = EnumUtils.getEnumIgnoreCase(AuthorizationLevel.class, trigger.getProperty("authLevel"));
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
        AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(targetUrl);
    }

    private String getAnonymousHttpTriggerUrl() {
        return String.format(HTTP_TRIGGER_URL, functionApp.hostName(), functionEntity.getName());
    }

    private String getFunctionHttpTriggerUrl() {
        // Linux function app doesn't support list function keys, use master key as workaround.
        if (functionApp.getRuntime().getOperatingSystem() != OperatingSystem.WINDOWS) {
            return getAdminHttpTriggerUrl();
        }
        final String key = functionApp.listFunctionKeys(functionEntity.getName()).values().stream().filter(StringUtils::isNotBlank)
                .findFirst().orElse(functionApp.getMasterKey());
        return String.format(HTTP_TRIGGER_URL_WITH_CODE, functionApp.hostName(), functionEntity.getName(), key);
    }

    private String getAdminHttpTriggerUrl() {
        return String.format(HTTP_TRIGGER_URL_WITH_CODE, functionApp.hostName(), functionEntity.getName(), functionApp.getMasterKey());
    }
}
