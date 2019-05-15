package com.microsoft.azure.hdinsight.spark.ui.filesystem;

import com.intellij.openapi.vfs.impl.http.HttpFileSystemBase;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.SharedKeyHttpObservable;
import com.microsoft.azure.hdinsight.sdk.storage.ADLSGen2StorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccountType;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class ADLSGen2FileSystem extends HttpFileSystemBase implements ILogger {
    public static final String myProtocol = "https";
    public HttpObservable http;
    public String rootPath;

    public ADLSGen2FileSystem(@NotNull String rootPath, @Nullable String clusterName) {
        super(myProtocol);
        this.rootPath = rootPath;

        Optional<IClusterDetail> clusterDetail = ClusterManagerEx.getInstance().getClusterDetailByName(clusterName);
        if (clusterDetail.isPresent()) {
            try {
                clusterDetail.get().getConfigurationInfo();
                IHDIStorageAccount storageAccount = clusterDetail.get().getStorageAccount();

                if (storageAccount.getAccountType() == StorageAccountType.ADLSGen2) {
                    String accessKey = ((ADLSGen2StorageAccount) storageAccount).getPrimaryKey();
                    if (!StringUtils.isBlank(accessKey)) {
                        this.http = new SharedKeyHttpObservable(storageAccount.getName(), accessKey);
                    }
                }
            } catch (Exception e) {
                log().warn("Initialize adls gen2 virtual file system encounters exception", e);
            }
        }
    }
}
