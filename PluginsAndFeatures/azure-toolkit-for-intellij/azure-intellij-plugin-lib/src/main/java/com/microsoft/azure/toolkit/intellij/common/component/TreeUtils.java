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
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.microsoft.azure.toolkit.ide.common.component.NodeView;
import com.microsoft.azure.toolkit.intellij.common.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Objects;
import java.util.Optional;

public class TreeUtils {

    public static final int INLINE_ACTION_ICON_OFFSET = 28;
    public static final int INLINE_ACTION_ICON_WIDTH = 16;

    public static void installSelectionListener(@Nonnull JTree tree) {
        tree.addTreeSelectionListener(e -> {
            final Object node = tree.getLastSelectedPathComponent();
            Disposable selectionDisposable = (Disposable) tree.getClientProperty("SELECTION_DISPOSABLE");
            if (selectionDisposable != null) {
                selectionDisposable.dispose();
            }
            if (node instanceof Tree.TreeNode) {
                final String place = "azure.component.tree";
                final ActionGroup actions = ((Tree.TreeNode<?>) node).inner.actions();
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
                    final String place = "azure.component.tree";
                    if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
                        final ActionGroup actions = node.inner.actions();
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
                final String place = "azure.component.tree";
                if (Objects.nonNull(node) && e.getClickCount() == 1 && isMouseAtInlineActionIcon(tree, e)) {
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

    private static IntellijAzureActionManager.ActionGroupWrapper toIntellijActionGroup(ActionGroup actions) {
        final ActionManager am = ActionManager.getInstance();
        if (actions.getOrigin() instanceof IntellijAzureActionManager.ActionGroupWrapper) {
            return (IntellijAzureActionManager.ActionGroupWrapper) actions.getOrigin();
        }
        return new IntellijAzureActionManager.ActionGroupWrapper(actions);
    }

    public static void renderMyTreeNode(@Nonnull Tree.TreeNode<?> node, @Nonnull SimpleColoredComponent renderer) {
        final NodeView view = node.inner.view();
        if (BooleanUtils.isFalse(node.loaded)) {
            renderer.setIcon(AnimatedIcon.Default.INSTANCE);
        } else {
            renderer.setIcon(AzureIcons.getIcon(view.getIcon()));
        }
        renderer.append(view.getLabel(), view.isEnabled() ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAY_ATTRIBUTES);
        renderer.append(Optional.ofNullable(view.getDescription()).map(d -> " " + d).orElse(""), SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES, true);
        renderer.setToolTipText(Optional.ofNullable(view.getTips()).orElse(view.getLabel()));
    }
}
