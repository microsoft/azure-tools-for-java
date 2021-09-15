/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.connection;

import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.progress.ProgressIndicator;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.ResourceManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.storage.service.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.service.StorageAccount;
import org.jetbrains.annotations.NotNull;

public class StorageAccountResource extends PreloadingActivity {

    public static final AzureServiceResource.Definition<StorageAccount> DEFINITION = new AzureServiceResource.Definition<>(
            "Azure.Storage",
            "Azure Storage Account",
            "/icons/StorageAccount/StorageAccount.svg",
            StorageAccountResourcePanel::new,
            (id) -> Azure.az(AzureStorageAccount.class).get(id));

    @Override
    public void preload(@NotNull ProgressIndicator progressIndicator) {
        ResourceManager.registerDefinition(StorageAccountResource.DEFINITION);
        ConnectionManager.registerDefinition(StorageAccountResourceConnection.DEFINITION);
    }
}
