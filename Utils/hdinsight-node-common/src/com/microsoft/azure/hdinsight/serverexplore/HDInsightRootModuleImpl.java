/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.serverexplore;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.sdk.cluster.*;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.ClusterNode;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;

import java.util.List;
import java.util.stream.Collectors;

public class HDInsightRootModuleImpl extends HDInsightRootModule {

    private static final String HDInsight_SERVICE_MODULE_ID = HDInsightRootModuleImpl.class.getName();
    private static final String ICON_PATH = CommonConst.HDExplorerIcon_16x_Path;
    private static final String BASE_MODULE_NAME = "HDInsight";

    public HDInsightRootModuleImpl(@NotNull Node parent) {
        super(HDInsight_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
    }

    private List<IClusterDetail> clusterDetailList;

    @Override
    public HDInsightRootModule getNewNode(@NotNull Node node) {
        return new HDInsightRootModuleImpl(node);
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        synchronized (this) { //todo???
//            TelemetryManager.postEvent(TelemetryCommon.HDInsightExplorerHDInsightNodeExpand, null, null);

            clusterDetailList = ClusterManagerEx.getInstance().getClusterDetails().stream()
                    .filter(ClusterManagerEx.getInstance().getHDInsightClusterFilterPredicate())
                    .collect(Collectors.toList());

            if (clusterDetailList != null) {
                for (IClusterDetail clusterDetail : clusterDetailList) {
                    addChildNode(new ClusterNode(this, clusterDetail));
                }
            }
        }
    }

    @Override
    public void refreshWithoutAsync() {
        synchronized (this) {
            removeAllChildNodes();

            // refresh the cluster list with invalidating the cache
            clusterDetailList = ClusterManagerEx.getInstance().getClusterDetails().stream()
                    .filter(ClusterManagerEx.getInstance().getHDInsightClusterFilterPredicate())
                    .collect(Collectors.toList());

            if (clusterDetailList != null) {
                for (IClusterDetail clusterDetail : clusterDetailList) {
                    addChildNode(new ClusterNode(this, clusterDetail));
                }
            }
        }

    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        if (!initialized) {
            this.load(false);
        }
    }
}