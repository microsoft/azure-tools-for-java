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

package com.microsoft.intellij.components;

import com.google.common.collect.ImmutableList;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.microsoft.azure.arcadia.serverexplore.ArcadiaSparkClusterRootModuleImpl;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkClusterRootModuleImpl;
import com.microsoft.azure.hdinsight.common.HDInsightUtil;
import com.microsoft.azure.sqlbigdata.serverexplore.SqlBigDataClusterModule;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.azuretools.ijidea.actions.SelectSubscriptionsAction;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.helpers.UIHelperImpl;
import com.microsoft.intellij.serviceexplorer.azure.AzureModuleImpl;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.collections.ListChangeListener;
import com.microsoft.tooling.msservices.helpers.collections.ListChangedEvent;
import com.microsoft.tooling.msservices.helpers.collections.ObservableList;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.*;

public class ServerExplorerToolWindowFactory implements ToolWindowFactory, PropertyChangeListener {
    public static final String EXPLORER_WINDOW = "Azure Explorer";

    private final Map<Project, DefaultTreeModel> treeModelMap = new HashMap<>();

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
        // initialize azure service module
        AzureModule azureModule = new AzureModuleImpl(project);
        HDInsightUtil.setHDInsightRootModule(azureModule);
        azureModule.setSparkServerlessModule(new CosmosSparkClusterRootModuleImpl(azureModule));
        azureModule.setArcadiaModule(new ArcadiaSparkClusterRootModuleImpl(azureModule));
        // initialize aris service module
        SqlBigDataClusterModule arisModule = new SqlBigDataClusterModule(project);

        // initialize with all the service modules
        DefaultTreeModel treeModel = new DefaultTreeModel(initRoot(project, ImmutableList.of(azureModule, arisModule)));
        treeModelMap.put(project, treeModel);

