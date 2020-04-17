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
package com.microsoft.intellij.helpers;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appservice.*;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import rx.Observable;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel.isApplicationLogEnabled;
import static com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel.isHttpLogEnabled;


public enum AppServiceStreamingLogManager {
    INSTANCE;

    private static final String STREAMING_LOG_NOT_STARTED = "Streaming log is not started.";
    private static final String ENABLE_FILE_LOGGING_PROMPT = "Do you want to enable file logging for %s";
    private static final String ENABLE_LOGGING = "Enable logging";
    private static final String[] YES_NO = {"Yes", "No"};
    private static final String STARTING_STREAMING_LOG = "Starting Streaming Log...";
    private static final String NOT_SUPPORTED = "Not supported";
    private static final String LINUX_LOG_STREAMING_IS_NOT_SUPPORTED =
            "Log streaming for Linux Function App is not supported in current version.";
    private static final String FAILED_TO_START_STREAMING_LOG = "Failed to start streaming log";
    private static final String FAILED_TO_CLOSE_STREAMING_LOG = "Failed to close streaming log";
    private static final String CLOSING_STREAMING_LOG = "Closing Streaming Log...";
    private static final String SITES = "sites";
    private static final String SUBSCRIPTIONS = "subscriptions";
    private static final String SLOTS = "slots";

    private Map<String, AppServiceStreamingLogConsoleView> consoleViewMap = new HashMap<>();

    public void showWebAppDeploymentSlotStreamingLog(Project project, String slotId) {
        final String slotName = AzureMvpModel.getSegment(slotId, SLOTS);
        showAppServiceStreamingLog(project, slotId, slotName, resourceId -> {
            final String subscriptionId = AzureMvpModel.getSegment(resourceId, SUBSCRIPTIONS);
            final String webAppId = resourceId.substring(0, resourceId.indexOf("/slots"));
            final WebApp webApp = AzureWebAppMvpModel.getInstance().getWebAppById(subscriptionId, webAppId);
            final DeploymentSlot deploymentSlot = webApp.deploymentSlots().getById(resourceId);
            if (isHttpLogEnabled(deploymentSlot) || enableHttpLog(deploymentSlot.update(), deploymentSlot.name())) {
                return deploymentSlot.streamAllLogsAsync();
            } else {
                return null;
            }
        });
    }

    public void showWebAppStreamingLog(Project project, String webAppId) {
        final String webAppName = AzureMvpModel.getSegment(webAppId, SITES);
        showAppServiceStreamingLog(project, webAppId, webAppName, resourceId -> {
            final String subscriptionId = AzureMvpModel.getSegment(resourceId, SUBSCRIPTIONS);
            final WebApp webApp = AzureWebAppMvpModel.getInstance().getWebAppById(subscriptionId, resourceId);
            if (isHttpLogEnabled(webApp) || enableHttpLog(webApp.update(), webApp.name())) {
                return webApp.streamAllLogsAsync();
            } else {
                return null;
            }
        });
    }

    public void showFunctionStreamingLog(Project project, String functionId) {
        final String functionName = AzureMvpModel.getSegment(functionId, SITES);
        showAppServiceStreamingLog(project, functionId, functionName, resourceId -> {
            final String subscriptionId = AzureMvpModel.getSegment(resourceId, SUBSCRIPTIONS);
            final FunctionApp function = AzureFunctionMvpModel.getInstance().getFunctionById(subscriptionId,
                                                                                             resourceId);
            if (function.operatingSystem() == OperatingSystem.LINUX) {
                // Todo: Open portal Application Insight pages for function logging
                DefaultLoader.getIdeHelper().invokeLater(() -> PluginUtil.displayInfoDialog(
                        NOT_SUPPORTED, LINUX_LOG_STREAMING_IS_NOT_SUPPORTED));
                return null;
            }
            if (isApplicationLogEnabled(function) || enableApplicationLog(function)) {
                return function.streamAllLogsAsync();
            } else {
                return null;
            }
        });
    }

    public void closeStreamingLog(Project project, String appId) {
        DefaultLoader.getIdeHelper().runInBackground(project, CLOSING_STREAMING_LOG, false, true, null, () -> {
            if (consoleViewMap.containsKey(appId) && consoleViewMap.get(appId).isActive()) {
                final AppServiceStreamingLogConsoleView consoleView = consoleViewMap.get(appId);
                consoleView.closeStreamingLog();
            } else {
                DefaultLoader.getIdeHelper().invokeLater(() -> PluginUtil.displayErrorDialog(
                        FAILED_TO_CLOSE_STREAMING_LOG, STREAMING_LOG_NOT_STARTED));
            }
        });
    }

    private void showAppServiceStreamingLog(Project project, String resourceId, String displayName,
                                            LogStreamingFunction logStreamingFunction) {
        DefaultLoader.getIdeHelper().runInBackground(project, STARTING_STREAMING_LOG, false, true, null, () -> {
            try {
                final AppServiceStreamingLogConsoleView consoleView = getOrCreateConsoleView(project, resourceId);
                if (!consoleView.isActive()) {
                    final Observable<String> log = logStreamingFunction.apply(resourceId);
                    if (log == null) {
                        return;
                    }
                    consoleView.startStreamingLog(logStreamingFunction.apply(resourceId));
                }
                StreamingLogsToolWindowManager.getInstance().showStreamingLogConsole(
                        project, resourceId, displayName, consoleView);
            } catch (Throwable e) {
                DefaultLoader.getIdeHelper().invokeLater(() -> PluginUtil.displayErrorDialog(
                        FAILED_TO_START_STREAMING_LOG, e.getMessage()));
            }
        });
    }

    private AppServiceStreamingLogConsoleView getOrCreateConsoleView(Project project, String resourceId) {
        if (!consoleViewMap.containsKey(resourceId) || consoleViewMap.get(resourceId).isDisposed()) {
            consoleViewMap.put(resourceId, new AppServiceStreamingLogConsoleView(project, resourceId));
        }
        return consoleViewMap.get(resourceId);
    }

    private boolean enableHttpLog(WebAppBase.Update webApp, String name) {
        final boolean enableLogStreaming = DefaultLoader.getUIHelper().showConfirmation(
                String.format(ENABLE_FILE_LOGGING_PROMPT, name), ENABLE_LOGGING, YES_NO, null);
        if (!enableLogStreaming) {
            return false;
        }
        AzureWebAppMvpModel.enableHttpLog(webApp);
        return true;
    }

    private boolean enableApplicationLog(FunctionApp functionApp) {
        final boolean enableLogStreaming = DefaultLoader.getUIHelper().showConfirmation(
                String.format(ENABLE_FILE_LOGGING_PROMPT, functionApp.name()), ENABLE_LOGGING, YES_NO, null);
        if (!enableLogStreaming) {
            return false;
        }
        AzureFunctionMvpModel.enableApplicationLog(functionApp);
        return true;
    }

    interface LogStreamingFunction {
        @Nullable
        Observable<String> apply(@Nullable String resourceId) throws Exception;
    }
}
