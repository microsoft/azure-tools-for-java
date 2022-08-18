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
import com.intellij.openapi.actionSystem.EmptyAction;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.tree.TreeUtil;
import com.microsoft.azure.toolkit.ide.common.component.NodeView;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAzureActionManager;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Objects;
import java.util.Optional;

public class TreeUtils {
    public static final Key<Pair<Object, Long>> HIGHLIGHTED_RESOURCE_KEY = Key.create("TreeHighlightedResource");
    public static final int INLINE_ACTION_ICON_OFFSET = 28;
    public static final int INLINE_ACTION_ICON_WIDTH = 16;

    public static void installSelectionListener(@Nonnull JTree tree) {
        tree.addTreeSelectionListener(e -> {
            final Object n = tree.getLastSelectedPathComponent();
            Disposable selectionDisposable = (Disposable) tree.getClientProperty("SELECTION_DISPOSABLE");
            if (selectionDisposable != null) {
                Disposer.dispose(selectionDisposable);
            }
            if (n instanceof Tree.TreeNode) {
                final Tree.TreeNode<?> node = (Tree.TreeNode<?>) n;
                final String place = "azure.explorer." + (TreeUtils.isInAppCentricView(node) ? "app" : "type");
                final IActionGroup actions = node.inner.actions();
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

    public static void installExpandListener(@Nonnull JTree tree) {
        final TreeWillExpandListener listener = new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) {
                final Object component = event.getPath().getLastPathComponent();
                if (component instanceof Tree.TreeNode) {
                    final Tree.TreeNode<?> treeNode = (Tree.TreeNode<?>) component;
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

    public static void installMouseListener(@Nonnull JTree tree) {
        tree.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                final Tree.TreeNode<?> node = getTreeNodeAtMouse(tree, e);
                final boolean inlineActionEnabled = Optional.ofNullable(node).map(Tree.TreeNode::getInlineActionView)
                    .map(IView.Label::isEnabled).orElse(false);
                final boolean isMouseAtActionIcon = inlineActionEnabled && isMouseAtInlineActionIcon(tree, e);
                final Cursor cursor = isMouseAtActionIcon ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor();
                tree.setCursor(cursor);
            }
        });
        final MouseAdapter popupHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final Object n = tree.getLastSelectedPathComponent();
                if (n instanceof Tree.TreeNode) {
                    final Tree.TreeNode<?> node = (Tree.TreeNode<?>) n;
                    final String place = "azure.explorer." + (TreeUtils.isInAppCentricView(node) ? "app" : "type");
                    if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
                        final IActionGroup actions = node.inner.actions();
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
                        node.inner.triggerDoubleClickAction(event);
                    }
                }
                super.mouseClicked(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                final Tree.TreeNode<?> node = getTreeNodeAtMouse(tree, e);
                if (Objects.nonNull(node) && e.getClickCount() == 1 && isMouseAtInlineActionIcon(tree, e)) {
                    final String place = "azure.explorer." + (TreeUtils.isInAppCentricView(node) ? "app" : "type");
                    final DataContext context = DataManager.getInstance().getDataContext(tree);
                    final AnActionEvent event = AnActionEvent.createFromAnAction(new EmptyAction(), e, place, context);
                    node.inner.triggerInlineAction(event);
                }
            }
        };
        tree.addMouseListener(popupHandler);
    }

    @Nullable
    public static Tree.TreeNode<?> getTreeNodeAtMouse(@Nonnull JTree tree, MouseEvent e) {
        final TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());
        if (path == null) {
            return null;
        }
        final Object node = path.getLastPathComponent();
        if (node instanceof Tree.TreeNode) {
            return (Tree.TreeNode<?>) node;
        }
        return null;
    }

    private static boolean isMouseAtInlineActionIcon(@Nonnull JTree tree, MouseEvent e) {
        final int width = tree.getWidth();
        final int x = e.getX() - INLINE_ACTION_ICON_WIDTH;
        return x > width - (INLINE_ACTION_ICON_OFFSET + INLINE_ACTION_ICON_WIDTH) && x < width - INLINE_ACTION_ICON_OFFSET;
    }

    private static IntellijAzureActionManager.ActionGroupWrapper toIntellijActionGroup(IActionGroup actions) {
        final ActionManager am = ActionManager.getInstance();
        if (actions instanceof IntellijAzureActionManager.ActionGroupWrapper) {
            return (IntellijAzureActionManager.ActionGroupWrapper) actions;
        }
        return new IntellijAzureActionManager.ActionGroupWrapper((ActionGroup) actions);
    }

    public static void renderMyTreeNode(JTree tree, @Nonnull Tree.TreeNode<?> node, boolean selected, @Nonnull SimpleColoredComponent renderer) {
        final NodeView view = node.inner.view();
        if (BooleanUtils.isFalse(node.loaded)) {
            renderer.setIcon(AnimatedIcon.Default.INSTANCE);
        } else {
            renderer.setIcon(IntelliJAzureIcons.getIcon(view.getIcon()));
        }
        final Object highlighted = tree.getClientProperty(HIGHLIGHTED_RESOURCE_KEY);
        final boolean toHighlightThisNode = Optional.ofNullable(highlighted).map(h -> ((Pair<Object, Long>) h))
            .filter(h -> Objects.equals(node.getUserObject(), h.getLeft())).isPresent();
        SimpleTextAttributes attributes = view.isEnabled() ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAY_ATTRIBUTES;
        if (selected && toHighlightThisNode) {
            attributes = attributes.derive(SimpleTextAttributes.STYLE_SEARCH_MATCH, JBColor.RED, JBColor.YELLOW, null);
        } else if (selected) {
            tree.putClientProperty(HIGHLIGHTED_RESOURCE_KEY, null);
        }
        renderer.append(view.getLabel(), attributes);
        renderer.append(Optional.ofNullable(view.getDescription()).map(d -> " " + d).orElse(""), SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES, true);
        renderer.setToolTipText(Optional.ofNullable(view.getTips()).orElse(view.getLabel()));
    }

    public static boolean isInAppCentricView(@Nonnull DefaultMutableTreeNode node) {
        return isInAppCentricView(new TreePath(node.getPath()));
    }

    public static boolean isInAppCentricView(@Nonnull TreePath path) {
        if (path.getPathCount() < 2) {
            return false;
        }
        final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getPathComponent(1);
        return treeNode.getUserObject() instanceof AzureResources;
    }

    public static void highlightResource(@Nonnull JTree tree, @Nonnull Object resource) {
        Condition<DefaultMutableTreeNode> condition = n -> isInAppCentricView(n) && Objects.equals(n.getUserObject(), resource);
        if (resource instanceof AzService) {
            condition = n -> Objects.equals(n.getUserObject(), resource);
        }
        final DefaultMutableTreeNode node = TreeUtil.findNode((DefaultMutableTreeNode) tree.getModel().getRoot(), condition);
        AzureTaskManager.getInstance().runLater(() -> {
            tree.putClientProperty(HIGHLIGHTED_RESOURCE_KEY, Pair.of(resource, System.currentTimeMillis()));
            Optional.ofNullable(node).ifPresent(n -> TreeUtil.selectPath(tree, new TreePath(node.getPath()), false));
        }, AzureTask.Modality.ANY);
    }
}
