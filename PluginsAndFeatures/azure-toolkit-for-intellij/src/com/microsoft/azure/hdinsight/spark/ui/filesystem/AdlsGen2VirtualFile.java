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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.vfs.impl.http.HttpFileSystemImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AdlsGen2VirtualFile extends AzureStorageVirtualFile {
    private String name;
    private String path;
    private VirtualFileSystem fileSystem;
    private boolean isDirectory;
    private String prefix;
    private List<VirtualFile> children = new ArrayList<>();
    private VirtualFile parent;
    public static VirtualFile Empty = new AdlsGen2VirtualFile("", false, "", new HttpFileSystemImpl());

    public AdlsGen2VirtualFile(String path, boolean isDirectory, String prefix, VirtualFileSystem fileSystem) {
        this.name = path.substring(path.lastIndexOf("/") + 1);
        this.path = path;
        this.isDirectory = isDirectory;
        this.prefix = prefix;
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
        return prefix != null ? String.format("%s/%s", this.prefix, this.path) : this.path;
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
        return this.children.toArray(new AdlsGen2VirtualFile[0]);
    }

    @Override
    public void addChildren(VirtualFile vf) {
        this.children.add(vf);
    }
}
