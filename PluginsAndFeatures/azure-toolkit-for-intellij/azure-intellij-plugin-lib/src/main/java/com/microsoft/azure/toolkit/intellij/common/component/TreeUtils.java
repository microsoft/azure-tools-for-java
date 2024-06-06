/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.util.ui.tree.TreeModelAdapter;
import com.intellij.util.ui.tree.TreeUtil;
import com.microsoft.azure.toolkit.ide.common.component.ActionNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.favorite.Favorite;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAzureActionManager;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzComponent;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class TreeUtils {
    public static final Key<Pair<Object, Long>> HIGHLIGHTED_RESOURCE_KEY = Key.create("TreeHighlightedResource");
    public static final Key<List<AbstractAzResource<?, ?, ?>>> RESOURCES_TO_FOCUS_KEY = Key.create("ResourcesToFocus");
    public static final int NODE_PADDING = 12;
    public static final int INLINE_ACTION_ICON_WIDTH = 16;
    public static final int INLINE_ACTION_ICON_MARGIN = 4;
    public static final String KEY_SCROLL_PANE = "SCROLL_PANE";

    public static void installSelectionListener(@Nonnull JTree tree) {
        tree.addTreeSelectionListener(e -> {
            final Object n = tree.getLastSelectedPathComponent();
            Disposable selectionDisposable = (Disposable) tree.getClientProperty("SELECTION_DISPOSABLE");
            if (selectionDisposable != null) {
                Disposer.dispose(selectionDisposable);
            }
            if (n instanceof Tree.TreeNode) {
                final Tree.TreeNode<?> node = (Tree.TreeNode<?>) n;
                final String place = TreeUtils.getPlace(tree) + "." + (TreeUtils.underAppGroups(node) ? "app" : "type");
                final IActionGroup actions = node.inner.getActions();
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
                if (component instanceof Tree.TreeNode<?> treeNode) {
                    if (treeNode.getAllowsChildren() && treeNode.loaded == null) {
                        expandNode(treeNode);
                    }
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) {

            }

            @AzureOperation(name = "user/$resource.expand_node.resource", params = {"treeNode.inner.getValue()"}, source = "treeNode.inner.getValue()")
            private static void expandNode(final Tree.TreeNode<?> treeNode) {
                treeNode.inner.refreshChildrenLater();
            }
        };
        tree.addTreeWillExpandListener(listener);
    }

    public static void installMouseListener(@Nonnull JTree tree) {
        tree.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                final Tree.TreeNode<?> node = getTreeNodeAtMouse(tree, e);
                final boolean isMouseAtActionIcon = getHoverInlineActionIndex(tree, e, Optional.ofNullable(node)
                    .map(Tree.TreeNode::getInlineActionViews).map(List::size).orElse(0)) > -1;
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
                    clickNode(e, node);
                } else if (n instanceof Tree.LoadMoreNode && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    ((Tree.LoadMoreNode) n).load();
                }
                super.mouseClicked(e);
            }

            @AzureOperation(name = "user/$resource.click_node.resource", params = {"node.inner.getValue()"}, source = "node.inner.getValue()")
            private static void clickNode(final MouseEvent e, final Tree.TreeNode<?> node) {
                final JTree tree = node.tree;
                String place = TreeUtils.getPlace(tree) + "." + (TreeUtils.underAppGroups(node) ? "app" : "type");
                if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
                    final IActionGroup actions = node.inner.getActions();
                    if (Objects.nonNull(actions)) {
                        final ActionManager am = ActionManager.getInstance();
                        final IntellijAzureActionManager.ActionGroupWrapper group = toIntellijActionGroup(actions);
                        place += ".menu";
                        final ActionPopupMenu menu = am.createActionPopupMenu(place, group);
                        menu.setTargetComponent(tree);
                        final JPopupMenu popupMenu = menu.getComponent();
                        popupMenu.show(tree, e.getX(), e.getY());
                    }
                } else {
                    final DataContext context = DataManager.getInstance().getDataContext(tree);
                    place += ".node";
                    final AnActionEvent event = AnActionEvent.createFromAnAction(new EmptyAction(), e, place, context);
                    if (e.getClickCount() == 1) {
                        node.inner.click(event);
                    } else if (e.getClickCount() == 2) {
                        node.inner.doubleClick(event);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                final Tree.TreeNode<?> node = getTreeNodeAtMouse(tree, e);
                final List<IView.Label> inlineActionViews = Optional.ofNullable(node)
                    .map(Tree.TreeNode::getInlineActionViews).orElse(new ArrayList<>());
                final int inlineActionIndex = getHoverInlineActionIndex(tree, e, inlineActionViews.size());
                if (Objects.nonNull(node) && e.getClickCount() == 1 && inlineActionIndex > -1) {
                    final String place = TreeUtils.getPlace(tree) + "." + (TreeUtils.underAppGroups(node) ? "app" : "type") + ".inline";
                    final DataContext context = DataManager.getInstance().getDataContext(tree);
                    final AnActionEvent event = AnActionEvent.createFromAnAction(new EmptyAction(), e, place, context);
                    node.inner.triggerInlineAction(event, inlineActionIndex, TreeUtils.getPlace(tree));
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

    public static int getHoverInlineActionIndex(@Nonnull JTree tree, MouseEvent e, int actionCount) {
        final JBScrollPane scrollPane = (JBScrollPane) tree.getClientProperty(KEY_SCROLL_PANE);
        if (Objects.isNull(scrollPane)) {
            return -1;
        }
        final Rectangle viewRect = scrollPane.getViewport().getViewRect();
        // `viewRect.x` is the scrolled width, `viewRect.width` is the width of the visible view port.
        final int rightX = viewRect.x + viewRect.width - NODE_PADDING; // the `right` edge of the right action icon.
        final int iconBoxWidth = INLINE_ACTION_ICON_WIDTH + INLINE_ACTION_ICON_MARGIN;
        final int distance = rightX - e.getX();
        final int m = distance % iconBoxWidth;
        if (m < INLINE_ACTION_ICON_MARGIN / 2 || m > INLINE_ACTION_ICON_WIDTH + INLINE_ACTION_ICON_MARGIN / 2) {// hover at the margin area between icons
            return -1;
        }
        final int index = distance / iconBoxWidth;
        return index < actionCount ? index : -1;
    }

    public static IntellijAzureActionManager.ActionGroupWrapper toIntellijActionGroup(IActionGroup actions) {
        final ActionManager am = ActionManager.getInstance();
        if (actions instanceof IntellijAzureActionManager.ActionGroupWrapper) {
            return (IntellijAzureActionManager.ActionGroupWrapper) actions;
        }
        return new IntellijAzureActionManager.ActionGroupWrapper((ActionGroup) actions);
    }

    public static void renderLoadModeNode(JTree tree, @Nonnull Tree.LoadMoreNode node, boolean selected, @Nonnull SimpleColoredComponent renderer) {
        final SimpleTextAttributes attributes = SimpleTextAttributes.GRAY_ATTRIBUTES;
        renderer.append("more...", attributes);
        renderer.setToolTipText("double click to load more.");
    }

    public static void renderActionNode(JTree tree, @Nonnull Tree.TreeNode<?> node, boolean selected, @Nonnull SimpleColoredComponent renderer) {
        final ActionNode<?> inner = (ActionNode<?>) node.getInner();
        final SimpleTextAttributes attributes = SimpleTextAttributes.LINK_ATTRIBUTES;
        renderer.append(inner.getLabel(), attributes);
        renderer.setToolTipText(inner.buildDescription());
    }

    public static void renderMyTreeNode(JTree tree, @Nonnull Tree.TreeNode<?> node, boolean selected, @Nonnull SimpleColoredComponent renderer) {
        final Node.View view = node.getInner().getView();
        renderer.setIcon(Optional.ofNullable(view.getIcon()).map(IntelliJAzureIcons::getIcon).orElseGet(() -> IntelliJAzureIcons.getIcon(AzureIcons.Resources.GENERIC_RESOURCE)));
        final Object highlighted = tree.getClientProperty(HIGHLIGHTED_RESOURCE_KEY);
        final boolean toHighlightThisNode = Optional.ofNullable(highlighted).filter(h -> Objects.equals(node.getUserObject(), h)).isPresent();
        SimpleTextAttributes attributes = view.isEnabled() ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAY_ATTRIBUTES;
        if (selected && toHighlightThisNode) {
            attributes = attributes.derive(SimpleTextAttributes.STYLE_SEARCH_MATCH, JBColor.RED, JBColor.YELLOW, null);
        } else if (selected) {
            tree.putClientProperty(HIGHLIGHTED_RESOURCE_KEY, null);
        }
        renderer.append(view.getLabel(), attributes);
        renderer.append(Optional.ofNullable(view.getDescription()).filter(StringUtils::isNotBlank).map(d -> " " + d).orElse(""), SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES, true);
        renderer.setToolTipText(Optional.ofNullable(view.getTips()).filter(StringUtils::isNotBlank).orElseGet(view::getLabel));
    }

    public static boolean underAppGroups(@Nonnull DefaultMutableTreeNode node) {
        return underAppGroups(new TreePath(node.getPath()));
    }

    public static boolean underTypeGroups(@Nonnull DefaultMutableTreeNode node) {
        return underTypeGroups(new TreePath(node.getPath()));
    }

    public static boolean underAppGroups(@Nonnull TreePath path) {
        if (path.getPathCount() < 2) {
            return false;
        }
        final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getPathComponent(1);
        return treeNode.getUserObject() instanceof AzureResources;
    }

    public static boolean underTypeGroups(@Nonnull TreePath path) {
        if (path.getPathCount() < 2) {
            return false;
        }
        final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path.getPathComponent(1);
        return treeNode.getUserObject().equals("Azure");
    }

    public static void selectResourceNode(@Nonnull JTree tree, @Nonnull AzComponent resource) {
        final DefaultMutableTreeNode r = (DefaultMutableTreeNode) tree.getModel().getRoot();
        DefaultMutableTreeNode root = r.getChildCount() > 1 ? ((DefaultMutableTreeNode) r.getChildAt(1)) : null; // apps root
        if (resource instanceof Favorite) {
            resource = ((Favorite) resource).getResource();
            root = r.getChildCount() > 0 ? (DefaultMutableTreeNode) r.getFirstChild() : null; // favorite root
        } else if (resource instanceof AzService) {
            root = r.getChildCount() > 1 ? ((DefaultMutableTreeNode) r.getChildAt(2)) : null; // types root
        }
        if (Objects.nonNull(resource)) {
            selectResourceNode(tree, resource, root);
        }
    }

    public static void selectResourceNode(@Nonnull JTree tree, @Nonnull AzComponent resource, DefaultMutableTreeNode node) {
        tree.putClientProperty(HIGHLIGHTED_RESOURCE_KEY, resource);
        Optional.ofNullable(node).ifPresent(n -> TreeUtils.selectNode(tree, new NodeFinder() {
            @Override
            public boolean matches(final TreePath path) {
                return Objects.equals(((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject(), resource);
            }

            @Override
            public boolean contains(final TreePath path) {
                if (resource instanceof AzService) {
                    return false;
                }
                final Object current = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                final ResourceId resourceId = ResourceId.fromString(resource.getId() + "DUMMY");
                if (current instanceof AzService s && s.getName().equalsIgnoreCase(resourceId.providerNamespace())) {
                    return true;
                }
                // why append? consider resource `xxx/abc` and `xxx/abcd`
                return (current instanceof AzComponent c && StringUtils.containsIgnoreCase(resource.getId(), StringUtils.appendIfMissing(c.getId(), "/")));
            }
        }, new TreePath(n.getPath())));
    }

    public static String getPlace(@Nonnull JTree tree) {
        return StringUtils.firstNonBlank((String) tree.getClientProperty(Action.PLACE), Action.EMPTY_PLACE);
    }

    public interface NodeFinder {
        /**
         * @return true if node is the target node.
         */
        boolean matches(TreePath path);

        /**
         * @return true if node is parent of the target node.
         */
        boolean contains(TreePath path);
    }

    public static void selectNode(@Nonnull JTree tree, @Nonnull NodeFinder finder, @Nullable TreePath from) {
        final AtomicReference<TreeModelListener> listener = new AtomicReference<>();
        final AtomicReference<TreePath> checkpoint = new AtomicReference<>(from);
        listener.set(new TreeModelAdapter() {
            @Override
            protected void process(@NotNull final TreeModelEvent event, @NotNull final EventType type) {
                if (event.getTreePath().equals(checkpoint.get()) && type != EventType.NodesRemoved && type != EventType.NodesChanged) {
                    doSelectNode(tree, finder, checkpoint, listener.get());
                }
            }
        });
        tree.getModel().addTreeModelListener(listener.get());
        AzureTaskManager.getInstance().runLater(()-> doSelectNode(tree, finder, checkpoint, listener.get()));
    }

    private static void doSelectNode(final @Nonnull JTree tree, final @Nonnull NodeFinder matcher, final AtomicReference<TreePath> checkpoint, final TreeModelListener listener) {
        TreeUtil.promiseSelect(tree, new TreeVisitor() {
            @Override
            public @NotNull Action visit(@NotNull final TreePath path) {
                if (matcher.matches(path)) {
                    checkpoint.set(path);
                    return Action.INTERRUPT;
                }
                if (Objects.nonNull(checkpoint.get()) && path.isDescendant(checkpoint.get())) {
                    return Action.CONTINUE;
                }
                if ((Objects.isNull(checkpoint.get()) || checkpoint.get().isDescendant(path)) && matcher.contains(path)) {
                    checkpoint.set(path);
                    return Action.CONTINUE;
                }
                return Action.SKIP_CHILDREN;
            }
        }).onSuccess(path -> {
            tree.getModel().removeTreeModelListener(listener);
        }).onProcessed(path -> {
            if (Objects.nonNull(checkpoint.get())) {
                AzureTaskManager.getInstance().runLater(() -> TreeUtil.selectPath(tree, checkpoint.get(), true));
            }
        });
    }
}
