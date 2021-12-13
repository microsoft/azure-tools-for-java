/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.functionapp.logstreaming;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;
import com.microsoft.azure.toolkit.eclipse.common.logstream.EclipseAzureLogStreamingManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsights;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsightsEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.FunctionApp;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.sdkmanage.IdentityAzureManager;

import reactor.core.publisher.Flux;

public class FunctionAppLogStreamingHandler {
    private static final String APPINSIGHTS_INSTRUMENTATIONKEY = "APPINSIGHTS_INSTRUMENTATIONKEY";
    private static final String APPLICATION_INSIGHT_PATTERN = "%s/#blade/AppInsightsExtension/QuickPulseBladeV2/ComponentId/%s/ResourceId/%s";
    private static final String MUST_CONFIGURE_APPLICATION_INSIGHTS = AzureMessageBundle
            .message("appService.logStreaming.error.noApplicationInsights").toString();
    private static final String ENABLE_FILE_LOGGING = "Do you want to enable file logging for ({0})";
    private static final String ENABLE_LOGGING = "Enable logging";

    public static void stopLogStreaming(final IFunctionAppBase<?> functionApp) {
        AzureTaskManager.getInstance()
                .runLater(() -> EclipseAzureLogStreamingManager.getInstance().stopLogStreaming(functionApp.id()));
    }

    public static void startLogStreaming(final IFunctionAppBase<?> functionApp) {
        if (!isLogStreamingEnabled(functionApp)) {
            final boolean enableLogging = AzureTaskManager.getInstance()
                    .runAndWaitAsObservable(new AzureTask<>(() -> AzureMessager.getMessager()
                            .confirm(AzureString.format(ENABLE_FILE_LOGGING, functionApp.name()), ENABLE_LOGGING)))
                    .toBlocking().single();
            if (enableLogging) {
                enableLogStreaming(functionApp);
            } else {
                return;
            }
        }
        if (functionApp.getRuntime().getOperatingSystem() == OperatingSystem.LINUX) {
            try {
                openLiveMetricsStream(functionApp);
            } catch (IOException e) {
                AzureTaskManager.getInstance().runLater(() -> AzureMessager.getMessager().warning(e.getMessage()));
            }
        } else {
            final Flux<String> log = functionApp.streamAllLogsAsync();
            AzureTaskManager.getInstance().runLater(
                    () -> EclipseAzureLogStreamingManager.getInstance().showLogStreaming(functionApp.id(), log));
        }
    }

    // Refers https://github.com/microsoft/vscode-azurefunctions/blob/v0.22.0/src/
    // commands/logstream/startStreamingLogs.ts#L53
    private static void openLiveMetricsStream(final IFunctionAppBase<?> functionApp) throws IOException {
        final String aiKey = functionApp.entity().getAppSettings().get(APPINSIGHTS_INSTRUMENTATIONKEY);
        if (StringUtils.isEmpty(aiKey)) {
            throw new IOException(MUST_CONFIGURE_APPLICATION_INSIGHTS);
        }
        final String subscriptionId = functionApp.subscriptionId();
        final ApplicationInsightsEntity insights = Azure.az(ApplicationInsights.class).list().stream()
                .filter(entity -> StringUtils.equals(entity.getInstrumentationKey(), aiKey)).findFirst()
                .orElseThrow(() -> new IOException(AzureMessageBundle
                        .message("appService.logStreaming.error.aiNotFound", subscriptionId).toString()));
        final String aiUrl = getApplicationInsightLiveMetricsUrl(insights,
                IdentityAzureManager.getInstance().getPortalUrl());
        AzureTaskManager.getInstance().runLater(() -> PluginUtil.openLinkInBrowser(aiUrl));
    }

    private static String getApplicationInsightLiveMetricsUrl(ApplicationInsightsEntity target, String portalUrl) {
        final JsonObject componentObject = new JsonObject();
        componentObject.addProperty("Name", target.getName());
        componentObject.addProperty("SubscriptionId", target.getSubscriptionId());
        componentObject.addProperty("ResourceGroup", target.getResourceGroup());
        final String componentId = URLEncoder.encode(componentObject.toString(), StandardCharsets.UTF_8);
        final String aiResourceId = URLEncoder.encode(target.getId(), StandardCharsets.UTF_8);
        return String.format(APPLICATION_INSIGHT_PATTERN, portalUrl, componentId, aiResourceId);
    }

    private static boolean isLogStreamingEnabled(IFunctionAppBase<?> functionApp) {
        return functionApp.getRuntime().getOperatingSystem() == OperatingSystem.LINUX
                || functionApp.getDiagnosticConfig().isEnableApplicationLog();
    }

    private static void enableLogStreaming(IFunctionAppBase<?> functionApp) {
        final DiagnosticConfig diagnosticConfig = functionApp.getDiagnosticConfig();
        diagnosticConfig.setEnableApplicationLog(true);
        if (functionApp instanceof FunctionApp) {
            ((FunctionApp) functionApp).update().withDiagnosticConfig(diagnosticConfig).commit();
        }
    }
}
