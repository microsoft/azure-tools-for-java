/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.containerregistry.actions;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.microsoft.azure.toolkit.intellij.common.AzureFileType;
import com.microsoft.azure.toolkit.intellij.common.AzureIcons;
import com.microsoft.azure.toolkit.intellij.legacy.docker.ContainerRegistryPropertyView;
import com.microsoft.azure.toolkit.intellij.legacy.docker.ContainerRegistryPropertyViewProvider;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

import static com.microsoft.azure.toolkit.intellij.common.properties.AzureResourceEditorViewManager.CANNOT_GET_FILE_EDITOR_MANAGER;
import static com.microsoft.azure.toolkit.intellij.common.properties.AzureResourceEditorViewManager.UNABLE_TO_OPEN_EDITOR_WINDOW;

public class OpenContainerRegistryPropertyViewAction {
    public static final Key<String> RESOURCE_ID = new Key<>("resourceId");
    public static final Key<String> SUBSCRIPTION_ID = new Key<>("subscriptionId");

    public static void openContainerRegistryPropertyView(@Nonnull ContainerRegistry registry, @Nonnull Project project) {
        final String sid = registry.getSubscriptionId();
        final String resId = registry.getId();
        if (StringUtils.isAnyEmpty(sid, resId)) {
            return;
        }
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        if (fileEditorManager == null) {
            AzureMessager.getMessager().error(CANNOT_GET_FILE_EDITOR_MANAGER, UNABLE_TO_OPEN_EDITOR_WINDOW);
            return;
        }
        LightVirtualFile itemVirtualFile = searchExistingFile(fileEditorManager,
                ContainerRegistryPropertyViewProvider.TYPE, resId);
        if (itemVirtualFile == null) {
            itemVirtualFile = createVirtualFile(registry.getName(), sid, resId);
            final AzureFileType fileType = new AzureFileType(ContainerRegistryPropertyViewProvider.TYPE,
                    AzureIcons.getIcon(AzureIconSymbol.ContainerRegistry.MODULE.getPath()));
            itemVirtualFile.setFileType(fileType);
        }
        final LightVirtualFile targetFile = itemVirtualFile;
        AzureTaskManager.getInstance().runLater(() -> {
            final FileEditor[] editors = fileEditorManager.openFile(targetFile, true /*focusEditor*/, true /*searchForOpen*/);
            for (final FileEditor editor: editors) {
                if (editor.getName().equals(ContainerRegistryPropertyView.ID) &&
                        editor instanceof ContainerRegistryPropertyView) {
                    ((ContainerRegistryPropertyView) editor).onReadProperty(sid, resId);
                }
            }
        });
    }

    private static LightVirtualFile searchExistingFile(FileEditorManager fileEditorManager, String fileType, String resourceId) {
        LightVirtualFile virtualFile = null;
        for (final VirtualFile editedFile : fileEditorManager.getOpenFiles()) {
            final String fileResourceId = editedFile.getUserData(RESOURCE_ID);
            if (fileResourceId != null && fileResourceId.equals(resourceId) &&
                    editedFile.getFileType().getName().equals(fileType)) {
                virtualFile = (LightVirtualFile) editedFile;
                break;
            }
        }
        return virtualFile;
    }

    private static LightVirtualFile createVirtualFile(String name, String sid, String resId) {
        final LightVirtualFile itemVirtualFile = new LightVirtualFile(name);
        itemVirtualFile.putUserData(SUBSCRIPTION_ID, sid);
        itemVirtualFile.putUserData(RESOURCE_ID, resId);
        return itemVirtualFile;
    }
}
