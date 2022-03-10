/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component;

import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.EmptyAction;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.LoadingNode;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.TreeUIHelper;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.util.ui.tree.TreeUtil;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.component.NodeView;
import com.microsoft.azure.toolkit.intellij.common.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import lombok.EqualsAndHashCode;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        installExpandListener(this);
        installSelectionListener(this);
        installPopupMenu(this);
    }

    public static void installSelectionListener(JTree tree) {
        tree.addTreeSelectionListener(e -> {
            final Object node = tree.getLastSelectedPathComponent();
            Disposable selectionDisposable = (Disposable) tree.getClientProperty("SELECTION_DISPOSABLE");
            if (selectionDisposable != null) {
                selectionDisposable.dispose();
            }
            if (node instanceof TreeNode) {
                final String place = "azure.component.tree";
                final ActionGroup actions = ((TreeNode<?>) node).inner.actions();
                if (Objects.nonNull(actions)) {
                    final ActionManager am = ActionManager.getInstance();
                    selectionDisposable = Disposer.newDisposable();
                    tree.putClientProperty("SELECTION_DISPOSABLE", selectionDisposable);
                    final IntellijAzureActionManager.ActionGroupWrapper group = toIntellijActionGroup(actions);
                    group.registerCustomShortcutSetForActions(tree, selectionDisposable);
                }
            }
        });
    }

    public static void installExpandListener(JTree tree) {
        final TreeWillExpandListener listener = new TreeWillExpandListener() {
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
        };
        tree.addTreeWillExpandListener(listener);
    }

    public static void installPopupMenu(JTree tree) {
        final MouseAdapter popupHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                final TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());
                if (path == null) {
                    return;
                }
                final Object node = path.getLastPathComponent();
                if (node instanceof TreeNode) {
                    final String place = "azure.component.tree";
                    if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
                        final ActionGroup actions = ((TreeNode<?>) node).inner.actions();
                        if (Objects.nonNull(actions)) {
                            final ActionManager am = ActionManager.getInstance();
                            final IntellijAzureActionManager.ActionGroupWrapper group = toIntellijActionGroup(actions);
                            final ActionPopupMenu menu = am.createActionPopupMenu(place, group);
                            menu.setTargetComponent(tree);
                            final JPopupMenu popupMenu = menu.getComponent();
                            popupMenu.show(tree, e.getX(), e.getY());
                        }
                    } else if (e.getClickCount() == 2) {
                        final DataContext context = DataManager.getInstance().getDataContext(tree);
                        final AnActionEvent event = AnActionEvent.createFromAnAction(new EmptyAction(), e, place, context);
                        ((TreeNode<?>) node).inner.doubleClick(event);
                    }
                }
            }
        };
        tree.addMouseListener(popupHandler);
    }

    private static IntellijAzureActionManager.ActionGroupWrapper toIntellijActionGroup(ActionGroup actions) {
        final ActionManager am = ActionManager.getInstance();
        if (actions.getOrigin() instanceof IntellijAzureActionManager.ActionGroupWrapper) {
            return (IntellijAzureActionManager.ActionGroupWrapper) actions.getOrigin();
        }
        return new IntellijAzureActionManager.ActionGroupWrapper(actions);
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
        @EqualsAndHashCode.Include
        protected final Node<T> inner;
        protected final JTree tree;
        private Boolean loaded = null; //null:not loading/loaded, false: loading: true: loaded

        public TreeNode(Node<T> n, JTree tree) {
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

        public T getData() {
            return this.inner.data();
        }

        public String getLabel() {
            return this.inner.view().getLabel();
        }

        @Override
        public void refreshView() {
            if (this.getParent() != null) {
                ((DefaultTreeModel) this.tree.getModel()).nodeChanged(this);
            }
        }

        @Override
        public synchronized void refreshChildren() {
            if (this.getAllowsChildren() && BooleanUtils.isNotFalse(this.loaded)) {
                final DefaultTreeModel model = (DefaultTreeModel) this.tree.getModel();
                model.insertNodeInto(new LoadingNode(), this, 0);
                this.loaded = null;
                this.loadChildren();
            }
        }

        protected synchronized void loadChildren() {
            if (loaded != null) {
                return; // return if loading/loaded
            }
            this.loaded = false;
            final AzureTaskManager tm = AzureTaskManager.getInstance();
            tm.runOnPooledThread(() -> {
                try {
                    final List<Node<?>> children = this.inner.getChildren();
                    tm.runLater(() -> setChildren(children.stream().map(c -> new TreeNode<>(c, this.tree))));
                } catch (final Exception e) {
                    this.setChildren(Stream.empty());
                    AzureMessager.getMessager().error(e);
                }
            });
        }

        private synchronized void setChildren(Stream<? extends DefaultMutableTreeNode> children) {
            final DefaultTreeModel model = (DefaultTreeModel) this.tree.getModel();
            final List<? extends MutableTreeNode> ordered = children.collect(Collectors.toList());
            final Set<? extends MutableTreeNode> newChildren = new HashSet<>(ordered);
            final Set<javax.swing.tree.TreeNode> oldChildren = new HashSet<>();
            final int count = this.getChildCount();
            for (int i = count - 1; i > 0; i--) {
                final javax.swing.tree.TreeNode node = this.getChildAt(i);
                if (node instanceof MutableTreeNode && !newChildren.contains(node)) {
                    model.removeNodeFromParent((MutableTreeNode) node);
                } else {
                    oldChildren.add(node);
                }
            }
            for (int i = 0; i < ordered.size(); i++) {
                final MutableTreeNode node = ordered.get(i);
                if (!oldChildren.contains(node)) {
                    model.insertNodeInto(node, this, i + 1);
                }
            }
            model.removeNodeFromParent((MutableTreeNode) this.getChildAt(0));
            this.loaded = true;
        }

        public synchronized void clearChildren() {
            this.removeAllChildren();
            this.loaded = null;
            if (this.getAllowsChildren()) {
                this.add(new LoadingNode());
                this.tree.collapsePath(new TreePath(this.getPath()));
            }
            ((DefaultTreeModel) this.tree.getModel()).reload(this);
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
        public static void renderMyTreeNode(@Nonnull TreeNode<?> node, @Nonnull SimpleColoredComponent renderer) {
            final IView.Label view = node.inner.view();
            if (BooleanUtils.isFalse(node.loaded)) {
                renderer.setIcon(AnimatedIcon.Default.INSTANCE);
            } else if (view instanceof NodeView) {
                renderer.setIcon(AzureIcons.getIcon(((NodeView) view).getIcon()));
            } else if (StringUtils.isNotBlank(view.getIconPath())) {
                renderer.setIcon(AzureIcons.getIcon(view.getIconPath(), Tree.class));
            }
            renderer.append(view.getLabel());
            renderer.append(Optional.ofNullable(view.getDescription()).map(d -> " " + d).orElse(""), SimpleTextAttributes.GRAY_ATTRIBUTES, true);
            renderer.setToolTipText(Optional.ofNullable(view.getDescription()).map(d -> view.getLabel() + ":" + d).orElse(view.getLabel()));
        }

        @Override
        public void customizeCellRenderer(@Nonnull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if (value instanceof TreeNode) {
                renderMyTreeNode((TreeNode<?>) value, this);
            } else {
                super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
            }
        }
    }
}

