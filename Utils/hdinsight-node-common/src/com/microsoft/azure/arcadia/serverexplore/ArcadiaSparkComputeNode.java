/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.arcadia.serverexplore;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkCompute;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;

public class ArcadiaSparkComputeNode extends RefreshableNode {
    private static final String ARCADIA_COMPUTE_ID = ArcadiaSparkComputeNode.class.getName();
    private static final String ICON_PATH = CommonConst.ClusterIConPath;

    @NotNull
    private ArcadiaSparkCompute compute;

    public ArcadiaSparkComputeNode(Node parent, @NotNull ArcadiaSparkCompute compute) {
        super(ARCADIA_COMPUTE_ID, compute.getTitleForNode(), parent, ICON_PATH, true);
        this.compute = compute;
        this.loadActions();
    }

    @Override
    protected synchronized void refreshItems() throws AzureCmdException {
    }

    @Override
    public String getServiceName() {
        return TelemetryConstants.SPARK_ON_ARCADIA;
    }
}
