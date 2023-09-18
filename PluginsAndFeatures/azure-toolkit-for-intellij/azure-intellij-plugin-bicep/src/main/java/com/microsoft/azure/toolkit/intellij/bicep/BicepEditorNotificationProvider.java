/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.bicep;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.editor.EditorBundle;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.dotnet.DotnetRuntimeHandler;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.swing.*;

public class BicepEditorNotificationProvider extends EditorNotifications.Provider {
    public static final ComparableVersion BICEP_MIN_VERSION = new ComparableVersion("6.0.0");
    private static final Key<Boolean> DISABLE_NOTIFICATION = Key.create("azure.bicep.editor.preview.notification.disable");
    private static final Key<Boolean> RUNTIME_NOTIFICATION_SHOWN = Key.create("azure.bicep.editor.runtime.notification.shown");
    private static final Key<Boolean> PREVIEW_NOTIFICATION_SHOWN = Key.create("azure.bicep.editor.preview.notification.shown");

    @Nullable
    @Override
    public JComponent createNotificationPanel(@NotNull VirtualFile file, @NotNull FileEditor editor, @NotNull Project project) {
        if (file.getFileType() instanceof BicepFileType) {
            if (!DotnetRuntimeHandler.isDotnetRuntimeInstalled() && BooleanUtils.isNotTrue(editor.getUserData(RUNTIME_NOTIFICATION_SHOWN))) {
                final EditorNotificationPanel panel = new EditorNotificationPanel(editor);
                panel.setText(".NET runtime (newer than v6.0) is required for full Bicep language support, but it's not found or outdated.");
                final AzureActionManager am = AzureActionManager.getInstance();
                final Action<Object> openSettingsAction = am.getAction(ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS);
                panel.createActionLabel("Install .NET Runtime", () -> am.getAction(ResourceCommonActionsContributor.INSTALL_DOTNET_RUNTIME).handle(null));
                panel.createActionLabel("Configure", () -> am.getAction(ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS).handle(null));
                panel.createActionLabel(IdeBundle.message("file.changed.externally.reload"), () -> {
                    if (!project.isDisposed()) {
                        file.refresh(false, false);
                        EditorNotifications.getInstance(project).updateNotifications(file);
                    }
                });
                editor.putUserData(RUNTIME_NOTIFICATION_SHOWN, true);
                return panel;
            } else if (!PropertiesComponent.getInstance().isTrueValue(DISABLE_NOTIFICATION.toString()) && BooleanUtils.isNotTrue(editor.getUserData(PREVIEW_NOTIFICATION_SHOWN))) {
                final EditorNotificationPanel panel = new EditorNotificationPanel(editor);
                panel.setText("Bicep file support is still in preview.");
                panel.createActionLabel("Report an issue", () -> BrowserUtil.browse("https://aka.ms/azure-ij-new-issue"));
                panel.createActionLabel(EditorBundle.message("notification.dont.show.again.message"), () -> {
                    PropertiesComponent.getInstance().setValue(DISABLE_NOTIFICATION.toString(), "true");
                    EditorNotifications.getInstance(project).updateAllNotifications();
                });
                editor.putUserData(PREVIEW_NOTIFICATION_SHOWN, true);
                return panel;
            }
        }
        return null;
    }

    @Override
    public @NotNull Key getKey() {
        return Key.create(BicepEditorNotificationProvider.class.getCanonicalName());
    }
}
