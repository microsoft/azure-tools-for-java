package com.microsoft.azure.hdinsight.common;

import com.microsoft.azure.hdinsight.common.explorer.AbstractHDIAzureStorageExplorerHandler;
import com.microsoft.azure.hdinsight.common.explorer.LinuxOpenHDIAzureStorageExplorerHandler;
import com.microsoft.azure.hdinsight.common.explorer.MacOSOpenHDIAzureStorageExplorerHandler;
import com.microsoft.azure.hdinsight.common.explorer.WindowsOpenHDIAzureStorageExplorerHandler;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.SystemUtils;

public class OpenHDIAzureStorageExplorerAction {
    private final AbstractHDIAzureStorageExplorerHandler handler;

    public OpenHDIAzureStorageExplorerAction() {
        if (SystemUtils.IS_OS_WINDOWS) {
            this.handler = new WindowsOpenHDIAzureStorageExplorerHandler();
        } else if (SystemUtils.IS_OS_MAC) {
            this.handler = new MacOSOpenHDIAzureStorageExplorerHandler();
        } else {
            this.handler = new LinuxOpenHDIAzureStorageExplorerHandler();
        }
    }

    @AzureOperation(name = "storage.open_azure_storage_explorer.account", params = {"clusterDetail.getName()"}, type = AzureOperation.Type.ACTION)
    public void openResource(final IClusterDetail clusterDetail) {
        this.handler.openResource(clusterDetail);
    }
}
