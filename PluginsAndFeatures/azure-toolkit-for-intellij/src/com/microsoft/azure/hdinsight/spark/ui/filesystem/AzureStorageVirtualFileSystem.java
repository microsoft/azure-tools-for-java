package com.microsoft.azure.hdinsight.spark.ui.filesystem;

import com.intellij.openapi.vfs.impl.http.HttpFileSystemBase;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import rx.Observable;

public abstract class AzureStorageVirtualFileSystem extends HttpFileSystemBase implements ILogger {
    public AzureStorageVirtualFileSystem(String protocol) {
        super(protocol);
    }

    @NotNull
    public abstract Observable<AzureStorageVirtualFile> getAzureStorageVirtualFiles(String rootPath);
}
