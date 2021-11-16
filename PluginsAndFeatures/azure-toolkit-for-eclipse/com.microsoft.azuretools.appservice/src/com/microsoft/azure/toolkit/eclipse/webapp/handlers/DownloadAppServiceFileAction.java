/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.webapp.handlers;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;

public class DownloadAppServiceFileAction {

    public static void downloadAppServiceFile(final AppServiceFile file) {
        final File destFile = DefaultLoader.getUIHelper().showFileSaver(String.format("Download %s", file.getName()), file.getName());
        if (destFile == null) {
            return;
        }
        try {
            OutputStream output = new FileOutputStream(destFile);
            final AzureString title = AzureString.format("appservice|file.download", file.getName());
            final AzureTask<Void> task = new AzureTask<>(null, title, false, () -> {
                file.getApp()
                        .getFileContent(file.getPath())
                        .doOnComplete(() -> notifyDownloadSuccess(file, destFile))
                        .doOnTerminate(() -> IOUtils.closeQuietly(output))
                        .subscribe(bytes -> {
                            try {
                                if (bytes != null) {
                                    output.write(bytes.array(), 0, bytes.limit());
                                }
                            } catch (final IOException exception) {
                                final String error = "failed to write data into local file";
                                final String action = "try later";
                                throw new AzureToolkitRuntimeException(error, exception, action);
                            }
                        });
            });
            AzureTaskManager.getInstance().runInModal(task);
        } catch (FileNotFoundException e1) {
            AzureMessager.getMessager().error(AzureString.format("Target file %s does not exists", destFile.getAbsolutePath()));
        }
    }

    private static void notifyDownloadSuccess(AppServiceFile file, File destFile) {
        final Action<?>[] actions = Stream.of(getOpenInExplorerAction(destFile), getOpenFileAction(destFile))
                .filter(Objects::nonNull).toArray(Action<?>[]::new);
        AzureMessager.getMessager().info(
                AzureString.format("%s has been saved to %s", file.getName(), destFile.getAbsolutePath()),
                "Azure Toolkit for Eclipse", actions);
    }

    // todo: migrate to eclipse action "org.eclipse.ui.ide.showInSystemExplorer"
    private static Action<?> getOpenInExplorerAction(final File file) {
        if (!(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN))) {
            return null;
        }
        return new Action<Object>(ignore -> {
            AzureTaskManager.getInstance().runLater(() -> {
                try {
                    Desktop.getDesktop().open(file.getParentFile());
                } catch (IOException e) {
                    AzureMessager.getMessager().error(AzureString.format("Failed to open explorer for file %s, %s",
                            file.getName(), e.getMessage()));
                }
            });
        }, new ActionView.Builder("Open in explorer"));
    }

    private static Action<?> getOpenFileAction(final File file) {
        return new Action<Object>(ignore -> {
            if (file.exists() && file.isFile()) {
                IFileStore fileStore = EFS.getLocalFileSystem().getStore(file.toURI());
                final IWorkbenchWindow window = Optional
                        .ofNullable(PlatformUI.getWorkbench().getActiveWorkbenchWindow())
                        .orElseGet(() -> PlatformUI.getWorkbench().getWorkbenchWindows()[0]);
                final IWorkbenchPage page = window.getActivePage();
                AzureTaskManager.getInstance().runLater(() -> {
                    try {
                        IDE.openEditorOnFileStore(page, fileStore);
                    } catch (Exception e) {
                        AzureMessager.getMessager().warning(
                                AzureString.format("Failed to open file %s, %s", file.getName(), e.getMessage()));
                    }
                });
            }
        }, new ActionView.Builder("Open in editor"));
    }
}
