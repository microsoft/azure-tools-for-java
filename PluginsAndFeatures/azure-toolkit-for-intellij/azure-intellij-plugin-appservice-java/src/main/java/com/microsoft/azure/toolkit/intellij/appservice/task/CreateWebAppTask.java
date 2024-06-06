package com.microsoft.azure.toolkit.intellij.appservice.task;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.ide.guidance.task.SignInTask;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceIntelliJActionsContributor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.legacy.webapp.WebAppService;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.ide.guidance.task.SelectSubscriptionTask.SUBSCRIPTION;

public class CreateWebAppTask implements Task {
    public static final String WEBAPP_NAME = "webAppName";
    public static final String WEBAPP_ID = "webappId";
    public static final String DEFAULT_WEB_APP_NAME = "defaultWebAppName";
    public static final String RESOURCE_GROUP = "resourceGroup";
    private final ComponentContext context;

    public CreateWebAppTask(@Nonnull final ComponentContext context) {
        this.context = context;
        this.init();
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.webapp.create_app";
    }

    @Override
    @AzureOperation(name = "internal/guidance.create_webapp")
    public void execute() throws Exception {
        final String name = (String) Objects.requireNonNull(context.getParameter(WEBAPP_NAME), "`name` is required to create web app");
        final Subscription subscription = Optional.ofNullable((Subscription) context.getParameter(SUBSCRIPTION))
                .orElseThrow(() -> new AzureToolkitRuntimeException("Failed to get subscription to create web app"));

        final AppServiceConfig webAppConfig = AppServiceIntelliJActionsContributor.getDefaultWebAppConfig(null);
        webAppConfig.appName(name);
        webAppConfig.subscriptionId(subscription.getId());
        webAppConfig.runtime(RuntimeConfig.fromRuntime(WebAppRuntime.getDefaultJavaseRuntime()));

        final String rgName = Utils.generateRandomResourceName(String.format("rg-%s", name), 90);
        webAppConfig.resourceGroup(rgName);
	    final WebAppBase<?, ?, ?> webApp = WebAppService.getInstance().createWebApp(webAppConfig);
        context.applyResult(WEBAPP_ID, webApp.getId());
        context.applyResult(RESOURCE_GROUP, webApp.getResourceGroupName());
    }

    private void init() {
        final String defaultWebAppName = String.format("%s-%s", context.getCourse().getName(), Utils.getTimestamp());
        context.applyResult(DEFAULT_WEB_APP_NAME, defaultWebAppName);
    }
}
