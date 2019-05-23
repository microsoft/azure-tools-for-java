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
import com.intellij.openapi.vfs.VirtualFileListener;
import com.microsoft.azure.hdinsight.sdk.common.HttpObservable;
import com.microsoft.azure.hdinsight.sdk.storage.adlsgen2.ADLSGen2FSOperation;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ADLSGen2FileSystem extends AzureStorageVirtualFileSystem {
    public static final String myProtocol = "abfs";

    @Nullable
    private HttpObservable http;

    // abfs://file_system@account_name.dfs.core.windows.net/
    private URI root;
    private ADLSGen2FSOperation op;

    // https://account_name.dfs.core.windows.net/file_system
    private String restApiRoot;

    public ADLSGen2FileSystem(@NotNull HttpObservable http, @NotNull String restApiRoot) {
        this.http = http;
        this.op = new ADLSGen2FSOperation(this.http);
        this.restApiRoot = restApiRoot;
        this.root = URI.create(op.uriHelper.convertToGen2Uri(URI.create(restApiRoot)));
    }

    @NotNull
    @Override
    public String getProtocol() {
        return myProtocol;
    }

    public URI getRoot() {return root;}

    @NotNull
    public VirtualFile[] listFiles(AdlsGen2VirtualFile vf) {
        List<AdlsGen2VirtualFile> childrenList = new ArrayList<>();
        if (vf.isDirectory()) {
            childrenList = this.op.list(this.restApiRoot, this.op.uriHelper.getDirectoryParam(vf.getUri()))
                    .map(path -> new AdlsGen2VirtualFile(this.root.resolve(path.getName()),
                            path.isDirectory(), this))
                    .doOnNext(file -> file.setParent(vf))
                    .toList().toBlocking().lastOrDefault(new ArrayList<>());
        }

        return childrenList.toArray(new VirtualFile[0]);
    }

    @Nullable
    @Override
    public VirtualFile findFileByPath(@NotNull String path) {
        return null;
    }

    @Override
    public void refresh(boolean asynchronous) {

    }

    @Nullable
    @Override
    public VirtualFile refreshAndFindFileByPath(@NotNull String path) {
        return null;
    }

    @Override
    public void addVirtualFileListener(@NotNull VirtualFileListener listener) {

    }

    @Override
    public void removeVirtualFileListener(@NotNull VirtualFileListener listener) {

    }

    @Override
    protected void deleteFile(Object requestor, @NotNull VirtualFile vFile) throws IOException {

    }

    @Override
    protected void moveFile(Object requestor, @NotNull VirtualFile vFile, @NotNull VirtualFile newParent) throws
            IOException {

    }

    @Override
    protected void renameFile(Object requestor, @NotNull VirtualFile vFile, @NotNull String newName) throws
            IOException {

    }

    @NotNull
    @Override
    protected VirtualFile createChildFile(Object requestor, @NotNull VirtualFile vDir, @NotNull String fileName) throws
            IOException {
        throw new UnsupportedOperationException("unimplemented method for Adls Gen2 FileSystem");
    }

    @NotNull
    @Override
    protected VirtualFile createChildDirectory(Object requestor, @NotNull VirtualFile vDir, @NotNull String dirName) throws
            IOException {
        throw new UnsupportedOperationException("unimplemented method for Adls Gen2 FileSystem");
    }

    @NotNull
    @Override
    protected VirtualFile copyFile(Object requestor, @org.jetbrains.annotations.NotNull VirtualFile
            virtualFile, @NotNull VirtualFile newParent, @org.jetbrains.annotations.NotNull String copyName) throws
            IOException {
        throw new UnsupportedOperationException("unimplemented method for Adls Gen2 FileSystem");
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }
}
