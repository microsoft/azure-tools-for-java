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

package com.microsoft.tooling.msservices.helpers;

import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.azure.toolkit.lib.appservice.file.AppServiceFile;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.tasks.CancellableTask;

import javax.swing.*;
import java.io.File;
import java.util.List;

public interface IDEHelper {
    class ProjectDescriptor {
        @NotNull
        private final String name;
        @NotNull
        private final String path;

        public ProjectDescriptor(@NotNull String name, @NotNull String path) {
            this.name = name;
            this.path = path;
        }

        @NotNull
        public String getName() {
            return name;
        }

        @NotNull
        public String getPath() {
            return path;
        }
    }

    class ArtifactDescriptor {
        @NotNull
        private String name;
        @NotNull
        private String artifactType;

        public ArtifactDescriptor(@NotNull String name, @NotNull String artifactType) {
            this.name = name;
            this.artifactType = artifactType;
        }

        @NotNull
        public String getName() {
            return name;
        }

        @NotNull
        public String getArtifactType() {
            return artifactType;
        }
    }

    String getProjectSettingsPath();

    void closeFile(@NotNull Object projectObject, @NotNull Object openedFile);

    void invokeLater(@NotNull Runnable runnable);

    void invokeAndWait(@NotNull Runnable runnable);

    void executeOnPooledThread(@NotNull Runnable runnable);

    void runInBackground(@Nullable Object project, @NotNull String name, boolean canBeCancelled,
                         boolean isIndeterminate, @Nullable String indicatorText,
                         Runnable runnable);

    @NotNull
    CancellableTask.CancellableTaskHandle runInBackground(@NotNull ProjectDescriptor projectDescriptor,
                                                          @NotNull String name,
                                                          @Nullable String indicatorText,
                                                          @NotNull CancellableTask cancellableTask) throws AzureCmdException;

    @Nullable
    String getProperty(@NotNull String name);

    @NotNull
    String getProperty(@NotNull String name, Object projectObject);

    @NotNull
    String getPropertyWithDefault(@NotNull String name, @NotNull String defaultValue);

    void setProperty(@NotNull String name, @NotNull String value);

    void setProperty(@NotNull String name, @NotNull String value, Object projectObject);

    void unsetProperty(@NotNull String name);

    boolean isPropertySet(@NotNull String name);

    void unsetProperty(@NotNull String name, Object projectObject);

    @Nullable
    String[] getProperties(@NotNull String name);

    @Nullable
    String[] getProperties(@NotNull String name, Object projectObject);

    void setProperties(@NotNull String name, @NotNull String[] value);

    @NotNull
    List<ArtifactDescriptor> getArtifacts(@NotNull ProjectDescriptor projectDescriptor)
            throws AzureCmdException;

    @NotNull
    ListenableFuture<String> buildArtifact(@NotNull ProjectDescriptor projectDescriptor,
                                           @NotNull ArtifactDescriptor artifactDescriptor);

    @NotNull
    Object getCurrentProject();

    void setApplicationProperty(@NotNull String name, @NotNull String value);

    void unsetApplicationProperty(@NotNull String name);

    @Nullable
    String getApplicationProperty(@NotNull String name);

    void setApplicationProperties(@NotNull String name, @NotNull String[] value);

    void unsetApplicatonProperties(@NotNull String name);

    @Nullable
    String[] getApplicationProperties(@NotNull String name);

    boolean isApplicationPropertySet(@NotNull String name);

    void openLinkInBrowser(@NotNull String url);

    @Nullable
    default Icon getFileTypeIcon(String name, boolean isDirectory) {
        return null;
    }

    default void saveAppServiceFile(@NotNull final AppServiceFile file, @NotNull Object context, @Nullable File dest) {
        // do nothing in default
    }

    default void openAppServiceFile(final AppServiceFile file, Object context) {
        // do nothing in default
    }
}
