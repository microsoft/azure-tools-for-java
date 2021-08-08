/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.appservice;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceUtils;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.ApplicationInsightsComponent;
import com.microsoft.azure.toolkit.intellij.common.StreamingLogsToolWindowManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.sdkmanage.IdentityAzureManager;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;


public enum AppServiceStreamingLogManager {
    INSTANCE;

    private static final String STREAMING_LOG_NOT_STARTED = message("appService.logStreaming.hint.notStart");
    private static final String FAILED_TO_START_STREAMING_LOG = message("appService.logStreaming.error.startFailed");
    private static final String FAILED_TO_CLOSE_STREAMING_LOG = message("appService.logStreaming.error.closeFailed");
    private static final String ENABLE_LOGGING = "Enable logging";
    private static final String NOT_SUPPORTED = "Not supported";
    private static final String SITES = "sites";
    private static final String SLOTS = "slots";
    private static final String SUBSCRIPTIONS = "subscriptions";
    private static final String[] YES_NO = {"Yes", "No"};

    private final Map<String, AppServiceStreamingLogConsoleView> consoleViewMap = new HashMap<>();

    public void showWebAppDeploymentSlotStreamingLog(Project project, String slotId) {
        showAppServiceStreamingLog(project, slotId, new WebAppSlotLogStreaming(slotId));
    }

    public void showWebAppStreamingLog(Project project, String webAppId) {
        showAppServiceStreamingLog(project, webAppId, new WebAppLogStreaming(webAppId));
    }

    public void showFunctionStreamingLog(Project project, String functionId) {
        showAppServiceStreamingLog(project, functionId, new FunctionLogStreaming(functionId));
    }

    @AzureOperation(name = "appservice|log_stream.close", params = {"nameFromResourceId(appId)"}, type = AzureOperation.Type.SERVICE)
    public void closeStreamingLog(Project project, String appId) {
        final AzureString title = AzureOperationBundle.title("appservice|log_stream.close", ResourceUtils.nameFromResourceId(appId));
        AzureTaskManager.getInstance().runInBackground(new AzureTask(project, title, false, () -> {
            if (consoleViewMap.containsKey(appId) && consoleViewMap.get(appId).isActive()) {
                consoleViewMap.get(appId).closeStreamingLog();
            } else {
                DefaultLoader.getIdeHelper().invokeLater(() -> PluginUtil.displayErrorDialog(
                        FAILED_TO_CLOSE_STREAMING_LOG, STREAMING_LOG_NOT_STARTED));
            }
        }));
    }

    @AzureOperation(name = "appservice|log_stream.open", params = {"nameFromResourceId(resourceId)"}, type = AzureOperation.Type.SERVICE)
    private void showAppServiceStreamingLog(Project project, String resourceId, ILogStreaming logStreaming) {
        final AzureString title = AzureOperationBundle.title("appservice|log_stream.open", ResourceUtils.nameFromResourceId(resourceId));
        AzureTaskManager.getInstance().runInBackground(new AzureTask(project, title, false, () -> {
            try {
                final String name = logStreaming.getTitle();
                final AppServiceStreamingLogConsoleView consoleView = getOrCreateConsoleView(project, resourceId);
                if (!consoleView.isActive()) {
                    if (!logStreaming.isLogStreamingSupported()) {
                        DefaultLoader.getIdeHelper().invokeLater(() -> PluginUtil.displayInfoDialog(
                                NOT_SUPPORTED, message("appService.logStreaming.hint.notSupport", name)));
                        return;
                    }
                    if (!logStreaming.isLogStreamingEnabled()) {
                        // Enable Log Streaming if log streaming of target is not enabled
                        final boolean userInput = DefaultLoader.getUIHelper().showConfirmation(
                            message("appService.logStreaming.hint.enablePrompt", name), ENABLE_LOGGING, YES_NO, null);
                        if (userInput) {
                            logStreaming.enableLogStreaming();
                        } else {
                            return;
                        }
                    }
                    final Flux<String> log = logStreaming.getStreamingLogContent();
                    if (log == null) {
                        return;
                    }
                    consoleView.startStreamingLog(log);
                }
                StreamingLogsToolWindowManager.getInstance().showStreamingLogConsole(
                        project, resourceId, logStreaming.getTitle(), consoleView);
            } catch (final Throwable e) {
                throw new AzureToolkitRuntimeException("failed to open streaming log", e);
            }
        }));
    }

    private AppServiceStreamingLogConsoleView getOrCreateConsoleView(Project project, String resourceId) {
        return consoleViewMap.compute(resourceId,
                                      (id, view) -> (view == null || view.isDisposed()) ? new AppServiceStreamingLogConsoleView(project, id) : view);
    }

    interface ILogStreaming {
        default boolean isLogStreamingSupported() {
            return true;
        }

        boolean isLogStreamingEnabled() throws IOException;

        void enableLogStreaming() throws IOException;

        String getTitle() throws IOException;

        Flux<String> getStreamingLogContent() throws IOException;
    }

    static class FunctionLogStreaming implements ILogStreaming {

        private static final String APPINSIGHTS_INSTRUMENTATIONKEY = "APPINSIGHTS_INSTRUMENTATIONKEY";
        private static final String APPLICATION_INSIGHT_PATTERN = "%s/#blade/AppInsightsExtension/QuickPulseBladeV2/ComponentId/%s/ResourceId/%s";
        private static final String MUST_CONFIGURE_APPLICATION_INSIGHTS = message("appService.logStreaming.error.noApplicationInsights");

        private final String resourceId;
        private IFunctionApp functionApp;

        FunctionLogStreaming(final String resourceId) {
            this.resourceId = resourceId;
        }

