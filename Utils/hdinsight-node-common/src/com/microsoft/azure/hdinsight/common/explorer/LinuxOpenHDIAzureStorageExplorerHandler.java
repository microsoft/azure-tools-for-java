package com.microsoft.azure.hdinsight.common.explorer;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class LinuxOpenHDIAzureStorageExplorerHandler extends AbstractHDIAzureStorageExplorerHandler{
    @Override
    protected boolean launchStorageExplorerWithUri(@NotNull IClusterDetail clusterDetail, @NotNull String resourceUrl) {
        // Launch storage explorer from uri is not supported for Linux
        // Refers https://docs.microsoft.com/en-us/azure/storage/common/storage-explorer-direct-link
        return false;
    }

    @Override
    protected String getStorageExplorerExecutableFromOS() {
        return null;
    }

    @Override
    protected void launchStorageExplorer(String explorer, String storageUrl) {
        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(explorer, storageUrl);
        try {
            processBuilder.start();
        } catch (IOException e) {
            throw new AzureToolkitRuntimeException(e.getMessage(), e);
        }
    }
}
