/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.storage.action;

import com.microsoft.azure.toolkit.ide.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.storage.service.StorageAccount;

public class OpenStorageBrowserAction {
    public static void openStorageBrowser(StorageAccount account) {
        final AzureString title = AzureString.format("storage|account.open_storage_explorer", account.name());
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(title, () -> {
            final Subscription subscription = Azure.az(AzureAccount.class).account().getSubscription(account.subscriptionId());
            final String url = account.portalUrl() + "/storageExplorer";
            AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(url);
        }));
    }
}
