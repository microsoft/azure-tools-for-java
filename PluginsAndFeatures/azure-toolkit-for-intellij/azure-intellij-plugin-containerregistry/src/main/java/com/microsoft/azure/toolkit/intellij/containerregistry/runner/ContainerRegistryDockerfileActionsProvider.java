/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.runner;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.containerregistry.action.PushImageAction;
import com.microsoft.azure.toolkit.intellij.containerregistry.action.RunOnDockerHostAction;

public class ContainerRegistryDockerfileActionsProvider implements DockerfileActionsProvider {
    @Override
    public AnAction[] getActions(VirtualFile dockerfile) {
        final DockerImage dockerImage = new DockerImage(dockerfile);
        final RunOnDockerHostAction runAction = new RunOnDockerHostAction(dockerImage);
        final PushImageAction pushAction = new PushImageAction(dockerImage);
        return new AnAction[]{runAction, pushAction};
    }
}
