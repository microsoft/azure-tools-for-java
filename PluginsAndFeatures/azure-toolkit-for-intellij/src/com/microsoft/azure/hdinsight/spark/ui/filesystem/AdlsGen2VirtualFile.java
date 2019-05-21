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
package com.microsoft.azure.hdinsight.spark.ui.filesystem;

import com.intellij.compiler.ant.taskdefs.Path;
import com.intellij.mock.MockVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.vfs.impl.http.HttpFileSystemImpl;
import com.microsoft.azure.hdinsight.sdk.storage.adlsgen2.ADLSGen2FSOperation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdlsGen2VirtualFile extends AzureStorageVirtualFile {
    private String name;
    private String path;
    private VirtualFileSystem fileSystem;
    private boolean isDirectory;
    private List<VirtualFile> children;
    private VirtualFile parent;

    public AdlsGen2VirtualFile(String path, boolean isDirectory, VirtualFileSystem fileSystem) {
        this.name = path.substring(path.lastIndexOf("/") + 1);
        this.path = path;
        this.isDirectory = isDirectory;
        this.fileSystem = fileSystem;
    }

    @NotNull
    @Override
    public String getName() {
        return this.name;
    }

    @NotNull
    @Override
    public VirtualFileSystem getFileSystem() {
        return this.fileSystem;
    }

    @NotNull
    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public boolean isDirectory() {
        return this.isDirectory;
    }

    @Override
    @Nullable
    public VirtualFile getParent() {
        return this.parent;
    }

    @Override
    public void setParent(VirtualFile parent) {
        this.parent = parent;
    }

    @Override
    public VirtualFile[] getChildren() {
        if (this.children != null) {
            return this.children.toArray(new AdlsGen2VirtualFile[0]);
        }

        this.children = new ArrayList<>();
        if(!this.isDirectory) {
            return this.children.toArray(new AdlsGen2VirtualFile[0]);
        }

        URI uri = URI.create(this.path);
        ADLSGen2FileSystem gen2FileSystem = (ADLSGen2FileSystem) fileSystem;
        ADLSGen2FSOperation op = new ADLSGen2FSOperation(gen2FileSystem.http);

        op.list(gen2FileSystem.root.toString(), PathHelper.getRelativePath(gen2FileSystem.root, uri))
                .filter(path -> path.getName().startsWith("SparkSubmission"))
                .map(path -> new AdlsGen2VirtualFile(PathHelper.getFullPath(gen2FileSystem.root, path.getName()),
                        path.isDirectory(), this.fileSystem))
                .doOnNext(file -> {
                    if (file.isDirectory()) {
                        gen2FileSystem.fileCaches.put(file.getPath(), file);
                    }

                    String parentPath = PathHelper.getParentPath(URI.create(file.getPath()));
                    if (gen2FileSystem.fileCaches.keySet().contains(parentPath)) {
                        gen2FileSystem.fileCaches.get(parentPath).addChildren(file);
                        file.setParent(gen2FileSystem.fileCaches.get(parentPath));
                    }
                }).toBlocking().subscribe(ob -> {
                    this.children.add(ob);
                },
                err -> {
                    log().warn("Listing files encounters exception", err);
                });

        return this.children.toArray(new AdlsGen2VirtualFile[0]);
    }

    @Override
    public void addChildren(VirtualFile vf) {
        this.children.add(vf);
    }
}
