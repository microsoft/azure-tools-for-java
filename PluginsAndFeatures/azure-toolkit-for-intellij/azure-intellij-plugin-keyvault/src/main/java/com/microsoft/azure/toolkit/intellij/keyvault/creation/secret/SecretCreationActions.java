/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.creation.secret;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVault;
import com.microsoft.azure.toolkit.lib.keyvault.secret.Secret;
import com.microsoft.azure.toolkit.lib.keyvault.secret.SecretDraft;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Consumer;

public class SecretCreationActions {
    public static void createNewSecret(@Nonnull final KeyVault keyVault, @Nullable final Project project) {
        createNewSecret(keyVault, null, s -> {
        });
    }

    public static void createNewSecret(@Nonnull final KeyVault keyVault, @Nullable final SecretDraft.Config data, Consumer<Secret> callback) {
        AzureTaskManager.getInstance().runLater(() -> {
            final SecretCreationDialog dialog = new SecretCreationDialog("Create new secret");
            final Action.Id<SecretDraft.Config> actionId = Action.Id.of("user/keyvault.create_secret.secret|keyvault");
            if (Objects.nonNull(data)) {
                dialog.setValue(data);
            }
            dialog.setOkAction(new Action<>(actionId)
                .withLabel("Create")
                .withIdParam(SecretDraft.Config::getName)
                .withIdParam(keyVault.getName())
                .withAuthRequired(true)
                .withHandler(config -> {
                    final Secret secret = keyVault.createNewSecret(config);
                    callback.accept(secret);
                }));
            dialog.show();
        });
    }

    public static void createNewSecretVersion(@Nonnull final Secret secret, @Nullable final Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final SecretCreationDialog dialog = new SecretCreationDialog(String.format("Create new version for secret %s", secret.getName()));
            final Action.Id<SecretDraft.Config> actionId = Action.Id.of("user/keyvault.create_secret_version.secret");
            dialog.setOkAction(new Action<>(actionId)
                .withLabel("Create")
                .withIdParam(SecretDraft.Config::getName)
                .withAuthRequired(true)
                .withHandler(secret::addNewSecretVersion));
            dialog.setFixedName(secret.getName());
            dialog.show();
        });
    }
}
