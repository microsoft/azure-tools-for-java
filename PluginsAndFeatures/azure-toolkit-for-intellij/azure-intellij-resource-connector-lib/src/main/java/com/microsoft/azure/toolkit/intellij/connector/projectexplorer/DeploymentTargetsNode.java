/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.projectexplorer;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.tree.LeafState;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.DeploymentTargetManager;
import com.microsoft.azure.toolkit.intellij.explorer.AzureExplorer;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor.REFRESH_MODULE_TARGETS;
import static com.microsoft.azure.toolkit.intellij.connector.projectexplorer.AzureFacetRootNode.ACTIONS_DEPLOY_TO_AZURE;

public class DeploymentTargetsNode extends AbstractAzureFacetNode<DeploymentTargetManager> {

    private final AzureEventBus.EventListener eventListener;
    private final Action<Object> editAction;

    public DeploymentTargetsNode(@Nonnull Project project, @Nonnull DeploymentTargetManager manager) {
        super(project, manager);
        this.eventListener = new AzureEventBus.EventListener(this::onEvent);
        AzureEventBus.on("connector.module_targets_changed", eventListener);
        this.editAction = new Action<>(Action.Id.of("user/connector.edit_targets_in_editor"))
            .withLabel("Open In Editor")
            .withIcon(AzureIcons.Action.EDIT.getIconPath())
            .withHandler(ignore -> AzureTaskManager.getInstance().runLater(() -> this.navigate(true)))
            .withAuthRequired(false);
    }

    private void onEvent(@Nonnull final AzureEvent azureEvent) {
        if (Objects.equals(azureEvent.getSource(), this.getValue())) {
            this.updateChildren();
        }
    }

    @Override
    @Nonnull
    public Collection<? extends AbstractAzureFacetNode<?>> buildChildren() {
        final List<? extends AbstractAzureFacetNode<?>> children = Optional.ofNullable(this.getValue()).stream()
            .flatMap(p -> p.getTargets().stream())
            .map(id -> Azure.az().getById(id))
            .filter(Objects::nonNull)
            .map(this::createResourceNode)
            .toList();
        if (CollectionUtils.isNotEmpty(children)) {
            return children;
        }
        final ArrayList<AbstractAzureFacetNode<?>> nodes = new ArrayList<>();
        nodes.add(new ActionNode<>(this.getProject(), Action.Id.of(ACTIONS_DEPLOY_TO_AZURE), this.getValue().getProfile().getModule().getModule()));
        return nodes;
    }

    @Override
    protected void buildView(@Nonnull final PresentationData presentation) {
        presentation.setIcon(AllIcons.Nodes.Deploy);
        presentation.setPresentableText("Deployment Targets");
        presentation.setTooltip("The Azure computing services that this module was deployed to.");
    }

    private AbstractAzureFacetNode<?> createResourceNode(@Nonnull final AbstractAzResource<?, ?, ?> app) {
        final Node<?> node = AzureExplorer.manager.createNode(app, null, IExplorerNodeProvider.ViewType.APP_CENTRIC);
        return new ResourceNode(this.getProject(), node, this);
    }

    @Nullable
    @Override
    public IActionGroup getActionGroup() {
        return new ActionGroup(Arrays.asList(
            REFRESH_MODULE_TARGETS,
            "---",
            editAction,
            "---",
            "Actions.DeployFunction",
            "Actions.DeploySpringCloud",
            "Actions.WebDeployAction"
        ), new Action.View("Deploy to Azure", AzureIcons.Action.DEPLOY.getIconPath(), true, null));
    }

    @Override
    public void navigate(boolean requestFocus) {
        Optional.ofNullable(getTargetsFile())
            .map(f -> PsiManager.getInstance(getProject()).findFile(f))
            .map(f -> NavigationUtil.openFileWithPsiElement(f, requestFocus, requestFocus));
    }

    @Override
    public boolean canNavigateToSource() {
        return Objects.nonNull(getTargetsFile());
    }

    @Nullable
    private VirtualFile getTargetsFile() {
        return Optional.ofNullable(getValue())
            .map(DeploymentTargetManager::getTargetsFile)
            .orElse(null);
    }

    @Override
    public boolean isAlwaysExpand() {
        return true;
    }

    @Override
    public int getWeight() {
        return DEFAULT_WEIGHT - 1;
    }

    @Override
    public void dispose() {
        super.dispose();
        AzureEventBus.off("connector.module_targets_changed", eventListener);
    }

    @Override
    public String toString() {
        return "Deployment Targets";
    }

    @Override
    public @Nonnull LeafState getLeafState() {
        return LeafState.NEVER;
    }
}