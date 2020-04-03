/**
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2018 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.helpers;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.tasks.CancellableTask;
import com.microsoft.intellij.ApplicationSettings;
import com.microsoft.intellij.AzureSettings;
import com.microsoft.intellij.helpers.tasks.CancellableTaskHandleImpl;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.IDEHelper;

import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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
//        return PropertiesComponent.getInstance().getValue(name);
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
//        PropertiesComponent.getInstance().setValue(name, value);
//        ApplicationManager.getApplication().invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                ApplicationManager.getApplication().saveSettings();
//            }
//        }, ModalityState.any());
    }

    @Override
    public void setProperty(@NotNull String name, @NotNull String value, Object projectObject) {
        AzureSettings.getSafeInstance((Project) projectObject).setProperty(name, value);
    }

    @Override
    public void unsetProperty(@NotNull String name) {
        AzureSettings.getSafeInstance(PluginUtil.getSelectedProject()).unsetProperty(name);
//        PropertiesComponent.getInstance().unsetValue(name);
//        ApplicationManager.getApplication().invokeLater(new Runnable() {
//            @Override
//            public void run() {
//                ApplicationManager.getApplication().saveSettings();
//            }
//        }, ModalityState.any());
    }

    @Override
    public void unsetProperty(@NotNull String name, Object projectObject) {
        AzureSettings.getSafeInstance((Project) projectObject).unsetProperty(name);
    }

    @Override
    public boolean isPropertySet(@NotNull String name) {
        return AzureSettings.getSafeInstance(PluginUtil.getSelectedProject()).isPropertySet(name);
//        return PropertiesComponent.getInstance().isValueSet(name);
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
//        PropertiesComponent.getInstance().setValues(name, value);
//        ApplicationManager.getApplication().saveSettings();
    }

    @NotNull
    @Override
    public List<ArtifactDescriptor> getArtifacts(@NotNull ProjectDescriptor projectDescriptor)
            throws AzureCmdException {
        return new ArrayList<>();
    }

    @NotNull
    @Override
    public ListenableFuture<String> buildArtifact(@NotNull ProjectDescriptor projectDescriptor,
                                                  @NotNull ArtifactDescriptor artifactDescriptor) {

        return Futures.immediateFuture(null);

        // TODO: SD -- check this change from upstream (start)
//        try {
//            Project project = findOpenProject(projectDescriptor);
//
//            final Artifact artifact = findProjectArtifact(project, artifactDescriptor);
//
//            final SettableFuture<String> future = SettableFuture.create();
//
//            Futures.addCallback(buildArtifact(project, artifact, false), new FutureCallback<Boolean>() {
//                @Override
//                public void onSuccess(@Nullable Boolean succeded) {
//                    if (succeded != null && succeded) {
//                        future.set(artifact.getOutputFilePath());
//                    } else {
//                        future.setException(new AzureCmdException("An error occurred while building the artifact"));
//                    }
//                }
//
//                @Override
//                public void onFailure(Throwable throwable) {
//                    if (throwable instanceof ExecutionException) {
//                        future.setException(new AzureCmdException("An error occurred while building the artifact",
//                                throwable.getCause()));
//                    } else {
//                        future.setException(new AzureCmdException("An error occurred while building the artifact",
//                                throwable));
//                    }
//                }
//            }, MoreExecutors.directExecutor());
//
//            return future;
//        } catch (AzureCmdException e) {
//            return Futures.immediateFailedFuture(e);
//        }
        // TODO: SD -- check this change from upstream (end)
    }

    @Override
    public Object getCurrentProject() {
        return PluginUtil.getSelectedProject();
    }

    @NotNull
    protected static Project findOpenProject(@NotNull ProjectDescriptor projectDescriptor)
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
        } catch (Throwable e) {
            DefaultLoader.getUIHelper().logError("Unexpected exception: " + e.getMessage(), e);
            throw new RuntimeException("Browse web app exception", e);
        }
    }
}