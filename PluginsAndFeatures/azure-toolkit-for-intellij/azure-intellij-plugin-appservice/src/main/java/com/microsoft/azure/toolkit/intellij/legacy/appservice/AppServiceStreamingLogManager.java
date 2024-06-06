/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.legacy.appservice;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceUtils;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.streaminglog.StreamingLogsConsoleView;
import com.microsoft.azure.toolkit.intellij.common.streaminglog.StreamingLogsManager;
import com.microsoft.azure.toolkit.intellij.common.streaminglog.StreamingLogsToolWindowManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlotDraft;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDraft;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.webapp.*;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
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

    private final Map<String, StreamingLogsConsoleView> consoleViewMap = new HashMap<>();

    public void showWebAppDeploymentSlotStreamingLog(Project project, String slotId) {
        showAppServiceStreamingLog(project, slotId, new WebAppSlotLogStreaming(slotId));
    }

    public void showWebAppStreamingLog(Project project, String webAppId) {
        showAppServiceStreamingLog(project, webAppId, new WebAppLogStreaming(webAppId));
    }

    public void showFunctionStreamingLog(Project project, FunctionAppBase<?,?,?> app) {
        showAppServiceStreamingLog(project, app.getId(), new FunctionLogStreaming(app));
    }

    public void showFunctionDeploymentSlotStreamingLog(Project project, FunctionAppBase<?,?,?> app) {
        showAppServiceStreamingLog(project, app.getId(), new FunctionDeploymentSlotLogStreaming(app));
    }

    public void closeStreamingLog(Project project, String appId) {
        StreamingLogsManager.getInstance().closeStreamingLog(project, appId);
    }

    private void showAppServiceStreamingLog(Project project, String resourceId, ILogStreaming logStreaming) {
        final Action<Void> retry = Action.retryFromFailure(() -> this.showAppServiceStreamingLog(project, resourceId, logStreaming));
        final AzureString title = OperationBundle.description("user/$appservice.open_log_stream.app", ResourceUtils.nameFromResourceId(resourceId));
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, title, false, () -> {
            try {
                final String name = logStreaming.getTitle();
                final StreamingLogsConsoleView consoleView = getOrCreateConsoleView(project, resourceId);
                if (!consoleView.isActive()) {
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
                AzureTaskManager.getInstance().runLater(() ->
                    StreamingLogsToolWindowManager.getInstance().showStreamingLogConsole(project, resourceId, logStreaming.getTitle(), consoleView)
                );
            } catch (final AzureToolkitRuntimeException e) {
                throw e;
            } catch (final Throwable e) {
                throw new AzureToolkitRuntimeException("failed to open streaming log", e, retry);
            }
        }));
    }

    private StreamingLogsConsoleView getOrCreateConsoleView(Project project, String resourceId) {
        return consoleViewMap.compute(resourceId,
            (id, view) -> (view == null || view.isDisposed()) ? new StreamingLogsConsoleView(project) : view);
    }

    interface ILogStreaming {
        boolean isLogStreamingEnabled();

        void enableLogStreaming();

        String getTitle();

        @Nullable
        Flux<String> getStreamingLogContent();
    }

    static abstract class AbstractFunctionLogStreaming implements ILogStreaming {

        private static final String APPINSIGHTS_INSTRUMENTATIONKEY = "APPINSIGHTS_INSTRUMENTATIONKEY";
        private static final String APPLICATION_INSIGHT_PATTERN = "%s/#blade/AppInsightsExtension/QuickPulseBladeV2/ComponentId/%s/ResourceId/%s";
        private static final String MUST_CONFIGURE_APPLICATION_INSIGHTS = message("appService.logStreaming.error.noApplicationInsights");

        @Getter
        protected final FunctionAppBase<?, ?, ?> functionApp;

        AbstractFunctionLogStreaming(final FunctionAppBase<?,?,?> functionApp) {
            this.functionApp = functionApp;
        }

        @Override
        public boolean isLogStreamingEnabled() {
            return true;
        }

        @Override
        public String getTitle() {
            return functionApp.getName();
        }

        @Override
        public Flux<String> getStreamingLogContent() {
            final OperatingSystem operatingSystem = Optional.ofNullable(functionApp.getRuntime()).map(Runtime::getOperatingSystem).orElse(null);
            if (operatingSystem == OperatingSystem.LINUX || operatingSystem == OperatingSystem.DOCKER) {
                // For linux function, we will just open the "Live Metrics Stream" view in the portal
                openLiveMetricsStream();
                return null;
            }
            return functionApp.streamingLogs(true);
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
            AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(aiUrl);
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

    static class FunctionLogStreaming extends AbstractFunctionLogStreaming {

        FunctionLogStreaming(FunctionAppBase<?, ?, ?> functionApp) {
            super(functionApp);
        }

        @Override
        public void enableLogStreaming() {
            final DiagnosticConfig diagnosticConfig = Optional.ofNullable(getFunctionApp().getDiagnosticConfig()).orElseGet(DiagnosticConfig::new);
            diagnosticConfig.setEnableApplicationLog(true);
            final FunctionAppDraft draft = (FunctionAppDraft) getFunctionApp().update();
            draft.setDiagnosticConfig(diagnosticConfig);
            draft.commit();
        }
    }

    static class FunctionDeploymentSlotLogStreaming extends AbstractFunctionLogStreaming {

        FunctionDeploymentSlotLogStreaming(FunctionAppBase<?,?,?> functionApp) {
            super(functionApp);
        }

        @Override
        public void enableLogStreaming() {
            final DiagnosticConfig diagnosticConfig = Optional.ofNullable(getFunctionApp().getDiagnosticConfig()).orElseGet(DiagnosticConfig::new);
            diagnosticConfig.setEnableApplicationLog(true);
            final FunctionAppDeploymentSlotDraft draft = (FunctionAppDeploymentSlotDraft) getFunctionApp().update();
            draft.setDiagnosticConfig(diagnosticConfig);
            draft.commit();
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
            return webApp.streamingLogs(true);
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
            return deploymentSlot.streamingLogs(true);
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
