/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.sqlbigdata.serverexplore;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;

import java.util.List;
import java.util.stream.Collectors;

public class SqlBigDataClusterModule extends RefreshableNode implements ILogger {
    private static final String ARIS_SERVICE_MODULE_ID = SqlBigDataClusterModule.class.getName();
    private static final String BASE_MODULE_NAME = "SQL Server Big Data Cluster";
    private static final String ICON_PATH = CommonConst.SQL_BIG_DATA_CLUSTER_MODULE_ICON_PATH;
    @Nullable
    private Object project;

    public SqlBigDataClusterModule(@Nullable Object project) {
        super(ARIS_SERVICE_MODULE_ID, BASE_MODULE_NAME, null, ICON_PATH);
        this.project = project;
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.SQLServerBigDataCluster.MODULE;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        synchronized (this) {
            List<IClusterDetail> clusterDetailList = ClusterManagerEx.getInstance().getClusterDetails().stream()
                    .filter(clusterDetail -> clusterDetail instanceof SqlBigDataLivyLinkClusterDetail)
                    .collect(Collectors.toList());

            if (clusterDetailList != null) {
                for (IClusterDetail clusterDetail : clusterDetailList) {
                    addChildNode(new SqlBigDataClusterNode(this, (SqlBigDataLivyLinkClusterDetail) clusterDetail));
                }
            }
        }
    }

    @Nullable
    @Override
    public Object getProject() {
        return project;
    }

    @Override
    public String getServiceName() {
        return TelemetryConstants.SPARK_ON_SQL_SERVER;
    }
}
