/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.explorer;

import com.intellij.ide.DataManager;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.tree.TreeUtil;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.explorer.node.AzureModuleRootNode;
import com.microsoft.azure.toolkit.intellij.connector.explorer.node.IAzureProjectExplorerNode;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;
import java.util.stream.Collectors;

public class ResourceConnectorStructureProvider implements TreeStructureProvider {

    @Override
    @Nonnull
    public Collection<AbstractTreeNode<?>> modify(@Nonnull AbstractTreeNode<?> parent, @Nonnull Collection<AbstractTreeNode<?>> children, ViewSettings settings) {
        final AzureModule azureModule = getIfAzureModule(parent);
        if (Objects.nonNull(azureModule)) {
            addListener(parent.getProject());
            final List<AbstractTreeNode<?>> nodes = new LinkedList<>();
            boolean containsAzureDir = false;
            for (final AbstractTreeNode<?> child : children) {
                boolean isDotAzureDir = Optional.ofNullable(child)
                        .filter(c -> c instanceof PsiDirectoryNode)
                        .map(c -> ((PsiDirectoryNode) c).getVirtualFile())
                        .filter(f -> ".azure".equalsIgnoreCase(f.getName()))
                        .isPresent();
                if (!isDotAzureDir) {
                    nodes.add(child);
                } else {
                    containsAzureDir = true;
                }
            }
            if (containsAzureDir) {
                nodes.add(0, new AzureModuleRootNode(azureModule, settings));
            }
            return nodes;
        }
        return children;
    }

    @Nullable
    private AzureModule getIfAzureModule(final AbstractTreeNode<?> parent) {
        if (parent instanceof PsiDirectoryNode) {
            final VirtualFile file = ((PsiDirectoryNode) parent).getValue().getVirtualFile();
            final Module module = ModuleUtil.findModuleForFile(file, parent.getProject());
            if (Objects.nonNull(module) && Objects.equals(ProjectUtil.guessModuleDir(module), file)) {
                final AzureModule azureModule = AzureModule.from(module);
                if (azureModule.isInitialized()) {
                    return azureModule;
                }
            }
        }
        return null;
    }

    @RequiredArgsConstructor
    static class AzureProjectExplorerMouseListener extends MouseAdapter {
        private static final Separator SEPARATOR = new Separator();
        private final JTree tree;
        private final Project project;

        private IAzureProjectExplorerNode currentNode;
        private List<AnAction> backupActions;

        @Override
        public void mousePressed(MouseEvent e) {
            final AbstractTreeNode<?> currentTreeNode = getCurrentTreeNode(e);
            if (SwingUtilities.isLeftMouseButton(e) && currentTreeNode instanceof IAzureProjectExplorerNode) {
                final IAzureProjectExplorerNode node = (IAzureProjectExplorerNode) currentTreeNode;
                final DataContext context = DataManager.getInstance().getDataContext(tree);
                final AnActionEvent event = AnActionEvent.createFromAnAction(new EmptyAction(), e, "explorer.connector", context);
                if (e.getClickCount() == 1) {
                    node.triggerClickAction(event);
                } else {
                    node.triggerClickAction(event);
                }
            } else {
                modifyPopupActions(e);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            // clean up node actions
            if (Objects.nonNull(currentNode)) {
                // clean up popup menu actions
                currentNode = null;
                resetPopupMenuActions();
            }
        }

        private void modifyPopupActions(MouseEvent e) {
            final AbstractTreeNode<?> node = getCurrentTreeNode(e);
            if (!(node instanceof IAzureProjectExplorerNode)) {
                if (Objects.nonNull(currentNode)) {
                    // clean up popup menu actions
                    resetPopupMenuActions();
                }
                return;
            }
            final IAzureProjectExplorerNode newNode = (IAzureProjectExplorerNode) node;
            if (!Objects.equals(newNode, currentNode)) {
                // update popup menu actions for new node
                updatePopupMenuActions(newNode);
                currentNode = newNode;
            }
        }

        private AbstractTreeNode<?> getCurrentTreeNode(MouseEvent e) {
            final int rowForLocation = tree.getRowForLocation(e.getX(), e.getY());
            final TreePath pathForRow = tree.getPathForRow(rowForLocation);
            return TreeUtil.getAbstractTreeNode(pathForRow);
        }

        private void resetPopupMenuActions() {
            final ActionManager manager = ActionManager.getInstance();
            final DefaultActionGroup popupMenu = (DefaultActionGroup) manager.getAction("ProjectViewPopupMenu");
            if (CollectionUtils.isNotEmpty(backupActions)) {
                popupMenu.removeAll();
                this.backupActions.forEach(popupMenu::add);
            }
            this.currentNode = null;
//            final IActionGroup group = IntellijAzureActionManager.getInstance().getGroup(groupId);
//            // clean up seperator added by toolkits
//            while (ArrayUtils.contains(popupMenu.getChildActionsOrStubs(), seperator)) {
//                popupMenu.remove(seperator);
//            }
//            group.getActions().stream().filter(action -> action instanceof Action.Id)
//                    .map(id -> ((Action.Id<?>) id).getId())
//                    .forEach(id -> Optional.ofNullable(manager.getAction(id)).ifPresent(popupMenu::remove));
        }

        private void updatePopupMenuActions(final IAzureProjectExplorerNode node) {
            final ActionManager manager = ActionManager.getInstance();
            final DefaultActionGroup popupMenu = (DefaultActionGroup) manager.getAction("ProjectViewPopupMenu");
            if (this.currentNode == null && CollectionUtils.isEmpty(backupActions)) {
                this.backupActions = Arrays.stream(popupMenu.getChildren(null)).collect(Collectors.toList());
            }
            final IActionGroup actionGroup = node.getActionGroup();
            if (Objects.nonNull(actionGroup)) {
                popupMenu.removeAll(); // clean up default actions
                actionGroup.getActions().stream()
                        .map(action -> action instanceof Action.Id ? manager.getAction(((Action.Id<?>) action).getId()) : SEPARATOR)
                        .forEach(action -> popupMenu.add(action, Constraints.LAST));
            }
        }
    }

    private void addListener(@NotNull final Project project) {
        final AbstractProjectViewPane currentProjectViewPane = ProjectView.getInstance(project).getCurrentProjectViewPane();
        final JTree tree = currentProjectViewPane.getTree();
        final boolean exists = Arrays.stream(tree.getMouseListeners()).anyMatch(listener -> listener instanceof AzureProjectExplorerMouseListener);
        if (!exists) {
            final MouseListener[] mouseListeners = tree.getMouseListeners();
            Arrays.stream(mouseListeners).forEach(tree::removeMouseListener);
            tree.addMouseListener(new AzureProjectExplorerMouseListener(tree, project));
            Arrays.stream(mouseListeners).forEach(tree::addMouseListener);
        }
    }
}
