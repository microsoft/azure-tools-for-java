package com.microsoft.azure.toolkit.intellij.appservice.task;

import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig;
import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.ide.guidance.task.SignInTask;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.legacy.webapp.WebAppService;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupConfig;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

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
    @AzureOperation(name = "guidance.create_webapp", type = AzureOperation.Type.SERVICE)
    public void execute() throws Exception {
        final String name = (String) Objects.requireNonNull(context.getParameter(WEBAPP_NAME), "`name` is required to create web app");
        final Subscription subscription = Optional.ofNullable((String) context.getParameter(SignInTask.SUBSCRIPTION_ID))
                .map(id -> Azure.az(AzureAccount.class).account().getSubscription(id))
                .orElseThrow(() -> new AzureToolkitRuntimeException("Failed to get subscription to create web app"));
        final WebAppConfig webAppConfig = WebAppConfig.getWebAppDefaultConfig(name);
        webAppConfig.setName(name);
        webAppConfig.setSubscription(subscription);
        webAppConfig.setRuntime(Runtime.LINUX_JAVA11);
        final String rgName = Utils.generateRandomResourceName(String.format("rg-%s", name), 90);
        final ResourceGroupConfig newGroupConfig = ResourceGroupConfig.builder().subscriptionId(subscription.getId()).name(rgName).region(webAppConfig.getRegion()).build();
        webAppConfig.setResourceGroup(newGroupConfig);
        final WebApp webApp = WebAppService.getInstance().createWebApp(webAppConfig);
        context.applyResult(WEBAPP_ID, webApp.getId());
        context.applyResult(RESOURCE_GROUP, webApp.getResourceGroupName());
    }

    private void init() {
        final String defaultWebAppName = String.format("%s-%s", context.getCourse().getName(), Utils.getTimestamp());
        context.applyResult(DEFAULT_WEB_APP_NAME, defaultWebAppName);
    }
}
