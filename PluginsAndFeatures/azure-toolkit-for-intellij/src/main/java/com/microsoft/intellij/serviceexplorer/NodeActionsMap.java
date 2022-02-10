/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.serviceexplorer;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.hdinsight.serverexplore.HDInsightRootModuleImpl;
import com.microsoft.azure.hdinsight.serverexplore.action.AddNewClusterAction;
import com.microsoft.azure.sqlbigdata.serverexplore.SqlBigDataClusterModule;
import com.microsoft.azure.toolkit.intellij.docker.action.PushToContainerRegistryAction;
import com.microsoft.azure.toolkit.intellij.vm.CreateVMAction;
import com.microsoft.intellij.serviceexplorer.azure.storage.ConfirmDialogAction;
import com.microsoft.intellij.serviceexplorer.azure.storage.CreateQueueAction;
import com.microsoft.intellij.serviceexplorer.azure.storage.CreateTableAction;
import com.microsoft.intellij.serviceexplorer.azure.storage.ModifyExternalStorageAccountAction;
import com.microsoft.sqlbigdata.serverexplore.action.LinkSqlServerBigDataClusterAction;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.ExternalStorageNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.QueueModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.TableModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule;

import java.util.HashMap;
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
        NODE_ACTIONS.put(ContainerRegistryNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(PushToContainerRegistryAction.class).build());
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
    }
}