        // initialize tree
        final JTree tree = new Tree(treeModel);
        tree.setRootVisible(false);
        tree.setCellRenderer(new NodeTreeCellRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // add a click handler for the tree
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    treeNodeDblClicked(e, tree, project);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                treeMousePressed(e, tree);
            }
        });
        // add keyboard handler for the tree
        tree.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {
                TreePath treePath = tree.getAnchorSelectionPath();
                if (treePath == null) {
                    return;
                }

                SortableTreeNode treeNode = (SortableTreeNode) treePath.getLastPathComponent();
                Node node = (Node) treeNode.getUserObject();

                Rectangle rectangle = tree.getRowBounds(tree.getRowForPath(treePath));
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (!node.isLoading()) {
                        node.getClickAction().fireNodeActionEvent();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU) {
                    if (node.hasNodeActions()) {
                        JPopupMenu menu = createPopupMenuForNode(node);
                        menu.show(e.getComponent(), (int) rectangle.getX(), (int) rectangle.getY());
                    }
                }
            }
        });
        // add the tree to the window
        toolWindow.getComponent().add(new JBScrollPane(tree));

        // set tree and tree path to expand the node later
        azureModule.setTree(tree);
        azureModule.setTreePath(tree.getPathForRow(0));

        // setup toolbar icons
        addToolbarItems(toolWindow, project, azureModule);

    }

    private SortableTreeNode initRoot(Project project, List<RefreshableNode> nodes) {
        SortableTreeNode root = new SortableTreeNode();

        nodes.forEach(node -> {
            root.add(createTreeNode(node, project));
            // kick-off asynchronous load of child nodes on all the modules
            node.load(false);
        });

        return root;
    }

    private void treeNodeDblClicked(MouseEvent e, JTree tree, final Project project) {
        final TreePath treePath = tree.getPathForLocation(e.getX(), e.getY());
        if (treePath == null) {
            return;
        }
        final Node node = getTreeNodeOnMouseClick(tree, treePath);
        if (!node.isLoading()) {
            node.onNodeDblClicked(project);
        }
    }

    private void treeMousePressed(MouseEvent e, JTree tree) {
        // delegate click to the node's click action if this is a left button click
        if (SwingUtilities.isLeftMouseButton(e)) {
            TreePath treePath = tree.getPathForLocation(e.getX(), e.getY());
            if (treePath == null) {
                return;
            }
            // get the tree node associated with left mouse click
            Node node = getTreeNodeOnMouseClick(tree, treePath);
            // if the node in question is in a "loading" state then we
            // do not propagate the click event to it
            if (!node.isLoading()) {
                node.getClickAction().fireNodeActionEvent();
            }
            // for right click show the context menu populated with all the
            // actions from the node
        } else if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
            TreePath treePath = tree.getClosestPathForLocation(e.getX(), e.getY());
            if (treePath == null) {
                return;
            }
            // get the tree node associated with right mouse click
            Node node = getTreeNodeOnMouseClick(tree, treePath);
            if (node.hasNodeActions()) {
                // select the node which was right-clicked
                tree.getSelectionModel().setSelectionPath(treePath);

                JPopupMenu menu = createPopupMenuForNode(node);
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private Node getTreeNodeOnMouseClick(JTree tree, TreePath treePath) {
        SortableTreeNode treeNode = (SortableTreeNode) treePath.getLastPathComponent();
        Node node = (Node) treeNode.getUserObject();
        // set tree and tree path to expand the node later
        node.setTree(tree);
        node.setTreePath(treePath);
        return node;
    }

    private JPopupMenu createPopupMenuForNode(Node node) {
        JPopupMenu menu = new JPopupMenu();

        for (final NodeAction nodeAction : node.getNodeActions()) {
            if (!nodeAction.isEnabled()) {
                continue;
            }
            JMenuItem menuItem = new JMenuItem(nodeAction.getName());
            menuItem.setEnabled(nodeAction.isEnabled());
            if (nodeAction.getIconPath() != null) {
                menuItem.setIcon(UIHelperImpl.loadIcon(nodeAction.getIconPath()));
            }
            // delegate the menu item click to the node action's listeners
            menuItem.addActionListener(e -> nodeAction.fireNodeActionEvent());

            menu.add(menuItem);
        }

        return menu;
    }

    private SortableTreeNode createTreeNode(Node node, Project project) {
        SortableTreeNode treeNode = new SortableTreeNode(node, true);

        // associate the DefaultMutableTreeNode with the Node via it's "viewData"
        // property; this allows us to quickly retrieve the DefaultMutableTreeNode
        // object associated with a Node
        node.setViewData(treeNode);

        // listen for property change events on the node
        node.addPropertyChangeListener(this);

        // listen for structure changes on the node, i.e. when child nodes are
        // added or removed
        node.getChildNodes().addChangeListener(new NodeListChangeListener(treeNode, project));

        // create child tree nodes for each child node
        if (node.hasChildNodes()) {
            for (Node childNode : node.getChildNodes()) {
                treeNode.add(createTreeNode(childNode, project));
            }
        }

        return treeNode;
    }

    private void removeEventHandlers(Node node) {
        node.removePropertyChangeListener(this);

        ObservableList<Node> childNodes = node.getChildNodes();
        childNodes.removeAllChangeListeners();

        if (node.hasChildNodes()) {
            // this remove call should cause the NodeListChangeListener object
            // registered on it's child nodes to fire which should recursively
            // clean up event handlers on it's children
            node.removeAllChildNodes();
        }
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        // if we are not running on the dispatch thread then switch
        // to dispatch thread
        if (!ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().invokeAndWait(() -> propertyChange(evt), ModalityState.any());

            return;
        }

        // this event is fired whenever a property on a node in the
        // model changes; we respond by triggering a node change
        // event in the tree's model
        Node node = (Node) evt.getSource();

        // the treeModel object can be null before it is initialized
        // from createToolWindowContent; we ignore property change
        // notifications till we have a valid model object
        DefaultTreeModel treeModel = treeModelMap.get(node.getProject());
        if (treeModel != null) {
            treeModel.nodeChanged((TreeNode) node.getViewData());
        }
    }

    private class NodeListChangeListener implements ListChangeListener {
        private final SortableTreeNode treeNode;
        private final Project project;

        NodeListChangeListener(SortableTreeNode treeNode, Project project) {
            this.treeNode = treeNode;
            this.project = project;
        }

        @Override
        public void listChanged(final ListChangedEvent e) {
            // if we are not running on the dispatch thread then switch
            // to dispatch thread
            if (!ApplicationManager.getApplication().isDispatchThread()) {
                ApplicationManager.getApplication().invokeAndWait(() -> listChanged(e), ModalityState.any());

                return;
            }

            switch (e.getAction()) {
                case add:
                    // create child tree nodes for the new nodes
                    for (Node childNode : (Collection<Node>) e.getNewItems()) {
                        treeNode.add(createTreeNode(childNode, project));
                    }
                    break;
                case remove:
                    // unregistered all event handlers recursively and remove
                    // child nodes from the tree
                    for (Node childNode : (Collection<Node>) e.getOldItems()) {
                        removeEventHandlers(childNode);

                        // remove this node from the tree
                        treeNode.remove((MutableTreeNode) childNode.getViewData());
                    }
                    break;
                default:
                    break;
            }
            if (treeModelMap.get(project) != null) {
                treeModelMap.get(project).reload(treeNode);
            }
        }
    }

    private class NodeTreeCellRenderer extends NodeRenderer {
        @Override
        protected void doPaint(Graphics2D g) {
            super.doPaint(g);
            setOpaque(false);
        }

        @Override
        public void customizeCellRenderer(@NotNull JTree jtree,
                                          Object value,
                                          boolean selected,
                                          boolean expanded,
                                          boolean isLeaf,
                                          int row,
                                          boolean focused) {
            super.customizeCellRenderer(jtree, value, selected, expanded, isLeaf, row, focused);

            // if the node has an icon set then we use that
            SortableTreeNode treeNode = (SortableTreeNode) value;
            Node node = (Node) treeNode.getUserObject();

            // "node" can be null if it's the root node which we keep hidden to simulate
            // a multi-root tree control
            if (node == null) {
                return;
            }

            final Icon icon = node.getIcon();
            final String iconPath = node.getIconPath();
            if (Objects.nonNull(icon)) {
                setIcon(icon);
            } else if (iconPath != null && !iconPath.isEmpty()) {
                setIcon(UIHelperImpl.loadIcon(iconPath));
            }

            // setup a tooltip
            setToolTipText(node.getToolTip());
        }
    }

    private void addToolbarItems(ToolWindow toolWindow, final Project project, final AzureModule azureModule) {
        if (toolWindow instanceof ToolWindowEx) {
            ToolWindowEx toolWindowEx = (ToolWindowEx) toolWindow;
            try {
                Runnable forceRefreshTitleActions = () -> {
                    try {
                        toolWindowEx.setTitleActions(
                            new AnAction("Refresh", "Refresh Azure Nodes List", null) {
                                @Override
                                public void actionPerformed(AnActionEvent event) {
                                    azureModule.load(true);
                                }

                                @Override
                                public void update(AnActionEvent e) {
                                    boolean isDarkTheme = DefaultLoader.getUIHelper().isDarkTheme();
                                    final String iconPath = isDarkTheme ? RefreshableNode.REFRESH_ICON_DARK : RefreshableNode.REFRESH_ICON_LIGHT;
                                    e.getPresentation().setIcon(UIHelperImpl.loadIcon(iconPath));
                                }
                            },
                            new AzureSignInAction(),
                            new SelectSubscriptionsAction());
                    } catch (Exception e) {
                        AzurePlugin.log(e.getMessage(), e);
                    }
                };
                AuthMethodManager.getInstance().addSignInEventListener(forceRefreshTitleActions);
                AuthMethodManager.getInstance().addSignOutEventListener(forceRefreshTitleActions);
                // Remove the sign in/out listener when project close
                ProjectManager.getInstance().addProjectManagerListener(project, new ProjectManagerListener() {
                    @Override
                    public void projectClosing(@NotNull Project project) {
                        AuthMethodManager.getInstance().removeSignInEventListener(forceRefreshTitleActions);
                        AuthMethodManager.getInstance().removeSignOutEventListener(forceRefreshTitleActions);
                    }
                });

                forceRefreshTitleActions.run();
            } catch (Exception e) {
                AzurePlugin.log(e.getMessage(), e);
            }
        }
    }
}
