package com.microsoft.azure.hdinsight.spark.ui.filesystem;

import com.intellij.openapi.vfs.VirtualFileSystem;
import com.microsoft.azure.hdinsight.common.logger.ILogger;

public abstract class AzureStorageVirtualFileSystem extends VirtualFileSystem implements ILogger {
    public enum VFSSupportStorageType {
        ADLSGen2
    }
}
