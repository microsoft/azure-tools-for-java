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

package com.microsoft.azure.sqlbigdata.serverexplore;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;

import java.util.List;
import java.util.stream.Collectors;

public class SqlBigDataClusterModule extends RefreshableNode implements ILogger {
    private static final String ARIS_SERVICE_MODULE_ID = SqlBigDataClusterModule.class.getName();
    private static final String BASE_MODULE_NAME = "SQL Big Data Cluster";
    private static final String ICON_PATH = CommonConst.SQL_BIG_DATA_CLUSTER_MODULE_ICON_PATH;
    @Nullable
    private Object project;

    public SqlBigDataClusterModule(@Nullable Object project) {
        super(ARIS_SERVICE_MODULE_ID, BASE_MODULE_NAME, null, ICON_PATH);
        this.project = project;
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
}
