/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAzureActionManager;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.containerregistry.runner.DockerfileActionsProvider;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.action.WebAppOnLinuxAction;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;

import java.util.Objects;
import java.util.Optional;

public class AzureAppServiceDockerfileActionsProvider implements DockerfileActionsProvider {
    @Override
    public AnAction[] getActions(VirtualFile dockerfile) {
        final DockerImage dockerImage = new DockerImage(dockerfile);
        return new AnAction[]{new WebAppOnLinuxAction(dockerImage)};
    }

    @Override
    public int getPriority() {
        return DockerfileActionsProvider.super.getPriority() + 20;
    }
}
