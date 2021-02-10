/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.arcadia.serverexplore;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkCompute;
import com.microsoft.azure.projectarcadia.common.ArcadiaWorkSpace;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.*;

public class ArcadiaSparkWorkspaceNode extends RefreshableNode {
    private static final String ARCADIA_WORKSPACE_ID = ArcadiaSparkWorkspaceNode.class.getName();
    private static final String ICON_PATH = CommonConst.ARCADIA_WORKSPACE_NODE_ICON_PATH;

    @NotNull
    private ArcadiaWorkSpace workspace;

    public ArcadiaSparkWorkspaceNode(Node parent, @NotNull ArcadiaWorkSpace workspace) {
        super(ARCADIA_WORKSPACE_ID, workspace.getTitleForNode(), parent, ICON_PATH, true);
        this.workspace = workspace;
        this.loadActions();
    }

    @Override
    protected synchronized void refreshItems() throws AzureCmdException {
        this.workspace.refresh();
        this.workspace.getClusters().forEach(compute -> {
            addChildNode(new ArcadiaSparkComputeNode(this, (ArcadiaSparkCompute) compute));
        });
    }

    @Override
    protected void loadActions() {
        super.loadActions();
        if (workspace.getWebUrl() != null) {
            addAction("Launch workspace", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    DefaultLoader.getIdeHelper().openLinkInBrowser(workspace.getWebUrl());
                }
            });
        }
    }

    @Override
    public String getServiceName() {
        return TelemetryConstants.SPARK_ON_ARCADIA;
    }
}
