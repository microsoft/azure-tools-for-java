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

import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import com.microsoft.applicationinsights.management.rest.model.Resource;
import com.microsoft.azure.management.appservice.*;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public enum AppServiceStreamingLogManager {
    INSTANCE;

    private static final String STREAMING_LOG_NOT_STARTED = "Streaming log is not started.";
    private static final String ENABLE_FILE_LOGGING_PROMPT = "Do you want to enable file logging for %s";
    private static final String ENABLE_LOGGING = "Enable logging";
    private static final String[] YES_NO = {"Yes", "No"};
    private static final String STARTING_STREAMING_LOG = "Starting Streaming Log...";
    private static final String NOT_SUPPORTED = "Not supported";
    private static final String LOG_STREAMING_IS_NOT_SUPPORTED =
            "Log streaming for %s is not supported in current version.";
    private static final String FAILED_TO_START_STREAMING_LOG = "Failed to start streaming log";
    private static final String FAILED_TO_CLOSE_STREAMING_LOG = "Failed to close streaming log";
    private static final String CLOSING_STREAMING_LOG = "Closing Streaming Log...";
    private static final String SITES = "sites";
    private static final String SUBSCRIPTIONS = "subscriptions";
    private static final String SLOTS = "slots";

    private Map<String, AppServiceStreamingLogConsoleView> consoleViewMap = new HashMap<>();

    public void showWebAppDeploymentSlotStreamingLog(Project project, String slotId) {
        showAppServiceStreamingLog(project, slotId, new WebAppSlotLogStreaming(slotId));
    }

    public void showWebAppStreamingLog(Project project, String webAppId) {
        showAppServiceStreamingLog(project, webAppId, new WebAppLogStreaming(webAppId));
    }

    public void showFunctionStreamingLog(Project project, String functionId) {
        showAppServiceStreamingLog(project, functionId, new FunctionLogStreaming(functionId));
    }

    public void closeStreamingLog(Project project, String appId) {
        DefaultLoader.getIdeHelper().runInBackground(project, CLOSING_STREAMING_LOG, false, true, null, () -> {
            if (consoleViewMap.containsKey(appId) && consoleViewMap.get(appId).isActive()) {
                consoleViewMap.get(appId).closeStreamingLog();
            } else {
                DefaultLoader.getIdeHelper().invokeLater(() -> PluginUtil.displayErrorDialog(
                        FAILED_TO_CLOSE_STREAMING_LOG, STREAMING_LOG_NOT_STARTED));
            }
        });
    }

    private void showAppServiceStreamingLog(Project project, String resourceId, ILogStreaming logStreaming) {
        DefaultLoader.getIdeHelper().runInBackground(project, STARTING_STREAMING_LOG, false, true, null, () -> {
            try {
                final String name = logStreaming.getTitle();
                final AppServiceStreamingLogConsoleView consoleView = getOrCreateConsoleView(project, resourceId);
                if (!consoleView.isActive()) {
                    if (!logStreaming.isLogStreamingSupported()) {
                        DefaultLoader.getIdeHelper().invokeLater(() -> PluginUtil.displayInfoDialog(
                                NOT_SUPPORTED, String.format(LOG_STREAMING_IS_NOT_SUPPORTED, name)));
                        return;
                    }
                    if (!logStreaming.isLogStreamingEnabled()) {
                        // Enable Log Streaming if log streaming of target is not enabled
                        final boolean userInput = DefaultLoader.getUIHelper().showConfirmation(
                                String.format(ENABLE_FILE_LOGGING_PROMPT, name), ENABLE_LOGGING, YES_NO, null);
                        if (userInput) {
                            logStreaming.enableLogStreaming();
                        } else {
                            return;
                        }
                    }
                    final Observable<String> log = logStreaming.getStreamingLogContent();
                    if (log == null) {
                        return;
                    }
                    consoleView.startStreamingLog(log);
                }
                StreamingLogsToolWindowManager.getInstance().showStreamingLogConsole(
                        project, resourceId, logStreaming.getTitle(), consoleView);
            } catch (Throwable e) {
                DefaultLoader.getIdeHelper().invokeLater(() -> PluginUtil.displayErrorDialog(
                        FAILED_TO_START_STREAMING_LOG, e.getMessage()));
            }
        });
    }

    private AppServiceStreamingLogConsoleView getOrCreateConsoleView(Project project, String resourceId) {
        return consoleViewMap.compute(resourceId, (id, view) -> {
            return (view == null || view.isDisposed()) ? new AppServiceStreamingLogConsoleView(project, id) : view;
        });
    }

    interface ILogStreaming {
        default boolean isLogStreamingSupported() throws IOException {
            return true;
        }

        boolean isLogStreamingEnabled() throws IOException;

        void enableLogStreaming() throws IOException;

        String getTitle() throws IOException;

        Observable<String> getStreamingLogContent() throws IOException;
    }

    class FunctionLogStreaming implements ILogStreaming {

        private static final String APPLICATION_INSIGHT_PATTERN = "%s/#blade/AppInsightsExtension/QuickPulseBladeV2"
                + "/ComponentId/%s/ResourceId/%s";

        private String resourceId;
        private FunctionApp functionApp;

        FunctionLogStreaming(final String resourceId) {
            this.resourceId = resourceId;
        }

        @Override
        public boolean isLogStreamingEnabled() throws IOException {
            return getFunctionApp().operatingSystem() == OperatingSystem.LINUX ? true
                   : AzureFunctionMvpModel.isApplicationLogEnabled(getFunctionApp());
        }

        @Override
        public void enableLogStreaming() throws IOException {
            AzureFunctionMvpModel.enableApplicationLog(getFunctionApp());
        }

        @Override
        public String getTitle() {
            return AzureMvpModel.getSegment(resourceId, SITES);
        }

        @Override
        public Observable<String> getStreamingLogContent() throws IOException {
            if (getFunctionApp().operatingSystem() == OperatingSystem.LINUX) {
                try {
                    // For linux function, we will just open the "Live Metrics Stream" view in the portal
                    openLiveMetricsStream();
                    return null;
                } catch (Exception e) {
                    throw new IOException(e.getMessage(), e);
                }
            }
            return getFunctionApp().streamAllLogsAsync();
        }

        private void openLiveMetricsStream() throws Exception {
            final AppSetting aiAppSettings = functionApp.getAppSettings().get("APPINSIGHTS_INSTRUMENTATIONKEY");
            if (aiAppSettings == null) {
                throw new IOException("You must configure Application Insights to stream logs on Linux Function Apps.");
            }
            final String aiKey = aiAppSettings.value();
            final String subscriptionId = AzureMvpModel.getSegment(resourceId, SUBSCRIPTIONS);
            final AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            final SubscriptionDetail subscriptionDetail = azureManager.getSubscriptionManager()
                                                                      .getSubscriptionIdToSubscriptionDetailsMap()
                                                                      .get(subscriptionId);
            final List<Resource> resources =
                    AzureSDKManager.getApplicationInsightsResources(subscriptionDetail);
            final Resource target =
                    resources.stream()
                             .filter(aiResource -> StringUtils.equals(aiResource.getInstrumentationKey(), aiKey))
                             .findFirst()
                             .orElseThrow(() -> new IOException(String.format(
                                     "AI instances defined in app setting was not found in current subscription %s",
                                     subscriptionId)));
            final String componentId = URLEncoder.encode(getAIResourceObject(target).toString(), "utf-8");
            final String resourceId = URLEncoder.encode(target.getId(), "utf-8");
            final String url = String.format(APPLICATION_INSIGHT_PATTERN,
                                             azureManager.getPortalUrl(),
                                             componentId,
                                             resourceId);
            DefaultLoader.getIdeHelper().openLinkInBrowser(url);
        }

        private JsonObject getAIResourceObject(Resource aiResource) {
            final JsonObject componentObject = new JsonObject();
            componentObject.addProperty("Name", aiResource.getName());
            componentObject.addProperty("SubscriptionId", AzureMvpModel.getSegment(aiResource.getId(), SUBSCRIPTIONS));
            componentObject.addProperty("ResourceGroup", aiResource.getResourceGroup());
            return componentObject;
        }

        private FunctionApp getFunctionApp() throws IOException {
            if (functionApp == null) {
                functionApp = AzureFunctionMvpModel.getInstance().getFunctionById(
                        AzureMvpModel.getSegment(resourceId, SUBSCRIPTIONS), resourceId);
            }
            return functionApp;
        }
    }

    class WebAppLogStreaming implements ILogStreaming {
        private String resourceId;
        private WebApp webApp;

        public WebAppLogStreaming(String resourceId) {
            this.resourceId = resourceId;
        }

        @Override
        public boolean isLogStreamingEnabled() throws IOException {
            return AzureWebAppMvpModel.isHttpLogEnabled(getWebApp());
        }

        @Override
        public void enableLogStreaming() throws IOException {
            AzureWebAppMvpModel.enableHttpLog(getWebApp().update());
        }

        @Override
        public String getTitle() {
            return AzureMvpModel.getSegment(resourceId, SITES);
        }

        @Override
        public Observable<String> getStreamingLogContent() throws IOException {
            return getWebApp().streamAllLogsAsync();
        }

        private WebApp getWebApp() throws IOException {
            if (webApp == null) {
                webApp = AzureWebAppMvpModel.getInstance().getWebAppById(
                        AzureMvpModel.getSegment(resourceId, SUBSCRIPTIONS), resourceId);
            }
            return webApp;
        }
    }

    class WebAppSlotLogStreaming implements ILogStreaming {
        private String resourceId;
        private DeploymentSlot deploymentSlot;

        public WebAppSlotLogStreaming(String resourceId) {
            this.resourceId = resourceId;
        }

        @Override
        public boolean isLogStreamingEnabled() throws IOException {
            return AzureWebAppMvpModel.isHttpLogEnabled(getDeploymentSlot());
        }

        @Override
        public void enableLogStreaming() throws IOException {
            AzureWebAppMvpModel.enableHttpLog(getDeploymentSlot().update());
        }

        @Override
        public String getTitle() {
            return AzureMvpModel.getSegment(resourceId, SLOTS);
        }

        @Override
        public Observable<String> getStreamingLogContent() throws IOException {
            return getDeploymentSlot().streamAllLogsAsync();
        }

        private DeploymentSlot getDeploymentSlot() throws IOException {
            if (deploymentSlot == null) {
                final String subscriptionId = AzureMvpModel.getSegment(resourceId, SUBSCRIPTIONS);
                final String webAppId = resourceId.substring(0, resourceId.indexOf("/slots"));
                final WebApp webApp = AzureWebAppMvpModel.getInstance().getWebAppById(subscriptionId, webAppId);
                deploymentSlot = webApp.deploymentSlots().getById(resourceId);
            }
            return deploymentSlot;
        }
    }
}
