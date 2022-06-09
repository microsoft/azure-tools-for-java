package com.microsoft.azure.toolkit.ide.guideline.task.create.webapp;

import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig;
import com.microsoft.azure.toolkit.ide.guideline.Context;
import com.microsoft.azure.toolkit.ide.guideline.InputComponent;
import com.microsoft.azure.toolkit.ide.guideline.Task;
import com.microsoft.azure.toolkit.ide.guideline.task.SignInTask;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.legacy.webapp.WebAppService;

import java.util.Optional;

public class CreateWebAppTask implements Task {
    public static final String WEBAPP_NAME = "webappName";
    public static final String WEBAPP_ID = "webappId";
    public static final String RESOURCE_ID = "webappId";

    @Override
    public InputComponent getInput() {
        return new CreateWebAppInputPanel();
    }

    @Override
    public void execute(Context context) {
        final String name = (String) context.getProperty(WEBAPP_NAME);
        final Subscription subscription = Optional.ofNullable((String) context.getProperty(SignInTask.SUBSCRIPTION_ID))
                .map(id -> Azure.az(AzureAccount.class).account().getSubscription(id))
                .orElseGet(() -> Azure.az(AzureAccount.class).getSubscriptions().get(0));
        WebAppConfig webAppConfig = WebAppConfig.getWebAppDefaultConfig(name);
        webAppConfig.setName(name);
        webAppConfig.setSubscription(subscription);
        webAppConfig.setRuntime(Runtime.LINUX_JAVA11);
        final WebApp webApp = WebAppService.getInstance().createWebApp(webAppConfig);
        context.setProperty(WEBAPP_ID, webApp.getId());
        context.setProperty(RESOURCE_ID, webApp.getId());
    }
}
