/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@Slf4j
public class EnvironmentVariablesNode extends AbstractAzureFacetNode<Connection<?, ?>> {
    @Nonnull
    private final Profile profile;
    private final Action<?> editAction;

    public EnvironmentVariablesNode(@Nonnull Project project, @Nonnull Profile profile, @Nonnull Connection<?, ?> connection) {
        super(project, connection);
        this.profile = profile;
        this.editAction = new Action<>(Action.Id.of("user/connector.edit_envs_in_editor"))
            .withLabel("Open In Editor")
            .withIcon(AzureIcons.Action.EDIT.getIconPath())
            .withHandler(ignore -> AzureTaskManager.getInstance().runLater(() -> this.navigate(true)))
            .withAuthRequired(false);
    }

    @Override
    @Nonnull
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
        final ArrayList<AbstractTreeNode<?>> children = new ArrayList<>();
        if (this.isDisposed()) {
            return children;
        }
        try {
            final Connection<?, ?> connection = this.getValue();
            final List<Pair<String, String>> generated = this.profile.getGeneratedEnvironmentVariables(connection);
            return generated.stream().map(g -> new EnvironmentVariableNode(this.getProject(), g, getValue())).toList();
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
            children.add(toExceptionNode(e));
        }
        return children;
    }

    @Override
    protected void update(@Nonnull final PresentationData presentation) {
        presentation.setIcon(IntelliJAzureIcons.getIcon(AzureIcons.Common.VARIABLE));
        presentation.setPresentableText("Environment Variables");
        presentation.setTooltip("Generated environment variables by connected resource.");
    }

    /**
     * get weight of the node.
     * weight is used for sorting, refer to {@link com.intellij.ide.util.treeView.AlphaComparator#compare(NodeDescriptor, NodeDescriptor)}
     */
    @Override
    public int getWeight() {
        return DEFAULT_WEIGHT + 1;
    }

    @Override
    @Nullable
    public Object getData(@Nonnull String dataId) {
        return StringUtils.equalsIgnoreCase(dataId, Action.SOURCE) ? this.getValue() : null;
    }

    @Nullable
    @Override
    public IActionGroup getActionGroup() {
        return new ActionGroup(
            editAction,
            "---",
            ResourceConnectionActionsContributor.COPY_ENV_VARS
        );
    }

    @Override
    public String toString() {
        return "Environment Variables";
    }

    @Override
    public void navigate(boolean requestFocus) {
        Optional.ofNullable(getDovEnvFile())
            .map(f -> PsiManager.getInstance(getProject()).findFile(f))
            .map(f -> NavigationUtil.openFileWithPsiElement(f, requestFocus, requestFocus));
    }

    @Override
    public boolean canNavigate() {
        return Objects.nonNull(getDovEnvFile());
    }

    @Override
    public boolean canNavigateToSource() {
        return Objects.nonNull(getDovEnvFile());
    }

    @Nullable
    private VirtualFile getDovEnvFile() {
        return Optional.ofNullable(getValue()).map(Connection::getProfile).map(Profile::getDotEnvFile).orElse(null);
    }
}