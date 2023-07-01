/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.SimpleTextAttributes;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.explorer.AzureExplorer;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor.EDIT_CONNECTION;
import static com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor.REMOVE_CONNECTION;

@Slf4j
public class ConnectionNode extends AbstractAzureFacetNode<Connection<?, ?>> {
    private final Action<?> editAction;
    private final AzureEventBus.EventListener eventListener;

    public ConnectionNode(@Nonnull final Project project, @Nonnull Connection<?, ?> connection) {
        super(project, connection);
        this.editAction = new Action<>(Action.Id.of("user/connector.edit_connection_in_editor"))
            .withLabel("Open In Editor")
            .withIcon(AzureIcons.Action.EDIT.getIconPath())
            .withHandler(ignore -> AzureTaskManager.getInstance().runLater(() -> this.navigate(true)))
            .withAuthRequired(false);
        this.eventListener = new AzureEventBus.EventListener(this::onEvent);
        AzureEventBus.on("resource.status_changed.resource", eventListener);
    }

    private void onEvent(@Nonnull final AzureEvent azureEvent) {
        final String type = azureEvent.getType();
        final Object source = azureEvent.getSource();
        final Resource<?> resource = this.getValue().getResource();
        if (resource instanceof AzureServiceResource &&
            source instanceof AzResource && StringUtils.equals(((AzResource) source).getId(), resource.getDataId())) {
            this.rerender(true);
        }
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
            if (!connection.isValidConnection()) {
                children.add(new ActionNode<>(this.getProject(), ResourceConnectionActionsContributor.FIX_CONNECTION, connection));
            }
            if (connection.getResource() instanceof AzureServiceResource) {
                children.add(getResourceNode(connection));
            }
            final Profile profile = connection.getProfile();
            final Boolean envFileExists = Optional.ofNullable(profile).map(Profile::getDotEnvFile).map(VirtualFile::exists).orElse(false);
            if (envFileExists) {
                children.add(new EnvironmentVariablesNode(this.getProject(), profile, connection));
            }
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
            children.add(toExceptionNode(e));
        }
        return children;
    }

    private AbstractTreeNode<?> getResourceNode(Connection<?, ?> connection) {
        try {
            final Object resource = connection.getResource().getData();
            if (Objects.isNull(resource)) {
                final ResourceId resourceId = ResourceId.fromString(connection.getResource().getDataId());
                return new GenericResourceNode(this.getProject(), resourceId, "Deleted");
            }
            final Node<?> node = AzureExplorer.manager.createNode(resource, null, IExplorerNodeProvider.ViewType.APP_CENTRIC);
            return new ResourceNode(this.getProject(), node);
        } catch (final Throwable e) {
            log.warn(e.getMessage(), e);
            return toExceptionNode(e);
        }
    }

    @Override
    protected void update(@Nonnull final PresentationData presentation) {
        try {
            final Connection<?, ?> connection = this.getValue();
            final Resource<?> resource = connection.getResource();
            final boolean isValid = connection.isValidConnection();
            final String icon = StringUtils.firstNonBlank(resource.getDefinition().getIcon(), AzureIcons.Common.AZURE.getIconPath());
            presentation.setIcon(IntelliJAzureIcons.getIcon(icon));
            presentation.addText(resource.getDefinition().getTitle(), AzureFacetRootNode.getTextAttributes(isValid));
            if (isValid) {
                presentation.addText(StringUtils.SPACE + resource.getName(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
            } else {
                presentation.setTooltip("Resource is missing, please edit the connection.");
            }
            if (resource.getDefinition().isEnvPrefixSupported()) {
                presentation.addText(" (" + connection.getEnvPrefix() + "_*)", SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    @Override
    public void onDoubleClicked(Object event) {
        final boolean isValid = getValue().validate(getProject());
        if (!isValid) {
            Optional.ofNullable(AzureActionManager.getInstance().getAction(EDIT_CONNECTION))
                .ifPresent(action -> action.handle(getValue(), event));
        }
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
            EDIT_CONNECTION,
            REMOVE_CONNECTION
        );
    }

    @Override
    public boolean isAlwaysExpand() {
        return true;
    }

    @Override
    public void navigate(boolean requestFocus) {
        final VirtualFile connectionsFile = this.getConnectionsFile();
        final PsiFile psiFile = Optional.ofNullable(connectionsFile)
            .map(f -> PsiManager.getInstance(getProject()).findFile(f)).orElse(null);
        if (Objects.isNull(psiFile)) {
            return;
        }
        NavigationUtil.openFileWithPsiElement(psiFile, requestFocus, requestFocus);
        EditorUtils.focusContentInCurrentEditor(getProject(), connectionsFile, getValue().getId());
    }

    @Override
    public boolean canNavigateToSource() {
        return Objects.nonNull(getConnectionsFile());
    }

    @Nullable
    private VirtualFile getConnectionsFile() {
        return Optional.ofNullable(getValue())
            .map(Connection::getProfile)
            .map(Profile::getConnectionManager)
            .map(ConnectionManager::getConnectionsFile)
            .orElse(null);
    }

    @Override
    public void dispose() {
        super.dispose();
        AzureEventBus.off("resource.status_changed.resource", eventListener);
    }

    @Override
    public String toString() {
        return "->" + this.getValue().getResource().getName();
    }
}