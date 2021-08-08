/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.LoadingNode;
import com.intellij.ui.TreeUIHelper;
import com.intellij.ui.treeStructure.SimpleTree;
import com.microsoft.azure.toolkit.ide.common.action.Action;
import com.microsoft.azure.toolkit.ide.common.action.ActionGroup;
import com.microsoft.azure.toolkit.ide.common.component.IView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.intellij.common.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAzureActionManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;

@Getter
public class Tree extends SimpleTree implements TreeWillExpandListener, DataProvider {
    protected Node<?> root;

    public Tree() {
        super();
    }

    public Tree(Node<?> root) {
        super();
        this.root = root;
        init(root);
    }

    protected void init(@Nonnull Node<?> root) {
        this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        TreeUIHelper.getInstance().installTreeSpeedSearch(this);
        TreeUIHelper.getInstance().installSmartExpander(this);
        TreeUIHelper.getInstance().installSelectionSaver(this);
        TreeUIHelper.getInstance().installEditSourceOnEnterKeyHandler(this);
        this.setCellRenderer(new NodeRenderer());
        this.setModel(new DefaultTreeModel(buildNode(root)));
        this.addTreeWillExpandListener(this);
        this.installPopupMenu();
    }

    private void installPopupMenu() {
        final MouseAdapter popupHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
                    final Tree tree = Tree.this;
                    final TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());
                    if (path == null) {
                        return;
                    }
                    final Object node = path.getLastPathComponent();
                    if (node instanceof TreeNode) {
                        final ActionGroup actions = ((TreeNode<?>) node).inner.actions();
                        if (Objects.nonNull(actions)) {
                            final ActionManager am = ActionManager.getInstance();
                            final ActionPopupMenu menu = am.createActionPopupMenu("azure.component.tree", toIntellijActionGroup(actions));
                            menu.setTargetComponent(tree);
                            menu.getComponent().show(tree, e.getX(), e.getY());
                        }
                    }
                }
            }

            private com.intellij.openapi.actionSystem.ActionGroup toIntellijActionGroup(ActionGroup actions) {
                final ActionManager am = ActionManager.getInstance();
                if (actions instanceof ActionGroup.Proxy) {
                    final String id = ((ActionGroup.Proxy) actions).id();
                    if (Objects.nonNull(id)) {
                        return (com.intellij.openapi.actionSystem.ActionGroup) am.getAction(id);
                    }
                }
                return new IntellijAzureActionManager.ActionGroupWrapper(actions);
            }
        };
        this.addMouseListener(popupHandler);
    }

    @Nonnull
    protected MutableTreeNode buildNode(@Nonnull Node<?> node) {
        return new TreeNode<>(node, this);
    }

    @Override
    public void treeWillExpand(TreeExpansionEvent event) {
        final Object component = event.getPath().getLastPathComponent();
        if (component instanceof TreeNode) {
            final TreeNode<?> treeNode = (TreeNode<?>) component;
            if (treeNode.getAllowsChildren()) {
                treeNode.loadChildren();
            }
        }
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) {

    }

    @Override
    public @Nullable Object getData(@Nonnull String dataId) {
        if (StringUtils.equals(dataId, Action.SOURCE)) {
            final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) this.getLastSelectedPathComponent();
            if (Objects.nonNull(selectedNode)) {
                return selectedNode.getUserObject();
            }
        }
        return null;
    }

    public static class TreeNode<T> extends DefaultMutableTreeNode implements IView.Dynamic.Updater {
        protected final Node<T> inner;
        protected final Tree tree;
        private Boolean loaded = null; //null:not loading/loaded, false: loading: true: loaded

        public TreeNode(Node<T> n, Tree tree) {
            super(n.data(), n.hasChildren());
            this.inner = n;
            this.tree = tree;
            if (this.inner.hasChildren()) {
                this.add(new LoadingNode());
            }
            if (!this.inner.lazy()) {
                this.loadChildren();
            }
            final IView.Label view = this.inner.view();
            if (view instanceof IView.Dynamic) {
                ((IView.Dynamic) view).setUpdater(this);
            }
        }

        @Override
        public void updateView() {
            if (this.getParent() != null) {
                ((DefaultTreeModel) this.tree.getModel()).nodeChanged(this);
            }
        }

        @Override
        public synchronized void updateChildren() {
            this.removeAllChildren();
            this.add(new LoadingNode());
            this.loaded = null;
            ((DefaultTreeModel) this.tree.getModel()).reload(this);
            this.loadChildren();
        }

        protected synchronized void loadChildren() {
            if (loaded != null) return; // return if loading/loaded
            this.loaded = false;
            final AzureTaskManager tm = AzureTaskManager.getInstance();
            tm.runOnPooledThread(() -> {
                final List<Node<?>> children = this.inner.getChildren();
                tm.runLater(() -> {
                    synchronized (this) {
                        this.removeAllChildren();
                        this.setAllowsChildren(children.size() > 0);
                        for (final Node<?> child : children) {
                            this.add(this.tree.buildNode(child));
                        }
                        this.loaded = true;
                    }
                    ((DefaultTreeModel) this.tree.getModel()).reload(this);
                });
            });
        }

        @Override
        public void setParent(MutableTreeNode newParent) {
            super.setParent(newParent);
            final IView.Label view = this.inner.view();
            if (parent == null && view != null) {
                view.dispose();
            }
        }
    }

    public static class NodeRenderer extends com.intellij.ide.util.treeView.NodeRenderer {
        @Override
        public void customizeCellRenderer(@Nonnull JTree tree, Object node, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Object value = node;
            if (node instanceof TreeNode) {
                final IView.Label view = ((TreeNode<?>) node).inner.view();
                if (view != null) {
                    if (StringUtils.isNotBlank(view.getIconPath())) {
                        final Icon icon = AzureIcons.getIcon(view.getIconPath(), Tree.class);
                        this.setIcon(icon);
                    }
                    value = view.getTitle();
                    this.setToolTipText(view.getDescription());
                }
                if (BooleanUtils.isFalse(((TreeNode<?>) node).loaded)) {
                    this.setIcon(new AnimatedIcon.Default());
                }
            }
            super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
        }
    }
}

