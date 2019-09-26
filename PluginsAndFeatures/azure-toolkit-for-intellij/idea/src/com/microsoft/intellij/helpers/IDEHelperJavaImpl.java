/**
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

import com.google.common.util.concurrent.*;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.impl.compiler.ArtifactCompileScope;
import com.intellij.packaging.impl.compiler.ArtifactsWorkspaceSettings;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class IDEHelperJavaImpl extends IDEHelperImpl {

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
}