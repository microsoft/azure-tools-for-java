/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.projectexplorer;

import com.google.common.collect.Sets;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.util.ui.tree.TreeUtil;
import com.microsoft.azure.toolkit.lib.auth.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
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

@Slf4j
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public abstract class AbstractAzureFacetNode<T> extends AbstractTreeNode<T> implements IAzureFacetNode {
    public static final String FOCUS_KEY = "focuses";

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
                if (this.getParent() instanceof IAzureFacetNode) {
                    final IAzureFacetNode parent = (IAzureFacetNode) this.getParent();
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
            pane.updateFromRoot(true);
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
            final ArrayList<AbstractAzureFacetNode<?>> children = new ArrayList<>();
            children.add(toExceptionNode(e, project));
            return children;
        }
    }
}
