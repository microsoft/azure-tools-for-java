/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.explorer;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.RelativeFont;
import com.intellij.ui.render.RenderingUtil;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.tree.TreeUtil;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.Optional;

public abstract class AzureTree extends SimpleTree {

    public AzureTree() {
        super(new DefaultTreeModel(new DefaultMutableTreeNode(null)));
        putClientProperty(RenderingUtil.ALWAYS_PAINT_SELECTION_AS_FOCUSED, true);
        setCellRenderer(new AzureNodeRenderer());
        setRootVisible(false);
        setShowsRootHandles(true);
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        TreeUtil.installActions(this);
        RelativeFont.BOLD.install(this);
        // right click to show popup actions.
        addMouseListener(new PopupHandler() {
            @Override
            public void invokePopup(Component comp, int x, int y) {
                Optional.ofNullable(AzureTree.this.getSelectedAzureNode())
                        .filter(selectedNode -> selectedNode.getActionGroup().getChildrenCount() > 0)
                        .ifPresent(selectedNode -> ActionManager.getInstance()
                                .createActionPopupMenu(selectedNode.toString() + "-actions", selectedNode.getActionGroup())
                                .getComponent()
                                .show(comp, x, y)
                        );
            }
        });
    }

    @Override
    public DefaultTreeModel getModel() {
        return (DefaultTreeModel) super.getModel();
    }

    public DefaultMutableTreeNode getRootNode() {
        return (DefaultMutableTreeNode) getModel().getRoot();
    }

    public AzureNode<?> getSelectedAzureNode() {
        AzureNode<?>[] arrayOfAbstractNode = this.getSelectedNodes(AzureNode.class, null);
        if (arrayOfAbstractNode.length == 0) {
            return null;
        }
        return arrayOfAbstractNode[0];
    }

    public abstract void loadNodes();

}
