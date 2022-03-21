/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.properties;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Function;

@RequiredArgsConstructor
public class AzureResourceEditorViewManager {
    public static final String UNABLE_TO_OPEN_EDITOR_WINDOW = "Unable to open new editor window";
    public static final String CANNOT_GET_FILE_EDITOR_MANAGER = "Cannot get FileEditorManager";
    public static final Key<AzResourceBase> AZURE_RESOURCE_KEY = new Key<>("AzureResource");
    public static final Key<AzureResourceEditorViewManager> AZURE_RESOURCE_EDITOR_MANAGER_KEY = new Key<>("AzureResourceEditorManager");
    private final Function<AzResourceBase, FileType> getFileType;

    public void showEditor(@Nonnull AzResourceBase resource, @Nonnull Project project) {
        final FileEditorManager manager = FileEditorManager.getInstance(project);
        if (manager == null) {
            AzureMessager.getMessager().error(UNABLE_TO_OPEN_EDITOR_WINDOW);
            return;
        }
        final LightVirtualFile existing = searchOpenedFile(manager, resource);
        final LightVirtualFile itemVirtualFile = Objects.isNull(existing) ? createVirtualFile(resource) : existing;
        if (Objects.isNull(existing)) {
            itemVirtualFile.setFileType(getFileType.apply(resource));
        }
        itemVirtualFile.putUserData(AZURE_RESOURCE_KEY, resource);
        itemVirtualFile.putUserData(AZURE_RESOURCE_EDITOR_MANAGER_KEY, this);
        AzureTaskManager.getInstance().runLater(() -> manager.openFile(itemVirtualFile, true, true));
    }

    public void closeEditor(@Nonnull AzResourceBase resource, @Nonnull Project project) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        final LightVirtualFile file = searchOpenedFile(fileEditorManager, resource);
        if (file != null) {
            AzureTaskManager.getInstance().runLater(() -> fileEditorManager.closeFile(file));
        }
    }

    @Nullable
    private LightVirtualFile searchOpenedFile(FileEditorManager manager, AzResourceBase resource) {
        final FileType fileType = getFileType.apply(resource);
        for (final VirtualFile openedFile : manager.getOpenFiles()) {
            final AzResourceBase opened = openedFile.getUserData(AZURE_RESOURCE_KEY);
            if (openedFile.getFileType().getName().equals(fileType.getName()) && opened != null && opened.getId().equals(resource.getId())) {
                return (LightVirtualFile) openedFile;
            }
        }
        return null;
    }

    private LightVirtualFile createVirtualFile(AzResourceBase resource) {
        final LightVirtualFile itemVirtualFile = new LightVirtualFile(resource.getName());
        itemVirtualFile.putUserData(AZURE_RESOURCE_KEY, resource);
        return itemVirtualFile;
    }

    @RequiredArgsConstructor
    @Getter
    public static class AzureResourceFileType implements FileType {
        private final String name;
        private final Icon icon;

        @Override
        public String getDescription() {
            return name;
        }

        @Override
        public String getDefaultExtension() {
            return "";
        }

        @Override
        public boolean isBinary() {
            return true;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public String getCharset(@Nonnull VirtualFile virtualFile, @Nonnull byte[] bytes) {
            return StandardCharsets.UTF_8.name();
        }
    }
}
