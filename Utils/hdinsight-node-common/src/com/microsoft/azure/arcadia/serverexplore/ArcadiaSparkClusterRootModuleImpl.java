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
package com.microsoft.azure.arcadia.serverexplore;

import com.microsoft.azure.arcadia.serverexplore.arcadianode.ArcadiaSparkWorkspaceNode;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.serverexplore.hdinsightnode.HDInsightRootModule;
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkComputeManager;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

public class ArcadiaSparkClusterRootModuleImpl extends HDInsightRootModule {
    private static final String SERVICE_MODULE_ID = ArcadiaSparkClusterRootModuleImpl.class.getName();
    private static final String ICON_PATH = CommonConst.ARCADIA_WORKSPACE_MODULE_ICON_PATH;
    private static final String BASE_MODULE_NAME = "Apache Spark on Arcadia";

    public ArcadiaSparkClusterRootModuleImpl(@NotNull Node parent) {
        super(SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH, true);
        this.loadActions();
    }

    @Override
    protected synchronized void refreshItems() throws AzureCmdException {
        if (!isFeatureEnabled()) {
            return;
        }

        ArcadiaSparkComputeManager.getInstance().refresh();
        ArcadiaSparkComputeManager.getInstance().getWorkspaces().forEach(workSpace -> {
            addChildNode(new ArcadiaSparkWorkspaceNode(this, workSpace));
        });
    }

    @Override
    public boolean isFeatureEnabled() {
        return CommonSettings.isProjectArcadiaFeatureEnabled;
    }

    @Override
    public HDInsightRootModule getNewNode(Node parent) {
        return new ArcadiaSparkClusterRootModuleImpl(parent);
    }

    @Override
    public String getServiceName() {
        return TelemetryConstants.SPARK_ON_ARCADIA;
    }
}
