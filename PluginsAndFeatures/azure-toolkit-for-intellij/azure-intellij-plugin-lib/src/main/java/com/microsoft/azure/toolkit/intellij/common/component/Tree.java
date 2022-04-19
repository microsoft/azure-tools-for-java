/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component;

import com.google.common.collect.Sets;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.LoadingNode;
import com.intellij.ui.TreeUIHelper;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.tree.TreeUtil;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.component.NodeView;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.intellij.ui.AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED;

@Getter
public class Tree extends SimpleTree implements DataProvider {
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
        ComponentUtil.putClientProperty(this, ANIMATION_IN_RENDERER_ALLOWED, true);
        this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        TreeUtil.installActions(this);
        TreeUIHelper.getInstance().installTreeSpeedSearch(this);
        TreeUIHelper.getInstance().installSmartExpander(this);
        TreeUIHelper.getInstance().installSelectionSaver(this);
        TreeUIHelper.getInstance().installEditSourceOnEnterKeyHandler(this);
        this.setCellRenderer(new NodeRenderer());
        this.setModel(new DefaultTreeModel(new TreeNode<>(root, this)));
        TreeUtils.installExpandListener(this);
        TreeUtils.installSelectionListener(this);
        TreeUtils.installMouseListener(this);
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

    @EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
    public static class TreeNode<T> extends DefaultMutableTreeNode implements NodeView.Refresher {
        @Nonnull
        @EqualsAndHashCode.Include
        protected final Node<T> inner;
        protected final JTree tree;
        Boolean loaded = null; //null:not loading/loaded, false: loading: true: loaded

        public TreeNode(@Nonnull Node<T> n, JTree tree) {
            super(n.data(), n.hasChildren());
            this.inner = n;
            this.tree = tree;
            if (this.getAllowsChildren()) {
                this.add(new LoadingNode());
            }
            if (!this.inner.lazy()) {
                this.loadChildren();
            }
            final NodeView view = this.inner.view();
            view.setRefresher(this);
        }

        @Override
        // NOTE: equivalent nodes in same tree will cause rendering problems.
        public javax.swing.tree.TreeNode getParent() {
            return super.getParent();
        }

        public T getData() {
            return this.inner.data();
        }

        public String getLabel() {
            return this.inner.view().getLabel();
        }

        @Nullable
        public IView.Label getInlineActionView() {
            return Optional.ofNullable(this.inner.inlineAction())
                .map(a -> a.getView(this.inner.data()))
                .filter(IView.Label::isEnabled)
                .orElse(null);
        }

        @Override
        public void refreshView() {
            if (this.getParent() != null) {
                ((DefaultTreeModel) this.tree.getModel()).nodeChanged(this);
            }
        }

        @Override
        public synchronized void refreshChildren(boolean... incremental) {
            if (this.getAllowsChildren() && BooleanUtils.isNotFalse(this.loaded)) {
                final DefaultTreeModel model = (DefaultTreeModel) this.tree.getModel();
                if (incremental.length > 0 && incremental[0]) {
                    model.insertNodeInto(new LoadingNode(), this, 0);
                } else {
                    this.removeAllChildren();
                    this.add(new LoadingNode());
                    if (Objects.nonNull(this.getParent())) {
                        ((DefaultTreeModel) this.tree.getModel()).nodeStructureChanged(this);
                    }
                }
                this.loaded = null;
                this.loadChildren(incremental);
            }
        }

        protected synchronized void loadChildren(boolean... incremental) {
            if (loaded != null) {
                return; // return if loading/loaded
            }
            this.loaded = false;
            final AzureTaskManager tm = AzureTaskManager.getInstance();
            tm.runOnPooledThread(() -> {
                final List<Node<?>> children = this.inner.getChildren();
                if (incremental.length > 0 && incremental[0]) {
                    tm.runLater(() -> updateChildren(children));
                } else {
                    tm.runLater(() -> setChildren(children));
                }
            });
        }

        private synchronized void setChildren(List<Node<?>> children) {
            this.removeAllChildren();
            children.stream().map(c -> new TreeNode<>(c, this.tree)).forEach(this::add);
            this.loaded = true;
            if (Objects.nonNull(this.getParent())) {
                ((DefaultTreeModel) this.tree.getModel()).nodeStructureChanged(this);
            }
        }

        private synchronized void updateChildren(List<Node<?>> children) {
            final Map<Object, DefaultMutableTreeNode> oldChildren = IntStream.range(1, this.getChildCount()).mapToObj(this::getChildAt)
                .filter(n -> n instanceof DefaultMutableTreeNode).map(n -> ((DefaultMutableTreeNode) n))
                .collect(Collectors.toMap(DefaultMutableTreeNode::getUserObject, n -> n));

            final Set<Object> newChildrenData = children.stream().map(Node::data).collect(Collectors.toSet());
            final Set<Object> oldChildrenData = oldChildren.keySet();
            Sets.difference(oldChildrenData, newChildrenData).forEach(o -> oldChildren.get(o).removeFromParent());

            TreePath toSelect = null;
            for (int i = 0; i < children.size(); i++) {
                final Node<?> node = children.get(i);
                if (!oldChildrenData.contains(node.data())) {
                    final TreeNode<?> treeNode = new TreeNode<>(node, this.tree);
                    this.insert(treeNode, i + 1);
                    toSelect = new TreePath(treeNode.getPath());
                } else { // discarded nodes should be disposed manually to unregister listeners.
                    node.dispose();
                }
            }
            this.remove(0);
            if (Objects.nonNull(this.getParent())) {
                ((DefaultTreeModel) this.tree.getModel()).nodeStructureChanged(this);
            }
            Optional.ofNullable(toSelect).ifPresent(p -> TreeUtil.selectPath(this.tree, p, false));
            this.loaded = true;
        }

        public synchronized void clearChildren() {
            this.removeAllChildren();
            this.loaded = null;
            if (this.getAllowsChildren()) {
                this.add(new LoadingNode());
                this.tree.collapsePath(new TreePath(this.getPath()));
            }
            if (Objects.nonNull(this.getParent())) {
                ((DefaultTreeModel) this.tree.getModel()).nodeStructureChanged(this);
            }
        }

        @Override
        public void setParent(MutableTreeNode newParent) {
            super.setParent(newParent);
            if (this.getParent() == null) {
                this.inner.dispose();
            }
        }
    }

    public static class NodeRenderer extends com.intellij.ide.util.treeView.NodeRenderer {

        @Override
        public void customizeCellRenderer(@Nonnull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (value instanceof TreeNode) {
                TreeUtils.renderMyTreeNode((TreeNode<?>) value, this);
            } else {
                super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
            }
        }
    }
}

