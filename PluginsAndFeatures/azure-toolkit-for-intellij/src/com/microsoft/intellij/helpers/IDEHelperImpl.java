/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.helpers;

import com.google.common.util.concurrent.*;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;
import com.intellij.packaging.impl.compiler.ArtifactsWorkspaceSettings;
import com.intellij.testFramework.LightVirtualFile;
import com.microsoft.azure.toolkit.lib.appservice.file.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.file.AppServiceFileService;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.tasks.CancellableTask;
import com.microsoft.intellij.ApplicationSettings;
import com.microsoft.intellij.AzureSettings;
import com.microsoft.intellij.helpers.tasks.CancellableTaskHandleImpl;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.IDEHelper;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.apache.commons.lang.StringUtils;
import rx.Observable;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

@Log
public class IDEHelperImpl implements IDEHelper {
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
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                FileEditorManager.getInstance((Project) projectObject).closeFile((VirtualFile) openedFile);
            }
        });
    }

    @Override
    public void invokeLater(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable, ModalityState.any());
    }

    @Override
    public void invokeAndWait(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().invokeAndWait(runnable, ModalityState.any());
    }

    @Override
    public void executeOnPooledThread(@NotNull Runnable runnable) {
        ApplicationManager.getApplication().executeOnPooledThread(runnable);
    }

    @Override
    public void runInBackground(@Nullable final Object project, @NotNull final String name, final boolean canBeCancelled,
                                final boolean isIndeterminate, @Nullable final String indicatorText,
                                final Runnable runnable) {
        // background tasks via ProgressManager can be scheduled only on the
        // dispatch thread
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ProgressManager.getInstance().run(new Task.Backgroundable((Project) project,
                                                                          name, canBeCancelled) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        if (isIndeterminate) {
                            indicator.setIndeterminate(true);
                        }

                        if (indicatorText != null) {
                            indicator.setText(indicatorText);
                        }

                        runnable.run();
                    }
                });
            }
        }, ModalityState.any());
    }

    @NotNull
    @Override
    public CancellableTask.CancellableTaskHandle runInBackground(@NotNull ProjectDescriptor projectDescriptor,
                                                                 @NotNull final String name,
                                                                 @Nullable final String indicatorText,
                                                                 @NotNull final CancellableTask cancellableTask)
        throws AzureCmdException {
        final CancellableTaskHandleImpl handle = new CancellableTaskHandleImpl();
        final Project project = findOpenProject(projectDescriptor);

        // background tasks via ProgressManager can be scheduled only on the
        // dispatch thread
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                ProgressManager.getInstance().run(getCancellableBackgroundTask(project, name, indicatorText, handle, cancellableTask));
            }
        }, ModalityState.any());

        return handle;
    }

    @Nullable
    @Override
    public String getProperty(@NotNull String name) {
        return AzureSettings.getSafeInstance(PluginUtil.getSelectedProject()).getProperty(name);
    }

    public String getProperty(@NotNull String name, Object projectObject) {
        return AzureSettings.getSafeInstance((Project) projectObject).getProperty(name);
    }

    @NotNull
    @Override
    public String getPropertyWithDefault(@NotNull String name, @NotNull String defaultValue) {
        return PropertiesComponent.getInstance().getValue(name, defaultValue);
    }

    @Override
    public void setProperty(@NotNull String name, @NotNull String value) {
        AzureSettings.getSafeInstance(PluginUtil.getSelectedProject()).setProperty(name, value);
    }

    @Override
    public void setProperty(@NotNull String name, @NotNull String value, Object projectObject) {
        AzureSettings.getSafeInstance((Project) projectObject).setProperty(name, value);
    }

    @Override
    public void unsetProperty(@NotNull String name) {
        AzureSettings.getSafeInstance(PluginUtil.getSelectedProject()).unsetProperty(name);
    }

    @Override
    public void unsetProperty(@NotNull String name, Object projectObject) {
        AzureSettings.getSafeInstance((Project) projectObject).unsetProperty(name);
    }

    @Override
    public boolean isPropertySet(@NotNull String name) {
        return AzureSettings.getSafeInstance(PluginUtil.getSelectedProject()).isPropertySet(name);
    }

    @Nullable
    @Override
    public String[] getProperties(@NotNull String name) {
        return AzureSettings.getSafeInstance(PluginUtil.getSelectedProject()).getProperties(name);
    }

    @Nullable
    @Override
    public String[] getProperties(@NotNull String name, Object projectObject) {
        return AzureSettings.getSafeInstance((Project) projectObject).getProperties(name);
    }

    @Override
    public void setProperties(@NotNull String name, @NotNull String[] value) {
        AzureSettings.getSafeInstance(PluginUtil.getSelectedProject()).setProperties(name, value);
    }

    @NotNull
    @Override
    public List<ArtifactDescriptor> getArtifacts(@NotNull ProjectDescriptor projectDescriptor)
        throws AzureCmdException {
        Project project = findOpenProject(projectDescriptor);

        List<ArtifactDescriptor> artifactDescriptors = new ArrayList<ArtifactDescriptor>();

        for (Artifact artifact : ArtifactUtil.getArtifactWithOutputPaths(project)) {
            artifactDescriptors.add(new ArtifactDescriptor(artifact.getName(), artifact.getArtifactType().getId()));
        }

        return artifactDescriptors;
    }

    @NotNull
    @Override
    public ListenableFuture<String> buildArtifact(@NotNull ProjectDescriptor projectDescriptor,
                                                  @NotNull ArtifactDescriptor artifactDescriptor) {
        try {
            Project project = findOpenProject(projectDescriptor);

            final Artifact artifact = findProjectArtifact(project, artifactDescriptor);

            final SettableFuture<String> future = SettableFuture.create();

            Futures.addCallback(buildArtifact(project, artifact, false), new FutureCallback<Boolean>() {
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
        } catch (AzureCmdException e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public Object getCurrentProject() {
        return PluginUtil.getSelectedProject();
    }

    @NotNull
    private static byte[] getArray(@NotNull InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int readCount;
        byte[] data = new byte[16384];

        while ((readCount = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, readCount);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

    private static ListenableFuture<Boolean> buildArtifact(@NotNull Project project, final @NotNull Artifact artifact, boolean rebuild) {
        final SettableFuture<Boolean> future = SettableFuture.create();

        Set<Artifact> artifacts = new LinkedHashSet<Artifact>(1);
        artifacts.add(artifact);
        CompileScope scope = ArtifactCompileScope.createArtifactsScope(project, artifacts, rebuild);
        ArtifactsWorkspaceSettings.getInstance(project).setArtifactsToBuild(artifacts);

        CompilerManager.getInstance(project).make(scope, new CompileStatusNotification() {
            @Override
            public void finished(boolean aborted, int errors, int warnings, CompileContext compileContext) {
                future.set(!aborted && errors == 0);
            }
        });

        return future;
    }

    @NotNull
    private static Project findOpenProject(@NotNull ProjectDescriptor projectDescriptor)
        throws AzureCmdException {
        Project project = null;

        for (Project openProject : ProjectManager.getInstance().getOpenProjects()) {
            if (projectDescriptor.getName().equals(openProject.getName())
                && projectDescriptor.getPath().equals(openProject.getBasePath())) {
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

        for (Artifact projectArtifact : ArtifactUtil.getArtifactWithOutputPaths(project)) {
            if (artifactDescriptor.getName().equals(projectArtifact.getName())
                && artifactDescriptor.getArtifactType().equals(projectArtifact.getArtifactType().getId())) {
                artifact = projectArtifact;
                break;
            }
        }

        if (artifact == null) {
            throw new AzureCmdException("Unable to find an artifact with the specified description.");
        }

        return artifact;
    }

    @org.jetbrains.annotations.NotNull
    private static Task.Backgroundable getCancellableBackgroundTask(final Project project,
                                                                    @NotNull final String name,
                                                                    @Nullable final String indicatorText,
                                                                    final CancellableTaskHandleImpl handle,
                                                                    @NotNull final CancellableTask cancellableTask) {
        return new Task.Backgroundable(project,
                                       name, true) {
            private final Semaphore lock = new Semaphore(0);

            @Override
            public void run(@org.jetbrains.annotations.NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                handle.setProgressIndicator(indicator);

                if (indicatorText != null) {
                    indicator.setText(indicatorText);
                }

                ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            cancellableTask.run(handle);
                        } catch (Throwable t) {
                            handle.setException(t);
                        } finally {
                            lock.release();
                        }
                    }
                });

                try {
                    while (!lock.tryAcquire(1, TimeUnit.SECONDS)) {
                        if (handle.isCancelled()) {
                            ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                                @Override
                                public void run() {
                                    cancellableTask.onCancel();
                                }
                            });

                            return;
                        }
                    }

                    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
                        @Override
                        public void run() {
                            if (handle.getException() == null) {
                                cancellableTask.onSuccess();
                            } else {
                                cancellableTask.onError(handle.getException());
                            }
                        }
                    });
                } catch (InterruptedException ignored) {
                }
            }
        };
    }

    public void openLinkInBrowser(@NotNull String url) {
        try {
            BrowserUtil.browse(url);
        } catch (Exception e) {
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

    private static final Key<String> APP_SERVICE_FILE_ID = new Key<>("APP_SERVICE_FILE_ID");
    private static final String ERROR_DOWNLOADING = "Failed to download file[%s] to [%s].";
    private static final String SUCCESS_DOWNLOADING = "File[%s] is successfully downloaded to [%s].";
    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";

    @SneakyThrows
    public void openAppServiceFile(final AppServiceFile file, Object context) {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance((Project) context);
        final VirtualFile virtualFile = getOrCreateVirtualFile(file, fileEditorManager);
        final String error = String.format("Error occurs while opening file[%s].", virtualFile.getName());
        final String failure = String.format("Can not open file %s. Try downloading it first and open it manually.", virtualFile.getName());
        final Consumer<Throwable> errorHandler = (Throwable e) -> {
            log.log(Level.WARNING, error, e);
            DefaultLoader.getUIHelper().showException(error, e, "Error Opening File", false, true);
        };
        final String title = String.format("Opening file %s...", virtualFile.getName());
        final Task.Modal task = new Task.Modal(null, title, true) {
            @SneakyThrows
            @Override
            public void run(ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                writeContentTo(virtualFile.getOutputStream(null), file, errorHandler)
                    .doOnError(errorHandler::accept)
                    .doOnCompleted(() -> ApplicationManager.getApplication().invokeLater(() -> {
                        if (fileEditorManager.openFile(virtualFile, true, true).length == 0) {
                            Messages.showWarningDialog(failure, "Open File");
                        }
                    }, ModalityState.NON_MODAL))
                    .subscribe();
            }
        };
        ProgressManager.getInstance().run(task);
    }

    /**
     * user is asked to choose where to save the file is @param dest is null
     */
    public void saveAppServiceFile(@NotNull final AppServiceFile file, @NotNull Object context, @Nullable File dest) {
        final File destFile = Objects.isNull(dest) ? DefaultLoader.getUIHelper().showFileSaver("Download", file.getName()) : dest;
        if (Objects.isNull(destFile)) {
            return;
        }
        final Project project = (Project) context;
        final String error = String.format(ERROR_DOWNLOADING, file.getName(), destFile.getAbsolutePath());
        final String success = String.format(SUCCESS_DOWNLOADING, file.getName(), destFile.getAbsolutePath());
        final Consumer<Throwable> errorHandler = (Throwable e) -> {
            log.log(Level.WARNING, error, e);
            UIUtils.showNotification(project, error, MessageType.ERROR);
        };
        final String title = String.format("Downloading file %s...", file.getName());
        final Task.Backgroundable task = new Task.Backgroundable(project, title, true) {
            @SneakyThrows
            @Override
            public void run(ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                writeContentTo(new FileOutputStream(destFile), file, errorHandler)
                    .doOnError(errorHandler::accept)
                    .doOnCompleted(() -> notifyDownloadSuccess(file, destFile, ((Project) context)))
                    .subscribe();
            }
        };
        ProgressManager.getInstance().run(task);
    }

    private void notifyDownloadSuccess(final AppServiceFile file, final File dest, final Project project) {
        final String title = "File downloaded";
        final File directory = dest.getParentFile();
        final String message = String.format("File [%s] is successfully downloaded into [%s]", file.getName(), directory.getAbsolutePath());
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

    @SneakyThrows
    private Observable<byte[]> writeContentTo(final OutputStream output,
                                              final AppServiceFile file,
                                              final Consumer<? super Throwable> errorHandler) {
        final Observable<byte[]> content = AppServiceFileService.forApp(file.getApp()).getFileContent(file.getPath());
        return content.doOnTerminate(() -> {
            try {
                output.flush();
                output.close();
            } catch (final IOException e) {
                errorHandler.accept(e);
            }
        }).doOnNext((bytes) -> {
            try {
                output.write(bytes);
            } catch (final IOException e) {
                errorHandler.accept(e);
            }
        });
    }

    private VirtualFile getOrCreateVirtualFile(AppServiceFile file, FileEditorManager manager) {
        synchronized (file) {
            return Arrays.stream(manager.getOpenFiles())
                         .filter(f -> StringUtils.equals(f.getUserData(APP_SERVICE_FILE_ID), file.getId()))
                         .findFirst().orElse(createVirtualFile(file, manager));
        }
    }

    @SneakyThrows
    private LightVirtualFile createVirtualFile(AppServiceFile file, FileEditorManager manager) {
        final LightVirtualFile virtualFile = new LightVirtualFile(file.getFullName());
        virtualFile.setFileType(FileTypeManager.getInstance().getFileTypeByFileName(file.getName()));
        virtualFile.setCharset(StandardCharsets.UTF_8);
        virtualFile.putUserData(APP_SERVICE_FILE_ID, file.getId());
        virtualFile.setWritable(true);
        return virtualFile;
    }
}
