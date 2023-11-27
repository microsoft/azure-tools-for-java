/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.creation;

import com.azure.resourcemanager.keyvault.models.SkuName;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.keyvault.AzureKeyVault;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVaultDraft;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVaultModule;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVaultSubscription;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import javax.annotation.Nullable;
import java.util.Optional;

public class KeyVaultCreationActions {
    public static void createNewKeyVault(@Nullable KeyVaultDraft.Config config, @Nullable final Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final KeyVaultCreationDialog dialog = new KeyVaultCreationDialog(project);
            final Action.Id<KeyVaultDraft.Config> actionId = Action.Id.of("user/keyvault.create_vault.vault");
            dialog.setOkAction(new Action<>(actionId)
                    .withLabel("Create")
                    .withIdParam(KeyVaultDraft.Config::getName)
                    .withAuthRequired(true)
                    .withHandler(result -> {
                        final KeyVaultSubscription subscription = Azure.az(AzureKeyVault.class).forSubscription(result.getSubscription().getId());
                        final KeyVaultModule module = subscription.getKeyVaultModule();
                        final KeyVaultDraft draft = module.create(result.getName(), result.getResourceGroup().getName());
                        draft.setConfig(result);
                        draft.commit();
                    }));
            Optional.ofNullable(config).ifPresent(dialog::setValue);
            dialog.show();
        });
    }

    public static KeyVaultDraft.Config getDefaultConfig(@Nullable ResourceGroup resourceGroup) {
        final KeyVaultDraft.Config config = new KeyVaultDraft.Config();
        config.setName(Utils.generateRandomResourceName("vault", 40));
        config.setSku(SkuName.STANDARD);
        Optional.ofNullable(resourceGroup).ifPresent(config::setResourceGroup);
        return config;
    }
}
