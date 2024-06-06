/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.helpers;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;
import com.intellij.packaging.impl.compiler.ArtifactsWorkspaceSettings;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.ApplicationSettings;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.IDEHelper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Slf4j
public class IDEHelperImpl implements IDEHelper {

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

    @Override
    public void setApplicationProperty(@NotNull String name, @NotNull String value) {
        ApplicationSettings.getInstance().setProperty(name, value);
    }

    @Override
    public void unsetApplicationProperty(@NotNull String name) {
        ApplicationSettings.getInstance().unsetProperty(name);
    }

    @Override
    public String getApplicationProperty(@NotNull String name) {
        return ApplicationSettings.getInstance().getProperty(name);
    }

    @Override
    public void setApplicationProperties(@NotNull String name, @NotNull String[] value) {
        ApplicationSettings.getInstance().setProperties(name, value);
    }

    @Override
    public void unsetApplicatonProperties(@NotNull String name) {
        ApplicationSettings.getInstance().unsetProperty(name);
    }

    @Override
    public String[] getApplicationProperties(@NotNull String name) {
        return ApplicationSettings.getInstance().getProperties(name);
    }

    @Override
    public boolean isApplicationPropertySet(@NotNull String name) {
        return ApplicationSettings.getInstance().isPropertySet(name);
    }

    @Override
    public String getProjectSettingsPath() {
        return PluginUtil.getPluginRootDirectory();
    }

    @Override
    public void closeFile(@NotNull final Object projectObject, @NotNull final Object openedFile) {
        AzureTaskManager.getInstance().runLater(() -> FileEditorManager.getInstance((Project) projectObject).closeFile((VirtualFile) openedFile));
    }

    @Override
    public void invokeLater(@NotNull Runnable runnable) {
        AzureTaskManager.getInstance().runLater(runnable, AzureTask.Modality.ANY);
    }

    @Override
    public void invokeAndWait(@NotNull Runnable runnable) {
        AzureTaskManager.getInstance().runAndWait(runnable, AzureTask.Modality.ANY);
    }

    @Override
    public void executeOnPooledThread(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().executeOnPooledThread(runnable);
    }

    @NotNull
    @Override
    public List<ArtifactDescriptor> getArtifacts(@NotNull ProjectDescriptor projectDescriptor)
        throws AzureCmdException {
        final Project project = findOpenProject(projectDescriptor);

        final List<ArtifactDescriptor> artifactDescriptors = new ArrayList<>();

        for (final Artifact artifact : ArtifactUtil.getArtifactWithOutputPaths(project)) {
            artifactDescriptors.add(new ArtifactDescriptor(artifact.getName(), artifact.getArtifactType().getId()));
        }

        return artifactDescriptors;
    }

