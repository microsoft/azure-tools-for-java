/**
 * Copyright (c) Microsoft Corporation
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
import com.microsoft.intellij.serviceexplorer.azure.docker.*;
import com.microsoft.intellij.serviceexplorer.azure.storage.ConfirmDialogAction;
import com.microsoft.intellij.serviceexplorer.azure.storage.CreateBlobContainer;
import com.microsoft.intellij.serviceexplorer.azure.storage.CreateQueueAction;
import com.microsoft.intellij.serviceexplorer.azure.storage.ModifyExternalStorageAccountAction;
import com.microsoft.intellij.serviceexplorer.azure.webapps.OpenWebappAction;
import com.microsoft.intellij.serviceexplorer.azure.webapps.RemoteDebugAction;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.docker.DockerHostModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.docker.DockerHostNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.*;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapps.WebappNode;

import java.util.HashMap;
import java.util.Map;

public class NodeActionsMap {
    public static final Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> node2Actions =
            new HashMap<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>>();

    static {
        node2Actions.put(VMArmModule.class, new ImmutableList.Builder().add(com.microsoft.intellij.serviceexplorer.azure.vmarm.CreateVMAction.class).build());
        node2Actions.put(QueueModule.class, new ImmutableList.Builder().add(CreateQueueAction.class).build());
        node2Actions.put(TableModule.class, new ImmutableList.Builder().add(com.microsoft.intellij.serviceexplorer.azure.storage.CreateTableAction.class).build());
        node2Actions.put(BlobModule.class, new ImmutableList.Builder().add(CreateBlobContainer.class).build());
        node2Actions.put(StorageModule.class, new ImmutableList.Builder().add(com.microsoft.intellij.serviceexplorer.azure.storagearm.CreateStorageAccountAction.class).build());
        node2Actions.put(RedisCacheModule.class, new ImmutableList.Builder().add(com.microsoft.intellij.serviceexplorer.azure.rediscache.CreateRedisCacheAction.class).build());
        node2Actions.put(StorageNode.class, new ImmutableList.Builder().add(CreateBlobContainer.class).build());
        // todo: what is ConfirmDialogAction?
        node2Actions.put(ExternalStorageNode.class, new ImmutableList.Builder().add(ConfirmDialogAction.class, ModifyExternalStorageAccountAction.class).build());
        node2Actions.put(WebappNode.class, new ImmutableList.Builder().add(OpenWebappAction.class/*, RemoteDebugAction.class*/).build());
        node2Actions.put(HDInsightRootModuleImpl.class, new ImmutableList.Builder().add(AddNewClusterAction.class, AddNewEmulatorAction.class).build());
        node2Actions.put(DockerHostNode.class, new ImmutableList.Builder().add(ViewDockerHostAction.class, DeployDockerContainerAction.class, DeleteDockerHostAction.class).build());
        node2Actions.put(DockerHostModule.class, new ImmutableList.Builder().add(CreateNewDockerHostAction.class, PublishDockerContainerAction.class).build());
    }
}
