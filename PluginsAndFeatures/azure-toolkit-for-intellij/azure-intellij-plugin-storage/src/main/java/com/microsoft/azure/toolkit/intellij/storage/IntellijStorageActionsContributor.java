/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.storage.StorageActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.intellij.storage.connection.StorageAccountResourceDefinition;
import com.microsoft.azure.toolkit.intellij.storage.creation.CreateStorageAccountAction;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.storage.service.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.service.StorageAccount;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntellijStorageActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> condition = (r, e) -> r instanceof AzureStorageAccount;
        final BiConsumer<Object, AnActionEvent> handler = (c, e) -> CreateStorageAccountAction.createStorageAccount((e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CREATE, condition, handler);

        am.<IAzureBaseResource<?, ?>, AnActionEvent>registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof StorageAccount,
                (r, e) -> AzureTaskManager.getInstance().runLater(() -> {
                    final ConnectorDialog dialog = new ConnectorDialog(e.getProject());
                    dialog.setResource(new AzureServiceResource<>(((StorageAccount) r), StorageAccountResourceDefinition.INSTANCE));
                    dialog.show();
                }));
    }

    @Override
    public int getOrder() {
        return StorageActionsContributor.INITIALIZE_ORDER + 1;
    }
}
