/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.functionapp.logstreaming;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;
import com.microsoft.azure.toolkit.eclipse.common.logstream.EclipseAzureLogStreamingManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDraft;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.core.utils.PluginUtil;

import reactor.core.publisher.Flux;

public class FunctionAppLogStreamingHandler {
    private static final String APPINSIGHTS_INSTRUMENTATIONKEY = "APPINSIGHTS_INSTRUMENTATIONKEY";
    private static final String APPLICATION_INSIGHT_PATTERN = "%s/#blade/AppInsightsExtension/QuickPulseBladeV2/ComponentId/%s/ResourceId/%s";
    private static final String MUST_CONFIGURE_APPLICATION_INSIGHTS = AzureMessageBundle
            .message("appService.logStreaming.error.noApplicationInsights").toString();
    private static final String ENABLE_FILE_LOGGING = "Do you want to enable file logging for ({0})";
    private static final String ENABLE_LOGGING = "Enable logging";

    public static void stopLogStreaming(final FunctionAppBase<?, ?, ?> functionApp) {
        AzureTaskManager.getInstance()
                .runLater(() -> EclipseAzureLogStreamingManager.getInstance().stopLogStreaming(functionApp.id()));
    }

    public static void startLogStreaming(final FunctionAppBase<?, ?, ?> functionApp) {
        if (!isLogStreamingEnabled(functionApp)) {
            final boolean enableLogging = AzureMessager.getMessager()
                            .confirm(AzureString.format(ENABLE_FILE_LOGGING, functionApp.getName()), ENABLE_LOGGING);
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

    // Refers https://github.com/microsoft/vscode-azurefunctions/blob/v0.22.0.JAVA17/src/
    // commands/logstream/startStreamingLogs.ts#L53
    private static void openLiveMetricsStream(final FunctionAppBase<?, ?, ?> functionApp) throws IOException {
        final String aiKey = functionApp.getAppSettings().get(APPINSIGHTS_INSTRUMENTATIONKEY);
        if (StringUtils.isEmpty(aiKey)) {
            throw new IOException(MUST_CONFIGURE_APPLICATION_INSIGHTS);
        }
        final String subscriptionId = functionApp.getSubscriptionId();
        final ApplicationInsight insights = Azure.az(AzureApplicationInsights.class)
                .applicationInsights(subscriptionId).list().stream()
                .filter(entity -> StringUtils.equals(entity.getInstrumentationKey(), aiKey)).findFirst()
                .orElseThrow(() -> new IOException(AzureMessageBundle
                        .message("appService.logStreaming.error.aiNotFound", subscriptionId).toString()));
        final String aiUrl = getApplicationInsightLiveMetricsUrl(insights,
                Azure.az(AzureAccount.class).account().getPortalUrl());
        AzureTaskManager.getInstance().runLater(() -> PluginUtil.openLinkInBrowser(aiUrl));
    }

    private static String getApplicationInsightLiveMetricsUrl(ApplicationInsight target, String portalUrl)
            throws UnsupportedEncodingException {
        final JsonObject componentObject = new JsonObject();
        componentObject.addProperty("Name", target.getName());
        componentObject.addProperty("SubscriptionId", target.getSubscriptionId());
        componentObject.addProperty("ResourceGroup", target.getResourceGroupName());
        final String componentId = URLEncoder.encode(componentObject.toString(), "utf-8");
        final String aiResourceId = URLEncoder.encode(target.getId(), "utf-8");
        return String.format(APPLICATION_INSIGHT_PATTERN, portalUrl, componentId, aiResourceId);
    }

    private static boolean isLogStreamingEnabled(FunctionAppBase<?, ?, ?> functionApp) {
        final OperatingSystem operatingSystem = Optional.ofNullable(functionApp.getRuntime())
                .map(r -> r.getOperatingSystem()).orElse(null);
        final boolean isEnableApplicationLog = Optional.ofNullable(functionApp.getDiagnosticConfig())
                .map(DiagnosticConfig::isEnableApplicationLog).orElse(false);
        return operatingSystem == OperatingSystem.LINUX || isEnableApplicationLog;
    }

    private static void enableLogStreaming(FunctionAppBase<?, ?, ?> functionApp) {
        final DiagnosticConfig diagnosticConfig = Optional.ofNullable(functionApp.getDiagnosticConfig()).orElseGet(DiagnosticConfig::new);
        diagnosticConfig.setEnableApplicationLog(true);
        final FunctionAppDraft draft = (FunctionAppDraft) functionApp.update();
        draft.setDiagnosticConfig(diagnosticConfig);
        draft.commit();
    }
}
