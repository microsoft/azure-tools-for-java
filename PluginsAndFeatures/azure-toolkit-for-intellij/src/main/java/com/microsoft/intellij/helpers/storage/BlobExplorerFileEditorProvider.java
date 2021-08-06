/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.helpers.storage;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.intellij.helpers.UIHelperImpl;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class BlobExplorerFileEditorProvider implements FileEditorProvider, DumbAware {
    public static Key<BlobContainer> CONTAINER_KEY = new Key<BlobContainer>("blobContainer");

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        StorageAccount storageAccount = virtualFile.getUserData(UIHelperImpl.STORAGE_KEY);
        ClientStorageAccount clientStorageAccount = virtualFile.getUserData(UIHelperImpl.CLIENT_STORAGE_KEY);
        BlobContainer blobContainer = virtualFile.getUserData(CONTAINER_KEY);

        return ((storageAccount != null || clientStorageAccount != null )&& blobContainer != null);
    }

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        BlobExplorerFileEditor blobExplorerFileEditor = new BlobExplorerFileEditor(project);

        StorageAccount storageAccount = virtualFile.getUserData(UIHelperImpl.STORAGE_KEY);
        BlobContainer blobContainer = virtualFile.getUserData(CONTAINER_KEY);

        blobExplorerFileEditor.setBlobContainer(blobContainer);
        if (storageAccount != null) {
            blobExplorerFileEditor.setConnectionString(StorageClientSDKManager.getConnectionString(storageAccount));
            blobExplorerFileEditor.setStorageAccount(storageAccount.name());
        } else {
            blobExplorerFileEditor.setConnectionString(virtualFile.getUserData(UIHelperImpl.CLIENT_STORAGE_KEY).getConnectionString());
            blobExplorerFileEditor.setStorageAccount(virtualFile.getUserData(UIHelperImpl.CLIENT_STORAGE_KEY).getName());
        }

        blobExplorerFileEditor.fillGrid();

        return blobExplorerFileEditor;
    }

    @Override
    public void disposeEditor(@NotNull FileEditor fileEditor) {
        Disposer.dispose(fileEditor);
    }

    @NotNull
    @Override
    public FileEditorState readState(@NotNull Element element, @NotNull Project project, @NotNull VirtualFile virtualFile) {
        return FileEditorState.INSTANCE;
    }

    @Override
    public void writeState(@NotNull FileEditorState fileEditorState, @NotNull Project project, @NotNull Element element) {
    }

    @NotNull
    @Override
    public String getEditorTypeId() {
        return "Azure-Storage-Blob-Editor";
    }

    @NotNull
    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }
}
