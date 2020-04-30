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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;
import com.intellij.packaging.impl.compiler.ArtifactsWorkspaceSettings;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
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

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
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
}
