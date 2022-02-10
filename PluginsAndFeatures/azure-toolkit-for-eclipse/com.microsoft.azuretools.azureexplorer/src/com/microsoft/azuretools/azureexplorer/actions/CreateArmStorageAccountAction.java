/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.azureexplorer.actions;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.storage.model.StorageAccountConfig;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccountDraft;
import com.microsoft.azuretools.azureexplorer.forms.CreateArmStorageAccountForm;
import com.microsoft.azuretools.azureexplorer.forms.common.Draft;
import com.microsoft.azuretools.core.handlers.SignInCommandHandler;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.StorageModule;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_STORAGE_ACCOUNT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.STORAGE;

@Name("Create Storage Account...")
public class CreateArmStorageAccountAction extends NodeActionListener {

    private StorageModule storageModule;

    public CreateArmStorageAccountAction(StorageModule storageModule) {
        this.storageModule = storageModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        SignInCommandHandler.requireSignedIn(PluginUtil.getParentShell(), () -> {
            CreateArmStorageAccountForm createStorageAccountForm = new CreateArmStorageAccountForm(PluginUtil.getParentShell(), null, null);
            createStorageAccountForm.setOnCreate(() -> {
                AzureTaskManager.getInstance().runInBackground(
                        "Creating storage account " + createStorageAccountForm.getStorageAccount().getName() + "...", new Runnable() {
                            @Override
                            public void run() {
                                EventUtil.executeWithLog(STORAGE, CREATE_STORAGE_ACCOUNT, (operation) -> {
                                    createStorageAccount(createStorageAccountForm.getStorageAccount());
                                            storageModule.load(false);
                                        }, (e) ->
                                                AzureTaskManager.getInstance().runLater(() ->
                                                        PluginUtil.displayErrorDialog(PluginUtil.getParentShell(), Messages.err,
                                                                "An error occurred while creating the storage account: " + e.getMessage())
                                                )
                                );
                            }
                        });

            });
            createStorageAccountForm.open();
        });
    }

    private static StorageAccount createStorageAccount(StorageAccountConfig config) {
        final String subscriptionId = config.getSubscription().getId();
        AzureTelemetry.getActionContext().setProperty("subscriptionId", subscriptionId);
        if (config.getResourceGroup() instanceof Draft) { // create resource group if necessary.
            final ResourceGroup newResourceGroup = Azure.az(AzureResources.class).groups(subscriptionId)
                    .createResourceGroupIfNotExist(config.getResourceGroup().getName(), config.getRegion());
            config.setResourceGroup(newResourceGroup);
        }
        final AzureStorageAccount az = Azure.az(AzureStorageAccount.class);
        final StorageAccountDraft draft = az.accounts(config.getSubscriptionId()).create(config.getName(),
                config.getResourceGroupName());
        draft.setConfig(config);
        return draft.commit();
    }

}
