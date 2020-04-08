/**
 * Copyright (c) 2018 JetBrains s.r.o.
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

package com.microsoft.intellij.serviceexplorer;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.hdinsight.serverexplore.HDInsightRootModuleImpl;
import com.microsoft.azure.hdinsight.serverexplore.action.AddNewClusterAction;
import com.microsoft.azure.hdinsight.serverexplore.action.AddNewEmulatorAction;
import com.microsoft.azure.sqlbigdata.serverexplore.SqlBigDataClusterModule;
import com.microsoft.intellij.serviceexplorer.azure.arm.*;
import com.microsoft.intellij.serviceexplorer.azure.container.PushToContainerRegistryAction;
import com.microsoft.intellij.serviceexplorer.azure.docker.*;
import com.microsoft.intellij.serviceexplorer.azure.rediscache.CreateRedisCacheAction;
import com.microsoft.intellij.serviceexplorer.azure.storage.*;
import com.microsoft.intellij.serviceexplorer.azure.storagearm.CreateStorageAccountAction;
import com.microsoft.intellij.serviceexplorer.azure.vmarm.CreateVMAction;
import com.microsoft.intellij.sqlbigdata.serverexplore.action.LinkSqlServerBigDataClusterAction;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.ResourceManagementModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.ResourceManagementNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.docker.DockerHostModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.docker.DockerHostNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.*;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule;

import java.util.*;

public class NodeJavaActionsMap extends NodeActionsMap {

    private static final Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> node2Actions =
            new HashMap<>();

    static {
        node2Actions.put(ContainerRegistryNode.class, new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                .add(PushToContainerRegistryAction.class).build());

        //noinspection unchecked
        node2Actions.put(HDInsightRootModuleImpl.class,
                new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                        .add(AddNewClusterAction.class, AddNewEmulatorAction.class).build());

        //noinspection unchecked
        node2Actions.put(DockerHostNode.class,
                new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                        .add(ViewDockerHostAction.class, DeployDockerContainerAction.class,
                                DeleteDockerHostAction.class).build());
        //noinspection unchecked
        node2Actions.put(DockerHostModule.class,
                new ImmutableList.Builder<Class<? extends NodeActionListener>>()
                        .add(CreateNewDockerHostAction.class, PublishDockerContainerAction.class).build());
    }

    @Override
    public Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> getMap() {
        return node2Actions;
    }
}
