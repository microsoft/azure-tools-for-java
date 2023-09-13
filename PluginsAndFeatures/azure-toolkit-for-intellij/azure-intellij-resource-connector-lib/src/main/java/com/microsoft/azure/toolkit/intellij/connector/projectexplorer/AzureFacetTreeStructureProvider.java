/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.projectexplorer;

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
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.tree.TreeUtil;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAzureActionManager;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.intellij.ui.AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED;

@Slf4j
public final class AzureFacetTreeStructureProvider implements TreeStructureProvider {
    private final Project myProject;
    private final Map<Module, AzureFacetRootNode> azureNodes = new ConcurrentHashMap<>();

    public AzureFacetTreeStructureProvider(Project project) {
        myProject = project;
        final AbstractProjectViewPane currentProjectViewPane = ProjectView.getInstance(project).getCurrentProjectViewPane();
        Optional.ofNullable(currentProjectViewPane)
                .map(AbstractProjectViewPane::getTree)
                .ifPresent(tree -> tree.putClientProperty(ANIMATION_IN_RENDERER_ALLOWED, true));
    }

    @Override
    @Nonnull
    public Collection<AbstractTreeNode<?>> modify(@Nonnull AbstractTreeNode<?> parent, @Nonnull Collection<AbstractTreeNode<?>> children, ViewSettings settings) {
        if (!(parent instanceof PsiDirectoryNode) || this.myProject.isDisposed()) {
            return children;
        }
        try {
            final AzureModule azureModule = Optional.ofNullable(toModule(parent)).map(AzureModule::from).orElse(null);
            final Boolean state = Optional.ofNullable(azureModule).map(AzureModule::getAzureFacetState).orElse(null);
            final boolean forceShow = BooleanUtils.isTrue(state);
            final boolean forceHide = BooleanUtils.isFalse(state);
            final boolean defaultShow = state == null && Objects.nonNull(azureModule) && (azureModule.hasAzureFacet() || azureModule.isInitialized() || azureModule.hasAzureDependencies());
            if (!forceHide && (forceShow || defaultShow)) {
                addListener(parent.getProject());
                final AbstractTreeNode<?> dotAzureDir = children.stream()
                    .filter(n -> n instanceof PsiDirectoryNode)
                    .map(n -> ((PsiDirectoryNode) n))
                    .filter(d -> Objects.nonNull(d.getVirtualFile()) && ".azure".equalsIgnoreCase(d.getVirtualFile().getName()))
                    .findAny().orElse(null);
                final List<AbstractTreeNode<?>> nodes = new LinkedList<>(children);
                nodes.removeIf(n -> Objects.equals(n, dotAzureDir));
                final AzureFacetRootNode azureNode = this.azureNodes.computeIfAbsent(azureModule.getModule(), m -> {
                    final AzureFacetRootNode node = new AzureFacetRootNode(azureModule, settings);
                    Disposer.register(ProjectView.getInstance(this.myProject).getCurrentProjectViewPane(), node);
                    return node;
                });
                nodes.add(azureNode);
                return nodes;
            } else {
                children.removeIf(c -> c instanceof AzureFacetRootNode);
            }
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }
        return children;
    }

    /**
     * convert to {@code Module} is the {@param node} is a module dir.
     */
    @Nullable
    private Module toModule(final AbstractTreeNode<?> node) {
        if (node instanceof PsiDirectoryNode) {
            final VirtualFile file = ((PsiDirectoryNode) node).getValue().getVirtualFile();
            final Module module = ModuleUtil.findModuleForFile(file, myProject);
            if (Objects.nonNull(module) && Objects.equals(ProjectUtil.guessModuleDir(module), file)) {
                return module;
            }
        }
        return null;
    }

    @RequiredArgsConstructor
    static class AzureProjectExplorerMouseListener extends MouseAdapter {
        private final JTree tree;

        private IAzureFacetNode currentNode;
        private List<AnAction> backupActions;