        @Override
        public boolean isLogStreamingEnabled() {
            return getFunctionApp().getRuntime().getOperatingSystem() == OperatingSystem.LINUX ||
                    getFunctionApp().getDiagnosticConfig().isEnableApplicationLog();
        }

        @Override
        public void enableLogStreaming() {
            final DiagnosticConfig diagnosticConfig = getFunctionApp().getDiagnosticConfig();
            diagnosticConfig.setEnableApplicationLog(true);
            getFunctionApp().update().withDiagnosticConfig(diagnosticConfig).commit();
        }

        @Override
        public String getTitle() {
            return AzureMvpModel.getSegment(resourceId, SITES);
        }

        @Override
        public Flux<String> getStreamingLogContent() throws IOException {
            if (getFunctionApp().getRuntime().getOperatingSystem() == OperatingSystem.LINUX) {
                // For linux function, we will just open the "Live Metrics Stream" view in the portal
                openLiveMetricsStream();
                return null;
            }
            return getFunctionApp().streamAllLogsAsync();
        }

        // Refers https://github.com/microsoft/vscode-azurefunctions/blob/v0.22.0/src/
        // commands/logstream/startStreamingLogs.ts#L53
        private void openLiveMetricsStream() throws IOException {
            final String aiKey = functionApp.entity().getAppSettings().get(APPINSIGHTS_INSTRUMENTATIONKEY);
            if (StringUtils.isEmpty(aiKey)) {
                throw new IOException(MUST_CONFIGURE_APPLICATION_INSIGHTS);
            }
            final String subscriptionId = AzureMvpModel.getSegment(resourceId, SUBSCRIPTIONS);
            final List<ApplicationInsightsComponent> insightsResources =
                    subscriptionId == null ? Collections.EMPTY_LIST : AzureSDKManager.getInsightsResources(subscriptionId);
            final ApplicationInsightsComponent target = insightsResources
                    .stream()
                    .filter(aiResource -> StringUtils.equals(aiResource.instrumentationKey(), aiKey))
                    .findFirst()
                    .orElseThrow(() -> new IOException(message("appService.logStreaming.error.aiNotFound", subscriptionId)));
            final String aiUrl = getApplicationInsightLiveMetricsUrl(target, IdentityAzureManager.getInstance().getPortalUrl());
            DefaultLoader.getIdeHelper().openLinkInBrowser(aiUrl);
        }

        private String getApplicationInsightLiveMetricsUrl(ApplicationInsightsComponent target, String portalUrl) {
            final JsonObject componentObject = new JsonObject();
            componentObject.addProperty("Name", target.name());
            componentObject.addProperty("SubscriptionId", AzureMvpModel.getSegment(target.id(), SUBSCRIPTIONS));
            componentObject.addProperty("ResourceGroup", target.resourceGroupName());
            final String componentId = URLEncoder.encode(componentObject.toString(), StandardCharsets.UTF_8);
            final String aiResourceId = URLEncoder.encode(target.id(), StandardCharsets.UTF_8);
            return String.format(APPLICATION_INSIGHT_PATTERN, portalUrl, componentId, aiResourceId);
        }

        private IFunctionApp getFunctionApp() {
            if (functionApp == null) {
                functionApp = Azure.az(AzureAppService.class).functionApp(resourceId);
            }
            return functionApp;
        }
    }

    static class WebAppLogStreaming implements ILogStreaming {
        private final IWebApp webApp;

        public WebAppLogStreaming(String resourceId) {
            this.webApp = Azure.az(AzureAppService.class).webapp(resourceId);
        }

        @Override
        public boolean isLogStreamingEnabled() {
            return webApp.getDiagnosticConfig().isEnableWebServerLogging();
        }

        @Override
        public void enableLogStreaming() {
            final DiagnosticConfig diagnosticConfig = webApp.getDiagnosticConfig();
            webApp.update().withDiagnosticConfig(enableHttpLog(diagnosticConfig)).commit();
        }

        @Override
        public String getTitle() {
            return AzureMvpModel.getSegment(webApp.id(), SITES);
        }

        @Override
        public Flux<String> getStreamingLogContent() {
            return webApp.streamAllLogsAsync();
        }
    }

    static class WebAppSlotLogStreaming implements ILogStreaming {
        private final IWebAppDeploymentSlot deploymentSlot;

        public WebAppSlotLogStreaming(String resourceId) {
            this.deploymentSlot = Azure.az(AzureAppService.class).deploymentSlot(resourceId);
        }

        @Override
        public boolean isLogStreamingEnabled() {
            return deploymentSlot.getDiagnosticConfig().isEnableWebServerLogging();
        }

        @Override
        public void enableLogStreaming() {
            final DiagnosticConfig diagnosticConfig = deploymentSlot.getDiagnosticConfig();
            deploymentSlot.update().withDiagnosticConfig(enableHttpLog(diagnosticConfig)).commit();
        }

        @Override
        public String getTitle() {
            return AzureMvpModel.getSegment(deploymentSlot.id(), SLOTS);
        }

        @Override
        public Flux<String> getStreamingLogContent() {
            return deploymentSlot.streamAllLogsAsync();
        }
    }

    // Refers values from Azure app service SDK
    // https://github.com/Azure/azure-sdk-for-java/blob/azure-resourcemanager-appservice_2.3.0/sdk/resourcemanager/azure-resourcemanager-appservice/src/
    // main/java/com/azure/resourcemanager/appservice/implementation/WebAppBaseImpl.java#L1565
    private static DiagnosticConfig enableHttpLog(DiagnosticConfig config) {
        config.setEnableWebServerLogging(true);
        config.setWebServerLogQuota(35);
        config.setWebServerRetentionPeriod(0);
        return config;
    }
}
