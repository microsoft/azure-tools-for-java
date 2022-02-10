/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.appservice.actions;

import com.intellij.ide.actions.RevealFileAction;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.intellij.common.FileChooser;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;

// todo: Clean up duplicate codes in UIHelper and IDEHelper
public class AppServiceFileAction {
    private static final String APP_SERVICE_FILE_EDITING = "App Service File Editing";
    private static final String FILE_HAS_BEEN_DELETED = "File '%s' has been deleted from remote server, " +
            "do you want to create a new file with the changed content?";
    private static final String FILE_HAS_BEEN_MODIFIED = "File '%s' has been modified since you view it, do you still want to save your changes?";
    private static final String SAVE_CHANGES = "Do you want to save your changes?";
    private static final Key<String> APP_SERVICE_FILE_ID = new Key<>("APP_SERVICE_FILE_ID");
    private static final String ERROR_DOWNLOADING = "Failed to download file[%s] to [%s].";
    private static final String SUCCESS_DOWNLOADING = "File[%s] is successfully downloaded to [%s].";
    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";
    private static final String FILE_HAS_BEEN_SAVED = "File %s has been saved to Azure";

    @AzureOperation(
            name = "appservice.open_file.file",
            params = {"target.getName()"},
            type = AzureOperation.Type.SERVICE
    )
    @SneakyThrows
    public void openAppServiceFile(AppServiceFile target, Object context) {
        final Action<Void> retry = Action.retryFromFailure((() -> this.openAppServiceFile(target, context)));
        final IAppService appService = target.getApp();
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance((Project) context);
        final VirtualFile virtualFile = getOrCreateVirtualFile(target, fileEditorManager);
        final OutputStream output = virtualFile.getOutputStream(null);
        final AzureString title = AzureOperationBundle.title("appservice.open_file.file", virtualFile.getName());
        final AzureTask<Void> task = new AzureTask<>(null, title, false, () -> {
            final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
            indicator.setIndeterminate(true);
            indicator.setText2("Checking file existence");
            final AppServiceFile file = appService.getFileByPath(target.getPath());
            if (file == null) {
                final String failureFileDeleted = String.format("Target file (%s) has been deleted", target.getName());
                UIUtil.invokeLaterIfNeeded(() -> Messages.showWarningDialog(failureFileDeleted, "Open File"));
                return;
            }
            indicator.setText2("Loading file content");
            final String failure = String.format("Can not open file (%s). Try downloading it first and open it manually.", virtualFile.getName());
            appService
                    .getFileContent(file.getPath())
                    .doOnComplete(() -> AzureTaskManager.getInstance().runLater(() -> {
                        final Consumer<String> contentSaver = content -> saveFileToAzure(target, content, fileEditorManager.getProject());
                        if (!openFileInEditor(contentSaver, virtualFile, fileEditorManager)) {
                            Messages.showWarningDialog(failure, "Open File");
                        }
                    }, AzureTask.Modality.NONE))
                    .doAfterTerminate(() -> IOUtils.closeQuietly(output, null))
                    .subscribe(bytes -> {
                        try {
                            if (bytes != null) {
                                output.write(bytes.array(), 0, bytes.limit());
                            }
                        } catch (final IOException e) {
                            final String error = "failed to load data into editor";
                            final String action = "try later or downloading it first";
                            throw new AzureToolkitRuntimeException(error, e, action, retry);
                        }
                    }, AppServiceFileAction::onRxException);
        });
        AzureTaskManager.getInstance().runInModal(task);
    }

    private boolean openFileInEditor(final Consumer<? super String> contentSaver, VirtualFile virtualFile, FileEditorManager fileEditorManager) {
        final FileEditor[] editors = fileEditorManager.openFile(virtualFile, true, true);
        if (editors.length == 0) {
            return false;
        }
        for (final FileEditor fileEditor : editors) {
            if (fileEditor instanceof TextEditor) {
                final String originContent = getTextEditorContent((TextEditor) fileEditor);
                final MessageBusConnection messageBusConnection = fileEditorManager.getProject().getMessageBus().connect(fileEditor);
                messageBusConnection.subscribe(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, new FileEditorManagerListener.Before() {
                    @Override
                    public void beforeFileClosed(FileEditorManager source, VirtualFile file) {
                        try {
                            final String content = getTextEditorContent((TextEditor) fileEditor);
                            if (file == virtualFile && !StringUtils.equals(content, originContent)) {
                                final boolean result = AzureMessager.getMessager().confirm(SAVE_CHANGES, APP_SERVICE_FILE_EDITING);
                                if (result) {
                                    contentSaver.consume(content);
                                }
                            }
                        } catch (final RuntimeException e) {
                            AzureMessager.getMessager().error(e);
                        } finally {
                            messageBusConnection.disconnect();
                        }
                    }
                });
            }
        }
        return true;
    }

    private static String getTextEditorContent(TextEditor textEditor) {
        return textEditor.getEditor().getDocument().getText();
    }

