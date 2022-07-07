package com.microsoft.azure.toolkit.intellij.appservice.task;

import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.ide.guidance.task.SignInTask;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.legacy.function.FunctionAppService;

import javax.annotation.Nonnull;
import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateFunctionAppTask.APPINSIGHTS_INSTRUMENTATION_KEY;

public class CreateFunctionAppTask implements Task {
    public static final String FUNCTION_APP_NAME = "functionAppName";
    public static final String FUNCTION_ID = "functionId";
    public static final String DEFAULT_FUNCTION_APP_NAME = "defaultFunctionAppName";
    public static final String RESOURCE_GROUP = "resourceGroup";
    public static final String INSIGHTS_INSTRUMENT_KEY = "insightsInstrumentKey";
    private final ComponentContext context;

    public CreateFunctionAppTask(@Nonnull final ComponentContext context) {
        this.context = context;
        init();
    }

    @Override
    @AzureOperation(name = "guidance.create_function_app", type = AzureOperation.Type.SERVICE)
    public void execute() throws Exception {
        final String name = (String) context.getParameter(FUNCTION_APP_NAME);
        final Subscription subscription = Optional.ofNullable((String) context.getParameter(SignInTask.SUBSCRIPTION_ID))
                .map(id -> Azure.az(AzureAccount.class).account().getSubscription(id))
                .orElseGet(() -> Azure.az(AzureAccount.class).account().getSelectedSubscriptions().get(0));
        final FunctionAppConfig functionAppConfig = FunctionAppConfig.getFunctionAppDefaultConfig(name);
        functionAppConfig.setName(name);
        functionAppConfig.setSubscription(subscription);
        functionAppConfig.setRuntime(Runtime.FUNCTION_WINDOWS_JAVA11);
        final FunctionApp functionApp = FunctionAppService.getInstance().createFunctionApp(functionAppConfig);
        context.applyResult(FUNCTION_ID, functionApp.getId());
        context.applyResult(RESOURCE_GROUP, functionApp.getResourceGroupName());
        context.applyResult(INSIGHTS_INSTRUMENT_KEY, functionApp.getAppSettings().get(APPINSIGHTS_INSTRUMENTATION_KEY));
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.function.create_app";
    }

    private void init() {
        final String defaultFunctionAppName = String.format("%s-%s", context.getCourse().getName(), Utils.getTimestamp());
        context.applyResult(DEFAULT_FUNCTION_APP_NAME, defaultFunctionAppName);
    }
}
