/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp;

import com.microsoft.intellij.helpers.UIHelperImpl;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public class WebAppPropertyViewProvider extends WebAppBasePropertyViewProvider {
    public static final String TYPE = "WEB_APP_PROPERTY";

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        String sid = virtualFile.getUserData(UIHelperImpl.SUBSCRIPTION_ID);
        String id = virtualFile.getUserData(UIHelperImpl.RESOURCE_ID);
        return WebAppPropertyView.create(project, sid, id, virtualFile);
    }

    @Override
    protected String getType() {
        return TYPE;
    }
}
