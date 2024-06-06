/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.tasks;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Phase;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.input.CognitiveSubscriptionInput;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.AzureCognitiveServices;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SelectSubscriptionTask implements Task {
    public static final String DEFAULT_SUBSCRIPTION = "default_subscription";
    public static final String SUBSCRIPTION = "subscription";
    private final ComponentContext context;

    public SelectSubscriptionTask(ComponentContext taskContext) {
        this.context = taskContext;
    }

    @Override
    public void prepare() {
        final Phase currentPhase = context.getCourse().getCurrentPhase();
        currentPhase.expandPhasePanel();

        this.init();
    }


    @Override
    public void execute() {
        final Subscription subscription = Optional.ofNullable((Subscription) context.getParameter(CognitiveSubscriptionInput.SUBSCRIPTION))
                .orElseThrow(() -> new AzureToolkitRuntimeException("Please select your subscription in the combo box."));
        if (!Azure.az(AzureCognitiveServices.class).isOpenAIEnabled(subscription.getId())) {
            throw new AzureToolkitRuntimeException("OpenAI is not enabled for current subscription. " +
                    "Please visit https://aka.ms/oai/access to request access to Azure OpenAI service");
        }
        final Account account = Azure.az(AzureAccount.class).account();
        final List<Subscription> selectedSubscriptions = account.getSelectedSubscriptions();
        if (!selectedSubscriptions.contains(subscription)) {
            selectedSubscriptions.add(subscription);
            account.setSelectedSubscriptions(selectedSubscriptions.stream().map(Subscription::getId).collect(Collectors.toList()));
        }
        context.applyResult(SUBSCRIPTION, subscription);
        AzureMessager.getMessager().info(AzureString.format("Sign in successfully with subscription \"%s [%s]\"", subscription.getName(), subscription.getId()));
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.openai.select_subscription";
    }

    private void init() {
        final Account account = Azure.az(AzureAccount.class).account();
        final List<Subscription> selectedSubscriptions = account.getSelectedSubscriptions();
        final List<Subscription> subscriptions = account.getSubscriptions();
        final Predicate<Subscription> predicate = s -> Azure.az(AzureCognitiveServices.class).isOpenAIEnabled(s.getId());
        final Subscription defaultSubscription = selectedSubscriptions.stream().filter(predicate).findFirst()
                .orElseGet(() -> subscriptions.stream().filter(predicate).findFirst().orElse(null));
        Optional.ofNullable(defaultSubscription).ifPresent(s -> context.applyResult(DEFAULT_SUBSCRIPTION, s));
    }
}
