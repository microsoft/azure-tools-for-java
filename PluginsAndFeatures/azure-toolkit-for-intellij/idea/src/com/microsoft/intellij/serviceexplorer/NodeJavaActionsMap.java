/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.serviceexplorer;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.hdinsight.serverexplore.HDInsightRootModuleImpl;
import com.microsoft.azure.hdinsight.serverexplore.action.AddNewClusterAction;
import com.microsoft.azure.sqlbigdata.serverexplore.SqlBigDataClusterModule;
import com.microsoft.azure.toolkit.intellij.function.action.CreateFunctionAppAction;
import com.microsoft.azure.toolkit.intellij.function.action.DeployFunctionAppAction;
import com.microsoft.azure.toolkit.intellij.webapp.action.CreateWebAppAction;
import com.microsoft.azure.toolkit.intellij.webapp.action.DeployWebAppAction;
import com.microsoft.intellij.serviceexplorer.azure.appservice.ProfileFlightRecordAction;
import com.microsoft.intellij.serviceexplorer.azure.appservice.SSHIntoWebAppAction;
import com.microsoft.intellij.serviceexplorer.azure.appservice.StartStreamingLogsAction;
import com.microsoft.intellij.serviceexplorer.azure.appservice.StopStreamingLogsAction;
import com.microsoft.intellij.serviceexplorer.azure.container.PushToContainerRegistryAction;
import com.microsoft.intellij.serviceexplorer.azure.springcloud.SpringCloudStreamingLogsAction;
import com.microsoft.intellij.sqlbigdata.serverexplore.action.LinkSqlServerBigDataClusterAction;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud.SpringCloudAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotNode;

import java.util.HashMap;
import java.util.Map;

public class NodeJavaActionsMap extends NodeActionsMap {

    private static final Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> node2Actions =
            new HashMap<>();

    static {
        node2Actions.put(ContainerRegistryNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(PushToContainerRegistryAction.class).build());

        node2Actions.put(HDInsightRootModuleImpl.class,
                         new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                                 .add(AddNewClusterAction.class).build());

        node2Actions.put(SqlBigDataClusterModule.class,
                         new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                                 .add(LinkSqlServerBigDataClusterAction.class).build());

        node2Actions.put(SpringCloudAppNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(SpringCloudStreamingLogsAction.class).build());

        node2Actions.put(FunctionModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(CreateFunctionAppAction.class).build());

        node2Actions.put(FunctionNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(StartStreamingLogsAction.class)
                .add(StopStreamingLogsAction.class)
                .add(DeployFunctionAppAction.class).build());

        node2Actions.put(WebAppModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(CreateWebAppAction.class).build());

        node2Actions.put(WebAppNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(StartStreamingLogsAction.class)
                .add(StopStreamingLogsAction.class)
                .add(DeployWebAppAction.class)
                .add(SSHIntoWebAppAction.class)
                .add(ProfileFlightRecordAction.class)
                .build());

        node2Actions.put(DeploymentSlotNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(StartStreamingLogsAction.class)
                .add(StopStreamingLogsAction.class)
                .build());
    }

    @Override
    public Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> getMap() {
        return node2Actions;
    }
}