        @Override
        public void mousePressed(MouseEvent e) {
            final AbstractTreeNode<?> currentTreeNode = getCurrentTreeNode(e);
            if (SwingUtilities.isLeftMouseButton(e) && currentTreeNode instanceof IAzureFacetNode node) {
                final DataContext context = DataManager.getInstance().getDataContext(tree);
                final AnActionEvent event = AnActionEvent.createFromAnAction(new EmptyAction(), e, ActionPlaces.PROJECT_VIEW_POPUP + ".click", context);
                if (e.getClickCount() == 1) {
                    node.onClicked(event);
                } else if (e.getClickCount() == 2) {
                    node.onDoubleClicked(event);
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
            if (!(node instanceof IAzureFacetNode newNode)) {
                if (Objects.nonNull(currentNode)) {
                    // clean up popup menu actions
                    resetPopupMenuActions();
                }
                return;
            }
            if (!Objects.equals(newNode, currentNode)) {
                // update popup menu actions for new node
                updatePopupMenuActions(newNode);
                currentNode = newNode;
            }
        }

        private AbstractTreeNode<?> getCurrentTreeNode(MouseEvent e) {
            final TreePath path = tree.getClosestPathForLocation(e.getX(), e.getY());
            return TreeUtil.getAbstractTreeNode(path);
        }

        private void resetPopupMenuActions() {
            final ActionManager manager = ActionManager.getInstance();
            final DefaultActionGroup popupMenu = (DefaultActionGroup) manager.getAction("ProjectViewPopupMenu");
            if (CollectionUtils.isNotEmpty(backupActions)) {
                popupMenu.removeAll();
                this.backupActions.forEach(popupMenu::add);
            }
            this.currentNode = null;
        }

        private void updatePopupMenuActions(final IAzureFacetNode node) {
            final ActionManager manager = ActionManager.getInstance();
            final DefaultActionGroup popupMenu = (DefaultActionGroup) manager.getAction("ProjectViewPopupMenu");
            if (this.currentNode == null && CollectionUtils.isEmpty(backupActions)) {
                this.backupActions = Arrays.stream(popupMenu.getChildren(null)).collect(Collectors.toList());
            }
            final List<Object> actions = Optional.ofNullable(node.getActionGroup()).map(IActionGroup::getActions).orElse(Collections.emptyList());
            final IntellijAzureActionManager.ActionGroupWrapper wrapper = new IntellijAzureActionManager.ActionGroupWrapper(new ActionGroup(actions));
            popupMenu.removeAll();
            Arrays.stream(wrapper.getChildren(null)).forEach(popupMenu::add);
        }
    }

    private void addListener(@Nonnull final Project project) {
        if (project.isDisposed()) {
            return;
        }
        final AbstractProjectViewPane currentProjectViewPane = ProjectView.getInstance(project).getCurrentProjectViewPane();
        final JTree tree = Optional.of(currentProjectViewPane).map(AbstractProjectViewPane::getTree).orElse(null);
        if (Objects.nonNull(tree) && Arrays.stream(tree.getMouseListeners()).noneMatch(listener -> listener instanceof AzureProjectExplorerMouseListener)) {
            final MouseListener[] mouseListeners = tree.getMouseListeners();
            Arrays.stream(mouseListeners).forEach(tree::removeMouseListener);
            tree.addMouseListener(new AzureProjectExplorerMouseListener(tree));
            Arrays.stream(mouseListeners).forEach(tree::addMouseListener);
        }
    }

    @Override
    public Object getData(@NotNull Collection<AbstractTreeNode<?>> selected, @NotNull String dataId) {
        final IAzureFacetNode azureFacetNode = selected.stream()
                .filter(node -> node instanceof IAzureFacetNode)
                .map(n -> (IAzureFacetNode) n).findFirst().orElse(null);
        return Objects.nonNull(azureFacetNode) ? azureFacetNode.getData(dataId) : TreeStructureProvider.super.getData(selected, dataId);
    }
}


