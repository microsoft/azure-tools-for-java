package com.microsoft.azure.toolkit.ide.guidance.task;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import javax.annotation.Nonnull;

public class SelectSubscriptionTask implements Task {
    public static final String SUBSCRIPTION_ID = "subscriptionId";

    private final ComponentContext context;
    private AzureEventBus.EventListener accountListener;

    public SelectSubscriptionTask(@Nonnull final ComponentContext context) {
        this.context = context;
    }

    @Override
    public void prepare() {
        this.accountListener = new AzureEventBus.EventListener(ignore ->
                AzureTaskManager.getInstance().runOnPooledThread(this::selectSubscription));
        AzureEventBus.on("account.subscription_changed.account", accountListener);
    }

    @Override
    @AzureOperation(name = "guidance.select_subscription", type = AzureOperation.Type.SERVICE)
    public void execute() {
        selectSubscription();
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.auth.select_subscription";
    }

    private void selectSubscription() {
        final AzureAccount az = Azure.az(AzureAccount.class);
        final Subscription subscription = az.account().getSelectedSubscriptions().get(0);
        context.applyResult(SUBSCRIPTION_ID, subscription.getId());
        AzureMessager.getMessager().info(AzureString.format("Sign in successfully with subscription %s", subscription.getId()));
    }

    @Override
    public void dispose() {
        if (accountListener != null) {
            AzureEventBus.off("account.subscription_changed.account", accountListener);
        }
        accountListener = null;
    }
}
