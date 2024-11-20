/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.projectexplorer;

import com.google.common.collect.Sets;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.util.ui.tree.TreeUtil;
import com.microsoft.azure.toolkit.intellij.common.component.TreeUtils;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.lib.auth.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzComponent;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@Slf4j
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public abstract class AbstractAzureFacetNode<T> extends AbstractTreeNode<T> implements IAzureFacetNode {
    public static final Integer DEFAULT_WEIGHT = 30;

    private final long createdTime;
    private long disposedTime;
    @Getter
    @Setter
    private boolean disposed;
    private final AtomicReference<Collection<? extends AbstractAzureFacetNode<?>>> children = new AtomicReference<>();

    protected AbstractAzureFacetNode(Project project, @Nonnull T value) {
        super(project, value);
        this.createdTime = System.currentTimeMillis();
    }

    public Collection<? extends AbstractAzureFacetNode<?>> getChildren() {
        if (this.isDisposed()) {
            return Collections.emptyList();
        }
        return this.rebuildChildren();
    }

    @Nonnull
    private Collection<? extends AbstractAzureFacetNode<?>> rebuildChildren() {
        final Collection<? extends AbstractAzureFacetNode<?>> newChildren = handleException(this::buildChildren, this.getProject());
        final HashSet<? extends AbstractAzureFacetNode<?>> newChildrenSet = new HashSet<>(newChildren);
        final HashSet<? extends AbstractAzureFacetNode<?>> oldChildrenSet = Optional.ofNullable(this.children.get()).map(HashSet::new).orElse(new HashSet<>());
        final Sets.SetView<? extends AbstractAzureFacetNode<?>> toRemove = Sets.difference(oldChildrenSet, newChildrenSet);
        final Sets.SetView<? extends AbstractAzureFacetNode<?>> toAdd = Sets.difference(newChildrenSet, oldChildrenSet);
        final Sets.SetView<? extends AbstractAzureFacetNode<?>> toKeep = Sets.intersection(oldChildrenSet, newChildrenSet);
        final Sets.SetView<AbstractAzureFacetNode<?>> result = Sets.union(toKeep, toAdd);
        toAdd.forEach(n -> Disposer.register(this, n));
        toRemove.forEach(Disposer::dispose);
        this.children.set(result);
        return this.children.get();
    }

    @Override
    protected void update(@Nonnull final PresentationData presentation) {
        try {
            this.buildView(presentation);
            if (Registry.is("ide.debugMode")) {
                presentation.addText(System.identityHashCode(this) + ":" + this.createdTime + ":" + this.disposedTime, SimpleTextAttributes.ERROR_ATTRIBUTES);
                presentation.addText("/", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                if (this.getParent() instanceof IAzureFacetNode parent) {
                    presentation.addText(System.identityHashCode(parent) + ":" + parent.isDisposed(), SimpleTextAttributes.ERROR_ATTRIBUTES);
                }
            }
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    @Override
    public void updateView() {
        rerender(false);
    }

    @Override
    public void updateChildren() {
        rerender(true);
    }

    private void rerender(boolean updateStructure) { // `static` to make it available for AzureFacetRootNode
        if (this.getProject().isDisposed()) {
            Disposer.dispose(this);
            return;
        }
        final AbstractProjectViewPane pane = ProjectView.getInstance(this.getProject()).getCurrentProjectViewPane();
        if (Objects.isNull(pane) || Objects.isNull(pane.getTree())) {
            Disposer.dispose(this);
            return;
        }
        final AsyncTreeModel model = (AsyncTreeModel) pane.getTree().getModel();
        final DefaultMutableTreeNode node = TreeUtil.findNodeWithObject((DefaultMutableTreeNode) model.getRoot(), this);
        if (Objects.nonNull(node)) {
            final TreePath path = TreeUtil.getPath((TreeNode) model.getRoot(), node);
            pane.updateFrom(path, false, updateStructure);
        }
    }

    @Nullable
    public JTree getTree() {
        final Project p = this.getProject();
        if (p.isDisposed()) return null;
        final AbstractProjectViewPane pane = ProjectView.getInstance(p).getCurrentProjectViewPane();
        return Objects.isNull(pane) ? null : pane.getTree();
    }

    @Nonnull
    private PresentationData buildView() {
        final PresentationData presentation = new PresentationData();
        this.buildView(presentation);
        return presentation;
    }

    @Nonnull
    protected abstract Collection<? extends AbstractAzureFacetNode<?>> buildChildren();

    protected abstract void buildView(PresentationData presentation);

    @Override
    public @Nullable Object getData(@Nonnull String dataId) {
        return StringUtils.equalsIgnoreCase(dataId, Action.SOURCE) ? this.getValue() : null;
    }

    public void dispose() {
        setDisposed(true);
        this.disposedTime = System.currentTimeMillis();
    }

    @ToString.Include
    @EqualsAndHashCode.Include
    public T getMyValue() {
        return this.getValue();
    }

    public AbstractAzureFacetNode<?> toExceptionNode(Throwable e, @Nonnull Project project) { // `static` to make it available for AzureFacetRootNode
        e = ExceptionUtils.getRootCause(e);
        if (e instanceof AzureToolkitAuthenticationException) {
            final Action<Object> signin = AzureActionManager.getInstance().getAction(Action.AUTHENTICATE).bind(project).withLabel("Sign in to manage connected resource...");
            return new ActionNode<>(project, signin);
        } else {
            return new ExceptionNode(project, e);
        }
    }

    private Collection<? extends AbstractAzureFacetNode<?>> handleException(Callable<Collection<? extends AbstractAzureFacetNode<?>>> t, Project project) { // `static` to make it available for AzureFacetRootNode
        try {
            return t.call();
        } catch (final Throwable e) {
            final Throwable cause = ExceptionUtils.getRootCause(e);
            if (cause instanceof ProcessCanceledException || cause instanceof InterruptedException) {
                return Collections.emptyList();
            }
            final ArrayList<AbstractAzureFacetNode<?>> children = new ArrayList<>();
            children.add(toExceptionNode(e, project));
            return children;
        }
    }

    public static void selectDeploymentResource(@Nonnull Module module, @Nonnull AbstractAzResource<?, ?, ?> app, final boolean requestFocus) {
        final String rId = app.getId();
        selectResource(module, rId, node -> node instanceof AzureFacetRootNode
            || node instanceof DeploymentTargetsNode
            || node instanceof ResourceNode rn && isAncestor(rn.getValue().getValue(), rId), requestFocus);
    }

    public static void selectConnectedResource(@Nonnull Connection<?, ?> connection, final boolean requestFocus) {
        selectConnectedResource(connection, null, requestFocus);
    }

    public static void selectConnectedResource(@Nonnull Connection<?, ?> connection, @Nullable String resourceId, final boolean requestFocus) {
        final Module module = connection.getProfile().getModule().getModule();
        final String rId = Optional.ofNullable(resourceId).filter(StringUtils::isNotBlank).orElseGet(() -> connection.getResource().getDataId());
        selectResource(module, rId, node -> node instanceof AzureFacetRootNode
            || node instanceof ConnectionsNode
            || node instanceof ConnectionNode cn && Objects.equals(cn.getValue(), connection)
            || node instanceof ResourceNode rn && isAncestor(rn.getValue().getValue(), rId), requestFocus);
    }

    private static void selectResource(final Module module, final String rId, Predicate<Object> isContainerNode, final boolean requestFocus) {
        final JTree tree = Optional.ofNullable(ProjectView.getInstance(module.getProject()).getCurrentProjectViewPane())
            .map(AbstractProjectViewPane::getTree).orElse(null);
        if (Objects.isNull(tree)) {
            return;
        }
        Optional.ofNullable(ToolWindowManager.getInstance(module.getProject()).getToolWindow("Project")).ifPresent(w -> w.activate(() -> AzureTaskManager.getInstance().runOnPooledThread(() -> {
            final DefaultMutableTreeNode moduleRoot = TreeUtil.findNode((DefaultMutableTreeNode) tree.getModel().getRoot(), node ->
                node.getUserObject() instanceof PsiDirectoryNode n
                    && Objects.equals(ModuleUtil.findModuleForFile(n.getValue().getVirtualFile(), module.getProject()), module));
            final TreePath from = Optional.ofNullable(moduleRoot).map(r -> new TreePath(r.getPath())).orElse(null);
            TreeUtils.selectNode(tree, new TreeUtils.NodeFinder() {
                @Override
                public boolean matches(final TreePath path) {
                    final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    return node.getUserObject() instanceof ResourceNode rn
                        && rn.getValue().getValue() instanceof AzResource r
                        && r.getId().equalsIgnoreCase(rId);
                }

                @Override
                public boolean contains(final TreePath path) {
                    final Object node = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                    return isContainerNode.test(node);
                }
            }, from);
        }), requestFocus));
    }

    private static boolean isAncestor(Object r, final String target) {
        // why append? consider resource `xxx/abc` and `xxx/abcd`
        return r instanceof AzComponent azr && StringUtils.containsIgnoreCase(target, StringUtils.appendIfMissing(azr.getId(), "/"));
    }
}
