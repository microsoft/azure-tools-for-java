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
import com.microsoft.azure.hdinsight.common.AbfsUri;
import com.microsoft.azure.hdinsight.common.AzureStorageUri;
import com.microsoft.azure.hdinsight.sdk.common.HttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.errorresponse.ForbiddenHttpErrorStatus;
import com.microsoft.azure.hdinsight.sdk.storage.adlsgen2.ADLSGen2FSOperation;
import com.microsoft.azure.hdinsight.spark.common.ADLSGen2Deploy;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import rx.Observable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ADLSGen2FileSystem extends AzureStorageVirtualFileSystem {
    public static final String myProtocol = "abfs";

    @Nullable
    private HttpObservable http;

    private AbfsUri rootPathUri;
    private ADLSGen2FSOperation op;

    public ADLSGen2FileSystem(@NotNull HttpObservable http, @NotNull AbfsUri rootPathUri) {
        this.http = http;
        this.op = new ADLSGen2FSOperation(this.http);
        this.rootPathUri = rootPathUri;
    }

    @NotNull
    @Override
    public String getProtocol() {
        return myProtocol;
    }

    @NotNull
    public VirtualFile[] listFiles(AdlsGen2VirtualFile vf) {
        List<AdlsGen2VirtualFile> childrenList = new ArrayList<>();
        if (vf.isDirectory()) {
            // sample fileSystemRootPath: https://accountName.dfs.core.windows.net/fileSystem/
            String fileSystemRootPath = rootPathUri.resolve("/").getUrl().toString();
            // sample directoryParam: sub/path/to
            String directoryParam = vf.getAbfsUri().getDirectoryParam();
            childrenList = this.op.list(fileSystemRootPath, directoryParam)
                    // sample remoteFile.getName(): sub/path/to/SparkSubmission
                    .map(remoteFile -> new AdlsGen2VirtualFile(
                            (AbfsUri) AbfsUri.parse(fileSystemRootPath)
                                    .resolveAsRoot(AzureStorageUri.encodeAndNormalizePath(remoteFile.getName())),
                            remoteFile.isDirectory(),
                            this))
                    .doOnNext(file -> file.setParent(vf))
                    .onErrorResumeNext(err -> {
                                String errorMessage = "Failed to list folders and files with error " + err.getMessage() + ". ";
                                if (err instanceof ForbiddenHttpErrorStatus) {
                                    errorMessage += ADLSGen2Deploy.getForbiddenErrorHints(vf.toString());
                                }
                                return Observable.error(new IOException(errorMessage));
                            }
                    )
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
