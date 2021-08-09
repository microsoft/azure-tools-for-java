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
import com.microsoft.azure.toolkit.intellij.common.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class IntellijShowPropertiesViewAction {
    private static final String UNABLE_TO_OPEN_EDITOR_WINDOW = "Unable to open new editor window";
    private static final String CANNOT_GET_FILE_EDITOR_MANAGER = "Cannot get FileEditorManager";
    public static final Key<IAzureResource<?>> AZURE_RESOURCE_KEY = new Key<>("AzureResource");

    public static void showPropertyView(@Nonnull IAzureResource<?> resource, @Nonnull Project project) {
        final FileEditorManager manager = FileEditorManager.getInstance(project);
        if (manager == null) {
            AzureMessager.getMessager().error(UNABLE_TO_OPEN_EDITOR_WINDOW);
            return;
        }
        final LightVirtualFile existing = searchOpenedFile(manager, resource);
        final LightVirtualFile itemVirtualFile = Objects.isNull(existing) ? createVirtualFile(resource) : existing;
        if (Objects.isNull(existing)) {
            final Icon icon = getFileTypeIcon(resource);
            final String name = getFileTypeName(resource);
            itemVirtualFile.setFileType(new AzureResourceFileType(name, icon));
        }
        AzureTaskManager.getInstance().runInModal(String.format("Loading properties of app(%s)", resource.name()), () -> {
            itemVirtualFile.putUserData(AZURE_RESOURCE_KEY, resource);
            AzureTaskManager.getInstance().runLater(() -> manager.openFile(itemVirtualFile, true, true));
        });
    }

    @Nullable
    private static LightVirtualFile searchOpenedFile(FileEditorManager manager, IAzureResource<?> resource) {
        for (final VirtualFile openedFile : manager.getOpenFiles()) {
            final IAzureResource<?> opened = openedFile.getUserData(AZURE_RESOURCE_KEY);
            if (openedFile.getFileType().getName().equals(getFileTypeName(resource)) && opened != null && opened.id().equals(resource.id())) {
                return (LightVirtualFile) openedFile;
            }
        }
        return null;
    }

    private static LightVirtualFile createVirtualFile(IAzureResource<?> resource) {
        final LightVirtualFile itemVirtualFile = new LightVirtualFile(resource.name());
        itemVirtualFile.putUserData(AZURE_RESOURCE_KEY, resource);
        return itemVirtualFile;
    }

    private static String getFileTypeName(@Nonnull IAzureResource<?> resource) {
        return String.format("%s_FILE_TYPE", resource.getClass().getSimpleName().toUpperCase());
    }

    private static Icon getFileTypeIcon(@Nonnull IAzureResource<?> resource) {
        return AzureIcons.getIcon(String.format("/icons/%s.svg", resource.getClass().getSimpleName().toLowerCase()));
    }

    @RequiredArgsConstructor
    @Getter
    private static class AzureResourceFileType implements FileType {
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
