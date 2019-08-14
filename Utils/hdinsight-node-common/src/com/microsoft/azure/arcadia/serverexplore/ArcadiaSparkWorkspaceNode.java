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
    private static final String ICON_PATH = CommonConst.ClusterIConPath;

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

        addAction("Launch workspace", new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                // ie: https://web.projectarcadia.net/?workspace=/subscriptions/sub_id/resourceGroups/rg_name/providers/Microsoft.ProjectArcadia/workspaces/ws_name
                DefaultLoader.getIdeHelper().openLinkInBrowser(String.format("https://web.projectarcadia.net/?workspace=%s",
                        workspace.getUri().getPath()));
            }
        });
    }

    @Override
    public String getServiceName() {
        return TelemetryConstants.SPARK_ON_ARCADIA;
    }
}
