/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.dbtools;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.cosmos.creation.CreateCosmosDBAccountAction;
import com.microsoft.azure.toolkit.intellij.dbtools.NoResourceTipLabel;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.cosmos.AzureCosmosService;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.event.InputEvent;

public class NoAccountsTipLabel extends NoResourceTipLabel {
    private static final String NO_ACCOUNT_TIPS_TEMPLATE = "<html>No Azure Cosmos DB accounts (%s). You can <a href=''>create one</a> first.</html>";
    @Nonnull
    private final DatabaseAccountKind kind;

    public NoAccountsTipLabel(@Nonnull DatabaseAccountKind kind) {
        super(String.format(NO_ACCOUNT_TIPS_TEMPLATE, kind.getValue()));
        this.kind = kind;
    }

    @Override
    @AzureOperation(name = "user/cosmos.create_account_from_dbtools")
    protected void createResourceInIde(InputEvent e) {
        OperationContext.action().setTelemetryProperty("kind", this.kind.getValue());
        super.createResourceInIde(e);
    }

    @Nullable
    @Override
    protected Class<? extends AzService> getClazzForNavigationToExplorer() {
        return AzureCosmosService.class;
    }

    @Override
    protected void createResource(Project project) {
        CreateCosmosDBAccountAction.create(null, null);
    }
}

