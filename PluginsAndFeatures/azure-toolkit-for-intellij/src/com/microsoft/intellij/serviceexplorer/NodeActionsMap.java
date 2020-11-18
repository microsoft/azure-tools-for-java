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
import com.microsoft.intellij.serviceexplorer.azure.arm.*;
import com.microsoft.intellij.serviceexplorer.azure.container.PushToContainerRegistryAction;
import com.microsoft.intellij.serviceexplorer.azure.rediscache.CreateRedisCacheAction;
import com.microsoft.intellij.serviceexplorer.azure.springcloud.SpringCloudStreamingLogsAction;
import com.microsoft.intellij.serviceexplorer.azure.storage.ConfirmDialogAction;
import com.microsoft.intellij.serviceexplorer.azure.storage.CreateQueueAction;
import com.microsoft.intellij.serviceexplorer.azure.storage.CreateTableAction;
import com.microsoft.intellij.serviceexplorer.azure.storage.ModifyExternalStorageAccountAction;
import com.microsoft.intellij.serviceexplorer.azure.storagearm.CreateStorageAccountAction;
import com.microsoft.intellij.serviceexplorer.azure.vmarm.CreateVMAction;
import com.microsoft.sqlbigdata.serverexplore.action.LinkSqlServerBigDataClusterAction;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.ResourceManagementModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.ResourceManagementNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud.SpringCloudAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.ExternalStorageNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.QueueModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.StorageModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.TableModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotNode;

import java.util.*;

public class NodeActionsMap {
    public static final Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> node2Actions =
            new HashMap<>();

    static {
        node2Actions.put(VMArmModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(CreateVMAction.class).build());
        node2Actions.put(QueueModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(CreateQueueAction.class).build());
        node2Actions.put(TableModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(CreateTableAction.class).build());
        node2Actions.put(StorageModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(CreateStorageAccountAction.class).build());
        node2Actions.put(RedisCacheModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(CreateRedisCacheAction.class).build());
        node2Actions.put(WebAppModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(CreateWebAppAction.class).build());
        node2Actions.put(FunctionModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(CreateFunctionAppAction.class).build());
        node2Actions.put(ContainerRegistryNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(PushToContainerRegistryAction.class).build());
        // todo: what is ConfirmDialogAction?
        //noinspection unchecked
        node2Actions.put(ExternalStorageNode.class,
                new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                        .add(ConfirmDialogAction.class, ModifyExternalStorageAccountAction.class).build());
        //noinspection unchecked
        node2Actions.put(HDInsightRootModuleImpl.class,
                new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                        .add(AddNewClusterAction.class).build());
        node2Actions.put(SqlBigDataClusterModule.class,
                new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                        .add(LinkSqlServerBigDataClusterAction.class).build());

        List<Class<? extends NodeActionListener>> deploymentNodeList = new ArrayList<>();
        deploymentNodeList.addAll(Arrays.asList(ExportTemplateAction.class, ExportParameterAction.class,
                UpdateDeploymentAction.class, EditDeploymentAction.class));

        node2Actions.put(DeploymentNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .addAll(deploymentNodeList).build());

        node2Actions.put(ResourceManagementModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .add(CreateDeploymentAction.class).build());

        node2Actions.put(ResourceManagementNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .add(CreateDeploymentAction.class).build());

        node2Actions.put(SpringCloudAppNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(SpringCloudStreamingLogsAction.class).build());

        node2Actions.put(FunctionNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(StartStreamingLogsAction.class).add(StopStreamingLogsAction.class).add(DeployFunctionAppAction.class).build());

        node2Actions.put(WebAppNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(StartStreamingLogsAction.class).add(StopStreamingLogsAction.class).add(SSHIntoWebAppAction.class)
                .add(DeployWebAppAction.class)
                .add(ProfileFlightRecordAction.class).build());

        node2Actions.put(DeploymentSlotNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(StartStreamingLogsAction.class).add(StopStreamingLogsAction.class).build());
    }
}
