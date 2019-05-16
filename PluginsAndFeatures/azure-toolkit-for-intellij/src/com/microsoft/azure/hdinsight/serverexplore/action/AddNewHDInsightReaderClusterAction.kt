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

package com.microsoft.azure.hdinsight.serverexplore.action

import com.intellij.openapi.project.Project
import com.microsoft.azure.hdinsight.common.ClusterManagerEx
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightAdditionalClusterDetail
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule
import com.microsoft.azure.hdinsight.serverexplore.ui.AddNewHDInsightReaderClusterForm
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener

class AddNewHDInsightReaderClusterAction(val module: HDInsightRootModule, val selectedClusterDetail: ClusterDetail): NodeActionListener() {
    override fun actionPerformed(event: NodeActionEvent?) {
        val defaultStorageRootPath = selectedClusterDetail.defaultStorageRootPath
        val form = object: AddNewHDInsightReaderClusterForm(module.project as Project, module, selectedClusterDetail.name) {
            override fun afterOkActionPerformed() {
                val linkedCluster =
                    ClusterManagerEx.getInstance().findClusterDetail({ clusterDetail ->
                        clusterDetail is HDInsightAdditionalClusterDetail
                                && clusterDetail.getName() == selectedClusterDetail.name
                    }, true) as? HDInsightAdditionalClusterDetail

                linkedCluster?.let {
                    it.defaultStorageRootPath = defaultStorageRootPath
                    ClusterManagerEx.getInstance().updateHdiAdditionalClusterDetail(it)
                }

                super.afterOkActionPerformed()
            }
        }
        form.show()
    }
}