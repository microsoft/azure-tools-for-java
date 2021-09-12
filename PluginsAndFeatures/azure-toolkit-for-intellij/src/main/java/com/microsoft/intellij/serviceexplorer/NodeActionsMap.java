/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.serviceexplorer;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.hdinsight.serverexplore.HDInsightRootModuleImpl;
import com.microsoft.azure.hdinsight.serverexplore.action.AddNewClusterAction;
import com.microsoft.azure.sqlbigdata.serverexplore.SqlBigDataClusterModule;
import com.microsoft.azure.toolkit.intellij.connector.database.ConnectToSQLAction;
import com.microsoft.azure.toolkit.intellij.function.action.CreateFunctionAppAction;
import com.microsoft.azure.toolkit.intellij.function.action.DeployFunctionAppAction;
import com.microsoft.azure.toolkit.intellij.mysql.action.CreateMySQLAction;
import com.microsoft.azure.toolkit.intellij.mysql.action.OpenMySQLByToolsAction;
import com.microsoft.azure.toolkit.intellij.connector.database.ConnectToMySQLAction;
import com.microsoft.azure.toolkit.intellij.sqlserver.CreateSqlServerAction;
import com.microsoft.azure.toolkit.intellij.sqlserver.OpenSqlServerByToolsAction;
import com.microsoft.azure.toolkit.intellij.webapp.action.CreateWebAppAction;
import com.microsoft.azure.toolkit.intellij.webapp.action.DeployWebAppAction;
import com.microsoft.azure.toolkit.intellij.appservice.action.ProfileFlightRecordAction;
import com.microsoft.azure.toolkit.intellij.appservice.action.SSHIntoWebAppAction;
import com.microsoft.azure.toolkit.intellij.appservice.action.StartStreamingLogsAction;
import com.microsoft.azure.toolkit.intellij.appservice.action.StopStreamingLogsAction;
import com.microsoft.azure.toolkit.intellij.arm.action.CreateDeploymentAction;
import com.microsoft.azure.toolkit.intellij.arm.action.EditDeploymentAction;
import com.microsoft.azure.toolkit.intellij.arm.action.ExportParameterAction;
import com.microsoft.azure.toolkit.intellij.arm.action.ExportTemplateAction;
import com.microsoft.azure.toolkit.intellij.arm.action.UpdateDeploymentAction;
import com.microsoft.azure.toolkit.intellij.webapp.docker.action.PushToContainerRegistryAction;
import com.microsoft.azure.toolkit.intellij.redis.action.CreateRedisCacheAction;
import com.microsoft.intellij.serviceexplorer.azure.storage.ConfirmDialogAction;
import com.microsoft.intellij.serviceexplorer.azure.storage.CreateQueueAction;
import com.microsoft.intellij.serviceexplorer.azure.storage.CreateTableAction;
import com.microsoft.intellij.serviceexplorer.azure.storage.ModifyExternalStorageAccountAction;
import com.microsoft.azure.toolkit.intellij.vm.CreateVMAction;
import com.microsoft.sqlbigdata.serverexplore.action.LinkSqlServerBigDataClusterAction;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.ResourceManagementModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.ResourceManagementNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.sqlserver.SqlServerModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.sqlserver.SqlServerNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.ExternalStorageNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.QueueModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.TableModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeActionsMap {
    public static final Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> NODE_ACTIONS = new HashMap<>();

    static {
        NODE_ACTIONS.put(VMArmModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(CreateVMAction.class).build());
        NODE_ACTIONS.put(QueueModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(CreateQueueAction.class).build());
        NODE_ACTIONS.put(TableModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(CreateTableAction.class).build());
        NODE_ACTIONS.put(RedisCacheModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(CreateRedisCacheAction.class).build());
        NODE_ACTIONS.put(WebAppModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(CreateWebAppAction.class).build());
        NODE_ACTIONS.put(FunctionModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(CreateFunctionAppAction.class).build());
        NODE_ACTIONS.put(ContainerRegistryNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(PushToContainerRegistryAction.class).build());
        NODE_ACTIONS.put(MySQLModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(CreateMySQLAction.class).build());
        NODE_ACTIONS.put(SqlServerModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(CreateSqlServerAction.class).build());
        // todo: what is ConfirmDialogAction?
        //noinspection unchecked
        NODE_ACTIONS.put(ExternalStorageNode.class,
                new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                        .add(ConfirmDialogAction.class, ModifyExternalStorageAccountAction.class).build());
        NODE_ACTIONS.put(HDInsightRootModuleImpl.class,
                new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                        .add(AddNewClusterAction.class).build());
        NODE_ACTIONS.put(SqlBigDataClusterModule.class,
                new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                        .add(LinkSqlServerBigDataClusterAction.class).build());

        final List<Class<? extends NodeActionListener>> deploymentNodeList = new ArrayList<>(
                Arrays.asList(ExportTemplateAction.class, ExportParameterAction.class, UpdateDeploymentAction.class, EditDeploymentAction.class));

        NODE_ACTIONS.put(DeploymentNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .addAll(deploymentNodeList).build());

        NODE_ACTIONS.put(ResourceManagementModule.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .add(CreateDeploymentAction.class).build());

        NODE_ACTIONS.put(ResourceManagementNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
            .add(CreateDeploymentAction.class).build());

        NODE_ACTIONS.put(FunctionAppNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(StartStreamingLogsAction.class).add(StopStreamingLogsAction.class).add(DeployFunctionAppAction.class).build());

        NODE_ACTIONS.put(MySQLNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(OpenMySQLByToolsAction.class).add(ConnectToMySQLAction.class).build());

        NODE_ACTIONS.put(SqlServerNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(OpenSqlServerByToolsAction.class).add(ConnectToSQLAction.class).build());

        NODE_ACTIONS.put(WebAppNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(StartStreamingLogsAction.class).add(StopStreamingLogsAction.class).add(SSHIntoWebAppAction.class)
                .add(DeployWebAppAction.class)
                .add(ProfileFlightRecordAction.class).build());

        NODE_ACTIONS.put(DeploymentSlotNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(StartStreamingLogsAction.class).add(StopStreamingLogsAction.class).build());
    }
}
