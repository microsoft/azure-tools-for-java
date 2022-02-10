/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm.property;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.properties.AzureResourceEditorViewManager;
import com.microsoft.azure.toolkit.lib.resource.ResourceDeployment;

import javax.annotation.Nonnull;

public class DeploymentPropertyViewProvider implements FileEditorProvider, DumbAware {

    public static final String RESOURCE_DEPLOYMENT_PROPERTY_TYPE = "Microsoft.Resources.resourceGroups.deployments";

    @Override
    public boolean accept(@Nonnull Project project, @Nonnull VirtualFile virtualFile) {
        return virtualFile.getFileType().getName().equals(getEditorTypeId());
    }

    @Nonnull
    @Override
    public FileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile virtualFile) {
        final ResourceDeployment deployment = (ResourceDeployment) virtualFile.getUserData(AzureResourceEditorViewManager.AZURE_RESOURCE_KEY);
        assert deployment != null;
        return new DeploymentPropertiesView(project, deployment, virtualFile);
    }

    @Nonnull
    @Override
    public String getEditorTypeId() {
        return RESOURCE_DEPLOYMENT_PROPERTY_TYPE;
    }

    @Nonnull
    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
