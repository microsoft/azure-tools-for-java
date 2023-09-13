/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.projectexplorer;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.tree.LeafState;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor.REFRESH_ENVIRONMENT_VARIABLES;

@Slf4j
public class EnvironmentVariablesNode extends AbstractAzureFacetNode<Connection<?, ?>> {
    private final Action<?> editAction;
    private final AzureEventBus.EventListener eventListener;

    public EnvironmentVariablesNode(@Nonnull Project project, @Nonnull Connection<?, ?> connection) {
        super(project, connection);
        this.eventListener = new AzureEventBus.EventListener(this::onEvent);
        AzureEventBus.on("connector.connection_environment_variables_changed", eventListener);
        this.editAction = new Action<>(Action.Id.of("user/connector.edit_envs_in_editor"))
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
        final Connection<?, ?> connection = this.getValue();
        final List<Pair<String, String>> generated = connection.getProfile().getGeneratedEnvironmentVariables(connection);
        return generated.stream().map(g -> new EnvironmentVariableNode(this.getProject(), g, getValue())).collect(Collectors.toList());
    }

    @Override
    protected void buildView(@Nonnull final PresentationData presentation) {
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
            REFRESH_ENVIRONMENT_VARIABLES,
            "---",
            editAction,
            ResourceConnectionActionsContributor.COPY_ENV_VARS
        );
    }

    @Override
    public void dispose() {
        super.dispose();
        AzureEventBus.off("connector.connection_environment_variables_changed", eventListener);
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

    @Override
    public @Nonnull LeafState getLeafState() {
        return LeafState.NEVER;
    }
}