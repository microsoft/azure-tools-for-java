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
package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;

import java.util.Map;

public class BlobContainerNode extends Node{
    private static final String CONTAINER_MODULE_ID = BlobContainerNode.class.getName();
    private static final String ICON_PATH = CommonConst.BlobContainerIConPath;
    private static final String DEFAULT_CONTAINER_FLAG = "(default)";

    private ClientStorageAccount storageAccount;
    private BlobContainer blobContainer;

    public BlobContainerNode(Node parent, ClientStorageAccount storageAccount, BlobContainer blobContainer) {
        this(parent, storageAccount, blobContainer, false);
    }

    public BlobContainerNode(Node parent, ClientStorageAccount storageAccount, BlobContainer blobContainer, boolean isDefaultContainer) {
        super(CONTAINER_MODULE_ID, isDefaultContainer ? blobContainer.getName() + DEFAULT_CONTAINER_FLAG : blobContainer.getName(), parent, ICON_PATH);
        this.storageAccount = storageAccount;
        this.blobContainer = blobContainer;
    }

    public class RefreshAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            DefaultLoader.getUIHelper().refreshBlobs(getProject(), storageAccount.getName(), blobContainer);
        }
    }

    public class ViewBlobContainer extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            onNodeClick(e);
        }
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
//        TelemetryManager.postEvent(TelemetryCommon.HDInsightExplorerContainerOpen, null, null);
        final Object openedFile = DefaultLoader.getUIHelper().getOpenedFile(getProject(), storageAccount.getName(), blobContainer);
        if (openedFile == null) {
            DefaultLoader.getUIHelper().openItem(getProject(), storageAccount, blobContainer, " [Container]", "BlobContainer", CommonConst.BlobContainerIConPath);
        } else {
            DefaultLoader.getUIHelper().openItem(getProject(), openedFile);
        }
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        return ImmutableMap.of(
                "Refresh", RefreshAction.class,
                "View Blob Container", ViewBlobContainer.class
                );
    }

    @Override
    public String getServiceName() {
        return TelemetryConstants.HDINSIGHT;
    }
}
