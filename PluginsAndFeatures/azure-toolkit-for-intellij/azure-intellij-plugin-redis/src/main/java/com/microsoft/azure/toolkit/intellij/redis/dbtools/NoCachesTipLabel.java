/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis.dbtools;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.dbtools.NoResourceTipLabel;
import com.microsoft.azure.toolkit.intellij.redis.creation.CreateRedisCacheAction;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.redis.AzureRedis;
import org.jetbrains.annotations.Nullable;

import java.awt.event.InputEvent;

public class NoCachesTipLabel extends NoResourceTipLabel {
    private static final String NO_SERVERS_TIPS = "<html>No existing Redis caches in Azure. You can <a href=''>create one</a> first.</html>";

    public NoCachesTipLabel() {
        super(NO_SERVERS_TIPS);
    }

    @Override
    protected void createResourceInIde(InputEvent e) {
        super.createResourceInIde(e);
    }

    @Nullable
    @Override
    protected Class<? extends AzService> getClazzForNavigationToExplorer() {
        return AzureRedis.class;
    }

    @Override
    protected void createResource(Project project) {
        CreateRedisCacheAction.create(project, null);
    }
}
