/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureText;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.mvp.ui.base.NodeContent;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class RefreshableNode extends Node {
    protected boolean initialized;
    public static String REFRESH_ICON_LIGHT = "RefreshLight_16.png";
    public static String REFRESH_ICON_DARK = "RefreshDark_16.png";
    private static final String REFRESH = "Refresh";

    public RefreshableNode(String id, String name, Node parent) {
        super(id, name, parent);
    }

    public RefreshableNode(String id, String name, Node parent, String iconPath) {
        super(id, name, parent, iconPath);
    }

    public RefreshableNode(String id, String name, Node parent, boolean delayActionLoading) {
        super(id, name, parent, delayActionLoading);
    }

    public RefreshableNode(String id, String name, Node parent, String iconPath, boolean delayActionLoading) {
        super(id, name, parent, iconPath, delayActionLoading);
    }

    @Override
    protected void loadActions() {
        addAction(new DelegateActionListener.BasicActionListener(new RefreshActionListener(), AzureActionEnum.REFRESH));
        super.loadActions();
    }

    @Override
    public List<NodeAction> getNodeActions() {
        getNodeActionByName(REFRESH).setIconPath(DefaultLoader.getUIHelper().isDarkTheme() ? REFRESH_ICON_DARK : REFRESH_ICON_LIGHT);

        return super.getNodeActions();
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        if (!initialized) {
            this.load(false);
        } else {
            expandNodeAfterLoading();
        }
    }

    // Sub-classes are expected to override this method if they wish to
    // refresh items synchronously. The default implementation does nothing.
    protected abstract void refreshItems() throws AzureCmdException;

    // Sub-classes are expected to override this method if they wish
    // to refresh items asynchronously. The default implementation simply
    // delegates to "refreshItems" *synchronously* and completes the Future
    // with the result of calling getChildNodes.
    protected synchronized void refreshItems(SettableFuture<List<Node>> future, boolean forceRefresh) {
        if (!loading) {
            setLoading(true);
            try {
                removeAllChildNodes();
                if (forceRefresh) {
                    refreshFromAzure();
                }
                refreshItems();
                future.set(getChildNodes());
            } catch (Exception e) {
                future.setException(e);
            } finally {
                setLoading(false);
            }
        }
    }

    protected void refreshFromAzure() throws Exception {
    }

    // Add update node name support after refresh the node
    protected void updateNodeNameAfterLoading() {
    }

    protected void expandNodeAfterLoading() {
        if (tree != null && treePath != null) {
            tree.expandPath(treePath);
        }
    }

    public ListenableFuture<List<Node>> load(boolean forceRefresh) {
        initialized = true;
        final RefreshableNode node = this;
        final SettableFuture<List<Node>> future = SettableFuture.create();

        final AzureText title = AzureOperationBundle.title("common|node.load_content", node.getName());
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(getProject(), title, false, new Runnable() {
            @Override
            public void run() {
                if (!loading) {
                    final String nodeName = node.getName();
                    DefaultLoader.getIdeHelper().invokeLater(() -> updateName(nodeName + " (Refreshing...)", null));

                    Futures.addCallback(future, new FutureCallback<List<Node>>() {
                        @Override
                        public void onSuccess(List<Node> nodes) {
                            DefaultLoader.getIdeHelper().invokeLater(() -> {
                                if (node.getName().endsWith("(Refreshing...)")) {
                                    updateName(nodeName, null);
                                }
                                updateNodeNameAfterLoading();
                                expandNodeAfterLoading();
                            });
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            DefaultLoader.getIdeHelper().invokeLater(() -> {
                                updateName(nodeName, throwable);
                                updateNodeNameAfterLoading();
                                expandNodeAfterLoading();
                            });
                        }
                    }, MoreExecutors.directExecutor());
                    node.refreshItems(future, forceRefresh);
                }
            }

            private void updateName(String name, final Throwable throwable) {
                node.setName(name);

                if (throwable != null) {
                    AzureMessager.getMessager().error(throwable);
                }
            }
        }));

        return future;
    }

    public void showNode(HashMap<String, ArrayList<NodeContent>> nodeMap) {
        for (String sid: nodeMap.keySet()) {
            for (NodeContent content: nodeMap.get(sid)) {
                addChildNode(createNode(this, sid, content));
            }
        }
    }

    private class RefreshActionListener extends NodeActionListener {

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            RefreshableNode.this.load(true);
        }
    }
}
