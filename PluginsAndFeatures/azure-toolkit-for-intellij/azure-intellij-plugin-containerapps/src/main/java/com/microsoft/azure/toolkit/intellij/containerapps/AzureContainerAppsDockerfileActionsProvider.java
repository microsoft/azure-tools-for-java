/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAzureActionManager;
import com.microsoft.azure.toolkit.intellij.containerregistry.runner.DockerfileActionsProvider;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;

import java.util.Objects;
import java.util.Optional;

public class AzureContainerAppsDockerfileActionsProvider implements DockerfileActionsProvider {
    @Override
    public AnAction[] getActions(VirtualFile dockerfile) {
        final Action<VirtualFile> bindedAction = AzureActionManager.getInstance().getAction(IntelliJContainerAppsActionsContributor.DEPLOY_IMAGE_TO_ACA).bind(dockerfile);
        final AnAction wrappedAction = Optional.ofNullable(bindedAction).map(IntellijAzureActionManager.AnActionWrapper::new).orElse(null);
        return Objects.isNull(wrappedAction) ? null : new AnAction[]{wrappedAction};
    }

    @Override
    public int getPriority() {
        return DockerfileActionsProvider.super.getPriority() + 10;
    }
}
