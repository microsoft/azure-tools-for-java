/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.DeploymentTargetManager;
import com.microsoft.azure.toolkit.intellij.explorer.AzureExplorer;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import java.util.*;

public class DeploymentTargetsNode extends AbstractAzureFacetNode<DeploymentTargetManager> {

    public DeploymentTargetsNode(@Nonnull Project project, @Nonnull DeploymentTargetManager manager) {
        super(project, manager);
    }

    @Override
    @Nonnull
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
        if (this.isDisposed()) {
            return Collections.emptyList();
        }
        try {
            return Optional.ofNullable(this.getValue()).stream()
                .flatMap(p -> p.getTargets().stream())
                .map(id -> Azure.az().getById(id))
                .filter(Objects::nonNull)
                .map(this::toNode)
                .toList();
        } catch (Throwable e) {
            e.printStackTrace();
            e = ExceptionUtils.getRootCause(e);
            final ArrayList<AbstractTreeNode<?>> children = new ArrayList<>();
            if (e instanceof AzureToolkitAuthenticationException) {
                final Action<Object> signin = AzureActionManager.getInstance().getAction(Action.AUTHENTICATE).bind(this.getValue()).withLabel("Sign in to manage the deployment targets.");
                children.add(new ActionNode<>(this.getProject(), signin));
            } else {
                children.add(new ExceptionNode(this.getProject(), e));
            }
            return children;
        }
    }

    private AbstractTreeNode<?> toNode(@Nonnull final AbstractAzResource<?, ?, ?> app) {
        final Node<?> node = AzureExplorer.manager.createNode(app, null, IExplorerNodeProvider.ViewType.APP_CENTRIC);
        return new ResourceNode(this.getProject(), node);
    }

    @Override
    protected void update(@Nonnull final PresentationData presentation) {
        presentation.setIcon(AllIcons.Nodes.Deploy);
        presentation.setPresentableText("Deployment Targets");
        presentation.setTooltip("The Azure services that this project was deployed to.");
    }

    @Override
    public int getWeight() {
        return DEFAULT_WEIGHT - 1;
    }

    @Override
    public String toString() {
        return "Deployment Targets";
    }
}