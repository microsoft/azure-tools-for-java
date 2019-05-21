package com.microsoft.azure.hdinsight.spark.ui.filesystem;

import com.intellij.openapi.vfs.impl.http.HttpFileSystemBase;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import java.util.HashMap;

public abstract class AzureStorageVirtualFileSystem extends HttpFileSystemBase implements ILogger {
    public HashMap<String, AzureStorageVirtualFile> fileCaches = new HashMap<>();

    public AzureStorageVirtualFileSystem(String protocol) {
        super(protocol);
    }

    abstract public void setFSRoot(AzureStorageVirtualFile vf);
}
