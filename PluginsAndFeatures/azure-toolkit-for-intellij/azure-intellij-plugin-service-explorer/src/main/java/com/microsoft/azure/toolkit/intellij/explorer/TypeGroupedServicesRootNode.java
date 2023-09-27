/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.explorer;

import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

import java.util.List;

public class TypeGroupedServicesRootNode extends Node<Azure> {
    private static final String NAME = "Azure";
    private final AzureEventBus.EventListener eventListener;

    public TypeGroupedServicesRootNode() {
        super(Azure.az());
        this.withLabel(NAME)
            .withIcon(AzureIcons.Common.AZURE)
            .withChildrenLoadLazily(false);

        this.eventListener = new AzureEventBus.EventListener(e -> this.onAuthEvent());
        AzureEventBus.on("account.logging_in.type", this.eventListener);
        AzureEventBus.on("account.failed_logging_in.type", this.eventListener);
        AzureEventBus.on("account.logged_in.account", this.eventListener);
        AzureEventBus.on("account.restore_sign_in", this.eventListener);
        AzureEventBus.on("account.subscription_changed.account", this.eventListener);
        AzureEventBus.on("account.logged_out.account", this.eventListener);

        this.onAuthEvent();
    }

    private void onAuthEvent() {
        final AzureAccount az = Azure.az(AzureAccount.class);
        final String desc;
        if (az.isLoggingIn()) {
            desc = "Signing In...";
            this.withTips("Signing in...");
        } else if (az.isLoggedIn()) {
            final Account account = az.account();
            final List<Subscription> subs = account.getSelectedSubscriptions();
            final int size = subs.size();
            if (size > 1) {
                desc = String.format("%d Subscriptions", size);
            } else if (size == 1) {
                desc = subs.get(0).getName();
            } else {
                desc = "No Subscriptions Selected";
            }
            this.withTips("Your Azure resources grouped by type.");
        } else {
            desc = "Not Signed In";
        }
        this.withDescription(" " + desc);
        this.refreshViewLater();
    }

    public void dispose() {
        super.dispose();
        AzureEventBus.off("account.logging_in.type", this.eventListener);
        AzureEventBus.off("account.failed_logging_in.type", this.eventListener);
        AzureEventBus.off("account.logged_in.account", this.eventListener);
        AzureEventBus.off("account.restore_sign_in", this.eventListener);
        AzureEventBus.off("account.subscription_changed.account", this.eventListener);
        AzureEventBus.off("account.logged_out.account", this.eventListener);
    }
}
