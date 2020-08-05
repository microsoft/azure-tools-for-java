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
import com.microsoft.tooling.msservices.helpers.IDEHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class JavaIDEHelper extends IDEHelperImpl {

    @Override
    public List<IDEHelper.ArtifactDescriptor> getArtifacts(
            @NotNull final IDEHelper.ProjectDescriptor projectDescriptor) throws AzureCmdException {

        Project project = findOpenProject(projectDescriptor);

        List<IDEHelper.ArtifactDescriptor> artifactDescriptors = new ArrayList<IDEHelper.ArtifactDescriptor>();

        for (Artifact artifact : ArtifactUtil.getArtifactWithOutputPaths(project)) {
            artifactDescriptors.add(new IDEHelper.ArtifactDescriptor(artifact.getName(), artifact.getArtifactType().getId()));
        }

        return artifactDescriptors;
    }

    @Override
    public ListenableFuture<String> buildArtifact(@NotNull final IDEHelper.ProjectDescriptor projectDescriptor,
                                                  @NotNull final IDEHelper.ArtifactDescriptor artifactDescriptor) {

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
    private static Artifact findProjectArtifact(@NotNull Project project, @NotNull
            IDEHelper.ArtifactDescriptor artifactDescriptor)
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
