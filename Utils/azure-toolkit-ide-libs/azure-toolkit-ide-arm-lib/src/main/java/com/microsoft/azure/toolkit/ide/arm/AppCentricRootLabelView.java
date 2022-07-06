/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.arm;

import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;

import javax.annotation.Nonnull;
import java.util.List;

public class AppCentricRootLabelView extends AzureServiceLabelView<AzureResources> {
    private static final String NAME = "Resource Groups";
    private final AzureEventBus.EventListener subscriptionListener;
    private final AzureEventBus.EventListener logoutListener;

    public AppCentricRootLabelView(@Nonnull AzureResources service, String iconPath) {
        super(service, NAME, iconPath);
        this.subscriptionListener = new AzureEventBus.EventListener(this::onLogin);
        this.logoutListener = new AzureEventBus.EventListener(this::onLogout);
        AzureEventBus.on("account.logged_in.account", subscriptionListener);
        AzureEventBus.on("account.restore_sign_in", subscriptionListener);
        AzureEventBus.on("account.subscription_changed.account", subscriptionListener);
        AzureEventBus.on("account.logged_out.account", logoutListener);
        this.onLogin(null);
    }

    private void onLogin(AzureEvent azureEvent) {
        final AzureAccount az = Azure.az(AzureAccount.class);
        if (!az.isSignedIn()) {
            this.label = NAME;
        } else {
            final Account account = az.account();
            final List<Subscription> subs = account.getSelectedSubscriptions();
            final int size = subs.size();
            if (size > 1) {
                this.label = String.format("%s (%d subscriptions)", NAME, size);
            } else if (size == 1) {
                this.label = String.format("%s (%s)", NAME, subs.get(0).getName());
            } else {
                this.label = NAME + " (No subscription)";
            }
        }
        this.refreshView();
    }

    private void onLogout(AzureEvent azureEvent) {
        this.label = NAME;
        this.refreshView();
    }

    public void dispose() {
        super.dispose();
        AzureEventBus.on("account.subscription_changed.account", subscriptionListener);
        AzureEventBus.off("account.logged_out.account", logoutListener);
    }
}
