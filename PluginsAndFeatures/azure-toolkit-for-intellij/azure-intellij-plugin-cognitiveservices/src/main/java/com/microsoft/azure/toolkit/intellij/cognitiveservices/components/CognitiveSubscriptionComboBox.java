/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cognitiveservices.components;

import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.AzureCognitiveServices;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class CognitiveSubscriptionComboBox extends SubscriptionComboBox {
    private boolean listUnselectedSubscriptions = false;
    private Map<Subscription, Boolean> status = new HashMap<>();

    public CognitiveSubscriptionComboBox(boolean listUnselectedSubscriptions) {
        this.listUnselectedSubscriptions = listUnselectedSubscriptions;
    }

    @Nonnull
    @Override
    protected List<Subscription> loadItems() {
        final List<Subscription> subscriptions = Optional.of(super.loadItems()).filter(CollectionUtils::isNotEmpty)
            .orElseGet(this::getAllSubscriptions);
        this.status = subscriptions.stream().collect(Collectors.toMap(subscription -> subscription,
            subscription -> Azure.az(AzureCognitiveServices.class).isOpenAIEnabled(subscription.getId())));
        return subscriptions;
    }

    private List<Subscription> getAllSubscriptions() {
        return listUnselectedSubscriptions && Azure.az(AzureAccount.class).isLoggedIn() ?
            Azure.az(AzureAccount.class).getAccount().getSubscriptions() : Collections.emptyList();
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof Subscription) {
            final Subscription subscription = (Subscription) item;
            final String name = subscription.getName();
            return BooleanUtils.isTrue(status.get(subscription)) ? name : String.format("%s (Not Available)", name);
        }
        return super.getItemText(item);
    }
}
