package com.microsoft.azure.toolkit.ide.guidance.task;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.IAccountActions;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import javax.annotation.Nonnull;
import java.util.List;

public class SelectSubscriptionTask implements Task {
    public static final String SUBSCRIPTION = "subscription";

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
    public void execute() {
        selectSubscription();
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.auth.select_subscription";
    }

    @AzureOperation(name = "internal/guidance.select_subscription")
    private void selectSubscription() {
        final Account account = Azure.az(AzureAccount.class).account();
        final List<Subscription> subscriptions = account.getSubscriptions();
        if (subscriptions.isEmpty()) {
            throw new AzureToolkitRuntimeException("there are no subscriptions in your account", IAccountActions.TRY_AZURE, IAccountActions.SELECT_SUBS);
        }
        List<Subscription> selectedSubscriptions = account.getSelectedSubscriptions();
        if (selectedSubscriptions.isEmpty()) {
            account.setSelectedSubscriptions(List.of(subscriptions.get(0).getId()));
            selectedSubscriptions = account.getSelectedSubscriptions();
        }
        final Subscription subscription = selectedSubscriptions.get(0);
        context.applyResult(SUBSCRIPTION, subscription);
        AzureMessager.getMessager().info(AzureString.format("Sign in successfully with subscription \"%s [%s]\"", subscription.getName(), subscription.getId()));
    }

    @Override
    public void dispose() {
        if (accountListener != null) {
            AzureEventBus.off("account.subscription_changed.account", accountListener);
        }
        accountListener = null;
    }
}