    @AzureOperation(
            name = "appservice.save_file.file",
            params = {"appServiceFile.getName()"},
            type = AzureOperation.Type.SERVICE
    )
    private void saveFileToAzure(final AppServiceFile appServiceFile, final String content, final Project project) {
        final AzureString title = AzureOperationBundle.title("appservice.save_file.file", appServiceFile.getName());
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, title, false, () -> {
            final IAppService appService = appServiceFile.getApp();
            final AppServiceFile target = appService.getFileByPath(appServiceFile.getPath());
            final boolean deleted = target == null;
            final boolean outDated = !deleted && ZonedDateTime.parse(target.getMtime()).isAfter(ZonedDateTime.parse(appServiceFile.getMtime()));
            boolean toSave = true;
            if (deleted) {
                toSave = AzureMessager.getMessager().confirm(String.format(FILE_HAS_BEEN_DELETED, appServiceFile.getName()), APP_SERVICE_FILE_EDITING);
            } else if (outDated) {
                toSave = AzureMessager.getMessager().confirm(String.format(FILE_HAS_BEEN_MODIFIED, appServiceFile.getName()), APP_SERVICE_FILE_EDITING);
            }
            if (toSave) {
                appService.uploadFileToPath(content, appServiceFile.getPath());
                AzureMessager.getMessager().info(String.format(FILE_HAS_BEEN_SAVED, appServiceFile.getName()), APP_SERVICE_FILE_EDITING);
            }
        }));
    }

    @SneakyThrows
    public void saveAppServiceFile(@NotNull AppServiceFile file, @Nullable Project project, @Nullable File dest) {
        final Action<Void> retry = Action.retryFromFailure((() -> this.saveAppServiceFile(file, project, dest)));
        final File destFile = Objects.isNull(dest) ? FileChooser.showFileSaver("Download", file.getName()) : dest;
        if (Objects.isNull(destFile)) {
            return;
        }
        final OutputStream output = new FileOutputStream(destFile);
        final AzureString title = AzureOperationBundle.title("appservice.download_file.file", file.getName());
        final AzureTask<Void> task = new AzureTask<>(project, title, false, () -> {
            ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
            file.getApp()
                    .getFileContent(file.getPath())
                    .doOnComplete(() -> notifyDownloadSuccess(file.getName(), destFile, project))
                    .doOnTerminate(() -> IOUtils.closeQuietly(output, null))
                    .subscribe(bytes -> {
                        try {
                            if (bytes != null) {
                                output.write(bytes.array(), 0, bytes.limit());
                            }
                        } catch (final IOException e) {
                            final String error = "failed to write data into local file";
                            throw new AzureToolkitRuntimeException(error, e, retry);
                        }
                    }, AppServiceFileAction::onRxException);
        });
        AzureTaskManager.getInstance().runInModal(task);
    }

    private synchronized VirtualFile getOrCreateVirtualFile(final AppServiceFile file, FileEditorManager manager) {
        synchronized (file) {
            return Arrays.stream(manager.getOpenFiles())
                    .filter(f -> StringUtils.equals(f.getUserData(APP_SERVICE_FILE_ID), file.getId()))
                    .findFirst().orElse(createVirtualFile(file.getId(), file.getFullName(), manager));
        }
    }

    @SneakyThrows
    private LightVirtualFile createVirtualFile(final String fileId, final String fullName, FileEditorManager manager) {
        final LightVirtualFile virtualFile = new LightVirtualFile(fullName);
        virtualFile.setFileType(FileTypeManager.getInstance().getFileTypeByFileName(fullName));
        virtualFile.setCharset(StandardCharsets.UTF_8);
        virtualFile.putUserData(APP_SERVICE_FILE_ID, fileId);
        virtualFile.setWritable(true);
        return virtualFile;
    }

    private void notifyDownloadSuccess(final String name, final File dest, final Project project) {
        final String title = "File downloaded";
        final File directory = dest.getParentFile();
        final String message = String.format(SUCCESS_DOWNLOADING, name, directory.getAbsolutePath());
        final Notification notification = new Notification(NOTIFICATION_GROUP_ID, title, message, NotificationType.INFORMATION);
        notification.addAction(new AnAction(RevealFileAction.getActionName()) {
            @Override
            public void actionPerformed(@NotNull final AnActionEvent anActionEvent) {
                RevealFileAction.openFile(dest);
            }
        });
        notification.addAction(new AnAction("Open In Editor") {
            @Override
            public void actionPerformed(@NotNull final AnActionEvent anActionEvent) {
                final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                final VirtualFile virtualFile = VfsUtil.findFileByIoFile(dest, true);
                if (Objects.nonNull(virtualFile)) {
                    fileEditorManager.openFile(virtualFile, true, true);
                }
            }
        });
        Notifications.Bus.notify(notification);
    }

    private static void onRxException(Throwable e) {
        final Throwable rootCause = ExceptionUtils.getRootCause(e);
        if (rootCause instanceof InterruptedIOException || rootCause instanceof InterruptedException) {
            // Swallow interrupted exception caused by unsubscribe
            return;
        }
        AzureMessager.getMessager().error(e);
    }
}
