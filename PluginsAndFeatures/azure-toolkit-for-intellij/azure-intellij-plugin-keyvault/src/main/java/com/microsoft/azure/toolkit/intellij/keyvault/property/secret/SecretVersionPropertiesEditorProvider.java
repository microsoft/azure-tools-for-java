/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.property.secret;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.properties.AzureResourceEditorViewManager;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.keyvault.secret.SecretVersion;

import javax.annotation.Nonnull;

public class SecretVersionPropertiesEditorProvider implements FileEditorProvider, DumbAware {

    public static final String TYPE = "Microsoft.KeyVault.vaults.secrets.versions";

    @Override
    public boolean accept(@Nonnull Project project, @Nonnull VirtualFile virtualFile) {
        return virtualFile.getFileType().getName().equals(getEditorTypeId());
    }

    @Nonnull
    @Override
    @ExceptionNotification
    @AzureOperation(name = "user/keyvault.create_properties_editor.secret_version", params = {"virtualFile.getName()"})
    public FileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile virtualFile) {
        final SecretVersion data = (SecretVersion) virtualFile.getUserData(AzureResourceEditorViewManager.AZURE_RESOURCE_KEY);
        assert data != null;
        return new SecretPropertiesEditor(project, data, virtualFile);
    }

    @Nonnull
    @Override
    public String getEditorTypeId() {
        return TYPE;
    }

    @Nonnull
    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
