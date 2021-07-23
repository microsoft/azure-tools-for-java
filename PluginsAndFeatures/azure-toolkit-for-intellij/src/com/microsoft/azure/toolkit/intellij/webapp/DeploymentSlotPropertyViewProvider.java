/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.intellij.helpers.UIHelperImpl;

public class DeploymentSlotPropertyViewProvider extends WebAppBasePropertyViewProvider {
    public static final String TYPE = "DEPLOYMENT_SLOT_PROPERTY";

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        final String sid = virtualFile.getUserData(UIHelperImpl.SUBSCRIPTION_ID);
        final String webAppId = virtualFile.getUserData(UIHelperImpl.WEBAPP_ID);
        final String name = virtualFile.getUserData(UIHelperImpl.SLOT_NAME);
        return DeploymentSlotPropertyView.create(project, sid, webAppId, name, virtualFile);
    }

    @Override
    protected String getType() {
        return TYPE;
    }
}