    @NotNull
    @Override
    public ListenableFuture<String> buildArtifact(@NotNull ProjectDescriptor projectDescriptor,
                                                  @NotNull ArtifactDescriptor artifactDescriptor) {
        try {
            final Project project = findOpenProject(projectDescriptor);

            final Artifact artifact = findProjectArtifact(project, artifactDescriptor);

            final SettableFuture<String> future = SettableFuture.create();

            Futures.addCallback(buildArtifact(project, artifact, false), new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable Boolean succeded) {
                    if (succeded != null && succeded) {
                        future.set(artifact.getOutputFilePath());
                    } else {
                        future.setException(new AzureCmdException("An error occurred while building the artifact"));
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    if (throwable instanceof ExecutionException) {
                        future.setException(new AzureCmdException("An error occurred while building the artifact",
                                                                  throwable.getCause()));
                    } else {
                        future.setException(new AzureCmdException("An error occurred while building the artifact",
                                                                  throwable));
                    }
                }
            }, MoreExecutors.directExecutor());

            return future;
        } catch (final AzureCmdException e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public Object getCurrentProject() {
        return PluginUtil.getSelectedProject();
    }

    @NotNull
    private static byte[] getArray(@NotNull InputStream is) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int readCount;
        final byte[] data = new byte[16384];

        while ((readCount = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, readCount);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

    private static ListenableFuture<Boolean> buildArtifact(@NotNull Project project, final @NotNull Artifact artifact, boolean rebuild) {
        final SettableFuture<Boolean> future = SettableFuture.create();

        final Set<Artifact> artifacts = new LinkedHashSet<>(1);
        artifacts.add(artifact);
        final CompileScope scope = ArtifactCompileScope.createArtifactsScope(project, artifacts, rebuild);
        ArtifactsWorkspaceSettings.getInstance(project).setArtifactsToBuild(artifacts);

        CompilerManager.getInstance(project).make(scope, (aborted, errors, warnings, compileContext) -> future.set(!aborted && errors == 0));

        return future;
    }

    @NotNull
    private static Project findOpenProject(@NotNull ProjectDescriptor projectDescriptor)
        throws AzureCmdException {
        Project project = null;

        for (final Project openProject : ProjectManager.getInstance().getOpenProjects()) {
            if (StringUtils.equals(projectDescriptor.getName(), openProject.getName()) &&
                StringUtils.equals(projectDescriptor.getPath(), openProject.getBasePath())) {
                project = openProject;
                break;
            }
        }

        if (project == null) {
            throw new AzureCmdException("Unable to find an open project with the specified description.");
        }

        return project;
    }

    @NotNull
    private static Artifact findProjectArtifact(@NotNull Project project, @NotNull ArtifactDescriptor artifactDescriptor)
        throws AzureCmdException {
        Artifact artifact = null;

        for (final Artifact projectArtifact : ArtifactUtil.getArtifactWithOutputPaths(project)) {
            if (artifactDescriptor.getName().equals(projectArtifact.getName()) &&
                artifactDescriptor.getArtifactType().equals(projectArtifact.getArtifactType().getId())) {
                artifact = projectArtifact;
                break;
            }
        }

        if (artifact == null) {
            throw new AzureCmdException("Unable to find an artifact with the specified description.");
        }

        return artifact;
    }

    public void openLinkInBrowser(@NotNull String url) {
        try {
            AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(url);
        } catch (final Exception e) {
            DefaultLoader.getUIHelper().showException("Unexpected exception: " + e.getMessage(), e, "Browse Web App", true, false);
            DefaultLoader.getUIHelper().logError("Unexpected exception: " + e.getMessage(), e);
        }
    }

    @Override
    public @Nullable Icon getFileTypeIcon(String name, boolean isDirectory) {
        if (isDirectory) {
            if (Objects.equals(name, "/")) {
                return AllIcons.Nodes.CopyOfFolder;
            }
            return AllIcons.Nodes.Folder;
        }
        final FileType type = FileTypeManager.getInstance().getFileTypeByFileName(name);
        return type.getIcon();
    }

    @AzureOperation(name = "user/$appservice.open_file.file", params = {"target.getName()"}, source = "target.getApp()")
    @SneakyThrows
    public void openAppServiceFile(AppServiceFile target, Object context) {
        final com.microsoft.azure.toolkit.lib.common.action.Action<Void> retry = Action.retryFromFailure((() -> this.openAppServiceFile(target, context)));
        final AppServiceAppBase<?, ?, ?> appService = target.getApp();
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance((Project) context);
        final VirtualFile virtualFile = getOrCreateVirtualFile(target, fileEditorManager);
        final OutputStream output = virtualFile.getOutputStream(null);
        final AzureString title = OperationBundle.description("user/$appservice.open_file.file", virtualFile.getName());
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
                    }, IDEHelperImpl::onRxException);
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
                                final boolean result = DefaultLoader.getUIHelper().showYesNoDialog(
                                    fileEditor.getComponent(), SAVE_CHANGES, APP_SERVICE_FILE_EDITING, Messages.getQuestionIcon());
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

    private void saveFileToAzure(final AppServiceFile appServiceFile, final String content, final Project project) {
        final AzureString title = OperationBundle.description("internal/$appservice.save_file.file", appServiceFile.getName());
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, title, false, () -> {
            final AppServiceAppBase<?, ?, ?> appService = appServiceFile.getApp();
            final AppServiceFile target = appService.getFileByPath(appServiceFile.getPath());
            final boolean deleted = target == null;
            final boolean outDated = !deleted && ZonedDateTime.parse(target.getMtime()).isAfter(ZonedDateTime.parse(appServiceFile.getMtime()));
            boolean toSave = true;
            if (deleted) {
                toSave = DefaultLoader.getUIHelper().showYesNoDialog(null, String.format(FILE_HAS_BEEN_DELETED, appServiceFile.getName()),
                        APP_SERVICE_FILE_EDITING, Messages.getQuestionIcon());
            } else if (outDated) {
                toSave = DefaultLoader.getUIHelper().showYesNoDialog(
                        null, String.format(FILE_HAS_BEEN_MODIFIED, appServiceFile.getName()), APP_SERVICE_FILE_EDITING, Messages.getQuestionIcon());
            }
            if (toSave) {
                appService.uploadFileToPath(content, appServiceFile.getPath());
                PluginUtil.showInfoNotification(APP_SERVICE_FILE_EDITING, String.format(FILE_HAS_BEEN_SAVED, appServiceFile.getName()));
            }
        }));
    }

    @SneakyThrows
    @Override
    public void saveAppServiceFile(@NotNull AppServiceFile file, @NotNull Object context, @Nullable File dest) {
        final Action<Void> retry = Action.retryFromFailure((() -> this.saveAppServiceFile(file, context, dest)));
        final File destFile = Objects.isNull(dest) ? DefaultLoader.getUIHelper().showFileSaver("Download", file.getName()) : dest;
        if (Objects.isNull(destFile)) {
            return;
        }
        final OutputStream output = new FileOutputStream(destFile);
        final Project project = (Project) context;
        final AzureString title = OperationBundle.description("user/$appservice.download_file.file", file.getName());
        final AzureTask<Void> task = new AzureTask<>(project, title, false, () -> {
            ProgressManager.getInstance().getProgressIndicator().setIndeterminate(true);
            file.getApp()
                    .getFileContent(file.getPath())
                    .doOnComplete(() -> notifyDownloadSuccess(file.getName(), destFile, ((Project) context)))
                    .doOnTerminate(() -> IOUtils.closeQuietly(output, null))
                    .subscribe(bytes -> {
                        try {
                            if (bytes != null) {
                                output.write(bytes.array(), 0, bytes.limit());
                            }
                        } catch (final IOException e) {
                            final String error = "failed to write data into local file";
                            final String action = "try later";
                            throw new AzureToolkitRuntimeException(error, e, action, retry);
                        }
                    }, IDEHelperImpl::onRxException);
        });
        AzureTaskManager.getInstance().runInModal(task);
    }

    private static void onRxException(Throwable e) {
        final Throwable rootCause = ExceptionUtils.getRootCause(e);
        if (rootCause instanceof InterruptedIOException || rootCause instanceof InterruptedException) {
            // Swallow interrupted exception caused by unsubscribe
            return;
        }
        AzureMessager.getMessager().error(e);
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

            @Override
            public ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
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

            @Override
            public ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        });
        Notifications.Bus.notify(notification);
    }

    private synchronized VirtualFile getOrCreateVirtualFile(final AppServiceFile file, FileEditorManager manager) {
        synchronized (file) {
            return Arrays.stream(manager.getOpenFiles())
                .filter(f -> StringUtils.equals(f.getUserData(APP_SERVICE_FILE_ID), file.getId()))
                .findFirst().orElseGet(() -> createVirtualFile(file.getId(), file.getFullName(), manager));
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
}
