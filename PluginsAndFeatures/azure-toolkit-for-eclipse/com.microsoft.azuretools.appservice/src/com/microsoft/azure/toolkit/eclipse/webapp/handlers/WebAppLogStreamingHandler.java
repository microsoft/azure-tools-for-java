/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.webapp.handlers;

import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.eclipse.common.logstream.EclipseAzureLogStreamingManager;

import reactor.core.publisher.Flux;

public class WebAppLogStreamingHandler {
    private static final String ENABLE_FILE_LOGGING = "Do you want to enable file logging for ({0})";
    private static final String ENABLE_LOGGING = "Enable logging";

    public static void stopLogStreaming(final IWebAppBase<?> webApp) {
        AzureTaskManager.getInstance()
                .runLater(() -> EclipseAzureLogStreamingManager.getInstance().stopLogStreaming(webApp.id()));
    }

    public static void startLogStreaming(final IWebAppBase<?> webApp) {
        if (!isLogStreamingEnabled(webApp)) {
            final boolean enableLogging = AzureTaskManager.getInstance()
                    .runAndWaitAsObservable(new AzureTask<>(() -> AzureMessager.getMessager()
                            .confirm(AzureString.format(ENABLE_FILE_LOGGING, webApp.name()), ENABLE_LOGGING)))
                    .toBlocking().single();
            if (enableLogging) {
                enableLogStreaming(webApp);
            } else {
                return;
            }
        }
        final Flux<String> log = webApp.streamAllLogsAsync();
        if (log == null) {
            return;
        }
        AzureTaskManager.getInstance().runLater(
                () -> EclipseAzureLogStreamingManager.getInstance().showLogStreaming(webApp.id(), log));
    }

    private static boolean isLogStreamingEnabled(IWebAppBase<?> webApp) {
        return webApp.getDiagnosticConfig().isEnableWebServerLogging();
    }

    private static void enableLogStreaming(IWebAppBase<?> webApp) {
        final DiagnosticConfig diagnosticConfig = webApp.getDiagnosticConfig();
        diagnosticConfig.setEnableWebServerLogging(true);
        diagnosticConfig.setWebServerLogQuota(35);
        diagnosticConfig.setWebServerRetentionPeriod(0);
        if (webApp instanceof IWebApp) {
            ((IWebApp) webApp).update().withDiagnosticConfig(diagnosticConfig).commit();
        } else if (webApp instanceof IWebAppDeploymentSlot) {
            ((IWebAppDeploymentSlot) webApp).update().withDiagnosticConfig(diagnosticConfig).commit();
        }
    }
}
