/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.legacy.appservice;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceUtils;
import com.google.gson.JsonObject;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.StreamingLogsToolWindowManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDraft;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlotDraft;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDraft;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;


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

    @AzureOperation(name = "appservice.close_log_stream.app", params = {"nameFromResourceId(appId)"}, type = AzureOperation.Type.SERVICE)
    public void closeStreamingLog(Project project, String appId) {
        final AzureString title = OperationBundle.description("appservice.close_log_stream.app", ResourceUtils.nameFromResourceId(appId));
        AzureTaskManager.getInstance().runInBackground(new AzureTask(project, title, false, () -> {
            if (consoleViewMap.containsKey(appId) && consoleViewMap.get(appId).isActive()) {
                consoleViewMap.get(appId).closeStreamingLog();
            } else {
                AzureTaskManager.getInstance().runLater(() -> AzureMessager.getMessager().error(STREAMING_LOG_NOT_STARTED, FAILED_TO_CLOSE_STREAMING_LOG));
            }
        }));
    }

    @AzureOperation(name = "appservice.open_log_stream.app", params = {"nameFromResourceId(resourceId)"}, type = AzureOperation.Type.SERVICE)
    private void showAppServiceStreamingLog(Project project, String resourceId, ILogStreaming logStreaming) {
        final Action<Void> retry = Action.retryFromFailure(() -> this.showAppServiceStreamingLog(project, resourceId, logStreaming));
        final AzureString title = OperationBundle.description("appservice.open_log_stream.app", ResourceUtils.nameFromResourceId(resourceId));
        AzureTaskManager.getInstance().runInBackground(new AzureTask(project, title, false, () -> {
            try {
                final String name = logStreaming.getTitle();
                final AppServiceStreamingLogConsoleView consoleView = getOrCreateConsoleView(project, resourceId);
                if (!consoleView.isActive()) {
                    if (!logStreaming.isLogStreamingSupported()) {
                        AzureTaskManager.getInstance().runLater(() -> AzureMessager.getMessager()
                                .error(message("appService.logStreaming.hint.notSupport", name), NOT_SUPPORTED));
                        return;
                    }
                    if (!logStreaming.isLogStreamingEnabled()) {
                        // Enable Log Streaming if log streaming of target is not enabled
                        final boolean userInput = AzureMessager.getMessager()
                                .confirm(message("appService.logStreaming.hint.enablePrompt", name), ENABLE_LOGGING);
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
                throw new AzureToolkitRuntimeException("failed to open streaming log", e, retry);
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

        boolean isLogStreamingEnabled();

        void enableLogStreaming();

        String getTitle();

        @Nullable
        Flux<String> getStreamingLogContent();
    }

    static class FunctionLogStreaming implements ILogStreaming {

        private static final String APPINSIGHTS_INSTRUMENTATIONKEY = "APPINSIGHTS_INSTRUMENTATIONKEY";
        private static final String APPLICATION_INSIGHT_PATTERN = "%s/#blade/AppInsightsExtension/QuickPulseBladeV2/ComponentId/%s/ResourceId/%s";
        private static final String MUST_CONFIGURE_APPLICATION_INSIGHTS = message("appService.logStreaming.error.noApplicationInsights");

        private final FunctionApp functionApp;

        FunctionLogStreaming(final String resourceId) {
            this.functionApp = Azure.az(AzureFunctions.class).functionApp(resourceId);
        }

        @Override
        public boolean isLogStreamingEnabled() {
            final OperatingSystem operatingSystem = Optional.ofNullable(functionApp.getRuntime()).map(Runtime::getOperatingSystem).orElse(null);
            final boolean isEnableApplicationLog = Optional.ofNullable(functionApp.getDiagnosticConfig())
                    .map(DiagnosticConfig::isEnableApplicationLog).orElse(false);
            return operatingSystem == OperatingSystem.LINUX || isEnableApplicationLog;
        }

        @Override
        public void enableLogStreaming() {
            final DiagnosticConfig diagnosticConfig = Optional.ofNullable(functionApp.getDiagnosticConfig()).orElseGet(DiagnosticConfig::new);
            diagnosticConfig.setEnableApplicationLog(true);
            final FunctionAppDraft draft = (FunctionAppDraft) functionApp.update();
            draft.setDiagnosticConfig(diagnosticConfig);
            draft.commit();
        }

        @Override
        public String getTitle() {
            return functionApp.getName();
        }

        @Override
        public Flux<String> getStreamingLogContent() {
            final OperatingSystem operatingSystem = Optional.ofNullable(functionApp.getRuntime()).map(Runtime::getOperatingSystem).orElse(null);
            if (operatingSystem == OperatingSystem.LINUX) {
                // For linux function, we will just open the "Live Metrics Stream" view in the portal
                openLiveMetricsStream();
                return null;
            }
            return functionApp.streamAllLogsAsync();
        }

        // Refers https://github.com/microsoft/vscode-azurefunctions/blob/v0.22.0.JAVA17/src/
        // commands/logstream/startStreamingLogs.ts#L53
        private void openLiveMetricsStream() {
            final String aiKey = Optional.ofNullable(functionApp.getAppSettings()).map(settings -> settings.get(APPINSIGHTS_INSTRUMENTATIONKEY)).orElse(null);
            if (StringUtils.isEmpty(aiKey)) {
                throw new AzureToolkitRuntimeException(MUST_CONFIGURE_APPLICATION_INSIGHTS);
            }
            final String subscriptionId = functionApp.getSubscriptionId();
            final List<ApplicationInsight> insightsResources = Azure.az(AzureApplicationInsights.class).applicationInsights(subscriptionId).list();
            final ApplicationInsight target = insightsResources
                    .stream()
                    .filter(aiResource -> StringUtils.equals(aiResource.getInstrumentationKey(), aiKey))
                    .findFirst()
                    .orElseThrow(() -> new AzureToolkitRuntimeException(message("appService.logStreaming.error.aiNotFound", subscriptionId)));
            final String aiUrl = getApplicationInsightLiveMetricsUrl(target, Azure.az(AzureAccount.class).account().getPortalUrl());
            BrowserUtil.browse(aiUrl);
        }

        private String getApplicationInsightLiveMetricsUrl(ApplicationInsight target, String portalUrl) {
            final JsonObject componentObject = new JsonObject();
            componentObject.addProperty("Name", target.getName());
            componentObject.addProperty("SubscriptionId", target.getSubscriptionId());
            componentObject.addProperty("ResourceGroup", target.getResourceGroupName());
            final String componentId = URLEncoder.encode(componentObject.toString(), StandardCharsets.UTF_8);
            final String aiResourceId = URLEncoder.encode(target.getId(), StandardCharsets.UTF_8);
            return String.format(APPLICATION_INSIGHT_PATTERN, portalUrl, componentId, aiResourceId);
        }
    }

    static class WebAppLogStreaming implements ILogStreaming {
        private final WebApp webApp;

        public WebAppLogStreaming(String resourceId) {
            this.webApp = Azure.az(AzureWebApp.class).webApp(resourceId);
        }

        @Override
        public boolean isLogStreamingEnabled() {
            return Optional.ofNullable(webApp.getDiagnosticConfig()).map(DiagnosticConfig::isEnableWebServerLogging).orElse(false);
        }

        @Override
        public void enableLogStreaming() {
            final DiagnosticConfig diagnosticConfig = Optional.ofNullable(webApp.getDiagnosticConfig()).orElseGet(DiagnosticConfig::new);
            final WebAppDraft draft = (WebAppDraft) webApp.update();
            draft.setDiagnosticConfig(enableHttpLog(diagnosticConfig));
            draft.commit();
        }

        @Override
        public String getTitle() {
            return webApp.getName();
        }

        @Override
        public Flux<String> getStreamingLogContent() {
            return webApp.streamAllLogsAsync();
        }
    }

    static class WebAppSlotLogStreaming implements ILogStreaming {
        private final WebAppDeploymentSlot deploymentSlot;

        public WebAppSlotLogStreaming(String resourceId) {
            final ResourceId id = ResourceId.fromString(resourceId);
            this.deploymentSlot = Optional.ofNullable(Azure.az(AzureWebApp.class).webApp(id.parent().id())).map(webapp -> webapp.slots().get(resourceId))
                    .orElseThrow(() -> new AzureToolkitRuntimeException("Target deployment slot does not exist"));
        }

        @Override
        public boolean isLogStreamingEnabled() {
            return Optional.ofNullable(deploymentSlot.getDiagnosticConfig()).map(DiagnosticConfig::isEnableWebServerLogging).orElse(false);
        }

        @Override
        public void enableLogStreaming() {
            final DiagnosticConfig diagnosticConfig = Optional.ofNullable(deploymentSlot.getDiagnosticConfig()).orElseGet(DiagnosticConfig::new);
            final WebAppDeploymentSlotDraft draft = (WebAppDeploymentSlotDraft) deploymentSlot.update();
            draft.setDiagnosticConfig(enableHttpLog(diagnosticConfig));
            draft.commit();
        }

        @Override
        public String getTitle() {
            return deploymentSlot.getName();
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
