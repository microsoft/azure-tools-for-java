/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.Draft;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.resource.AzureGroup;
import com.microsoft.azure.toolkit.lib.storage.model.StorageAccountConfig;
import com.microsoft.azure.toolkit.lib.storage.service.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.service.StorageAccount;

public class CreateStorageAccountAction {
    public static void createStorageAccount(Project project) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final StorageAccountCreationDialog dialog = new StorageAccountCreationDialog(project);
            dialog.setOkActionListener((config) -> {
                dialog.close();
                createStorageAccount(config);
            });
            dialog.show();
        });
    }

    @AzureOperation(name = "storage|account.create", params = {"config.getName()"}, type = AzureOperation.Type.ACTION)
    public static void create(final StorageAccountConfig config) {
        final AzureString title = AzureOperationBundle.title("storage|account.create", config.getName());
        AzureTaskManager.getInstance().runInBackground(title, () -> create(config));
    }

    public static StorageAccount createStorageAccount(StorageAccountConfig config) {
        final String subscriptionId = config.getSubscription().getId();
        AzureTelemetry.getActionContext().setProperty("subscriptionId", subscriptionId);
        if (config.getResourceGroup() instanceof Draft) { // create resource group if necessary.
            final ResourceGroup newResourceGroup = Azure.az(AzureGroup.class)
                    .subscription(subscriptionId).create(config.getResourceGroup().getName(), config.getRegion().getName());
            config.setResourceGroup(newResourceGroup);
        }
        return Azure.az(AzureStorageAccount.class).create(config).commit();
    }
}
