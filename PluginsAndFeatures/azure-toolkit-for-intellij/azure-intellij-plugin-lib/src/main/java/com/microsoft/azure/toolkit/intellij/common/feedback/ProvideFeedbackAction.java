/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.feedback;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureFileType;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import javax.annotation.Nonnull;
import java.util.Objects;

public class ProvideFeedbackAction extends AnAction implements DumbAware {
    public static final Key<String> ID = new Key<>("ProvideFeedbackAction");

    @Override
    public void actionPerformed(@Nonnull AnActionEvent anActionEvent) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(Objects.requireNonNull(anActionEvent.getProject()));
        if (fileEditorManager == null) {
            return;
        }
        LightVirtualFile itemVirtualFile = searchExistingFile(fileEditorManager);
        if (itemVirtualFile == null) {
            itemVirtualFile = new LightVirtualFile("Provide Feedback");
            itemVirtualFile.putUserData(ID, ProvideFeedbackAction.class.getCanonicalName());
            itemVirtualFile.setFileType(new AzureFileType(ProvideFeedbackEditorProvider.TYPE, IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE)));
        }
        final LightVirtualFile finalItemVirtualFile = itemVirtualFile;
        AzureTaskManager.getInstance().runLater(() -> fileEditorManager.openFile(finalItemVirtualFile, true /*focusEditor*/, true /*searchForOpen*/));
    }

    private LightVirtualFile searchExistingFile(FileEditorManager fileEditorManager) {
        LightVirtualFile virtualFile = null;
        for (final VirtualFile editedFile : fileEditorManager.getOpenFiles()) {
            final String fileResourceId = editedFile.getUserData(ID);
            if (fileResourceId != null && fileResourceId.equals(ProvideFeedbackAction.class.getCanonicalName()) &&
                editedFile.getFileType().getName().equals(ProvideFeedbackEditorProvider.TYPE)) {
                virtualFile = (LightVirtualFile) editedFile;
                break;
            }
        }
        return virtualFile;
    }
}
