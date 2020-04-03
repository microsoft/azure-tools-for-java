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

package com.microsoft.intellij.docker;

import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.microsoft.azure.docker.ops.utils.AzureDockerValidationUtils;
import org.jetbrains.annotations.NotNull;

public abstract class WebAppBasePropertyViewProvider implements FileEditorProvider, DumbAware {
    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        return virtualFile.getFileType().getName().equals(getType());
    }

    @NotNull
    @Override
    public String getEditorTypeId() {
        return getType();
    }

    @NotNull
    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
    }

    protected abstract String getType();
}

// TODO: SD -- check this class usage (start)
//public class DockerArtifactsFromJavaProjectModel extends DockerArtifactProvider {
//    @Override
//    public Collection<File> getPaths(@NotNull Project project) {
//        final ArrayList<File> result = new ArrayList<>();
//
//        for (Artifact item: ArtifactUtil.getArtifactWithOutputPaths(project)) {
//            String path = item.getOutputFilePath();
//            if (path != null && (path.toLowerCase().endsWith(".war") || path.toLowerCase().endsWith(".jar")) &&
//                    AzureDockerValidationUtils.validateDockerArtifactPath(path)) {
//                result.add(new File(path));
//            }
//        }
//
//        return result;
//    }
//}
// TODO: SD -- check this class usage (end)