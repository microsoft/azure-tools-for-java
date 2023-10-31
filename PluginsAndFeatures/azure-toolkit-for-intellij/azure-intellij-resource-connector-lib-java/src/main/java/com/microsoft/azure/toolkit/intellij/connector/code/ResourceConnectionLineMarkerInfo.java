/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.code;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.MergeableLineMarkerInfo;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.psi.PsiElement;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.projectexplorer.AbstractAzureFacetNode;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;
import java.util.Objects;

import static com.microsoft.azure.toolkit.ide.common.component.AzureResourceIconProvider.DEFAULT_AZURE_RESOURCE_ICON_PROVIDER;

public class ResourceConnectionLineMarkerInfo extends MergeableLineMarkerInfo<PsiElement> {
    @Getter
    private final Connection<? extends AzResource, ?> connection;

    public ResourceConnectionLineMarkerInfo(@Nonnull Connection<? extends AzResource, ?> connection, final AzureServiceResource<?> resource, @Nonnull PsiElement element) {
        super(element, element.getTextRange(),
                IntelliJAzureIcons.getIcon(DEFAULT_AZURE_RESOURCE_ICON_PROVIDER.getIcon(resource.getData())),
                ignore -> String.format("%s : %s", resource.getData().getModule().getResourceTypeName(), resource.getName()),
                null, null, GutterIconRenderer.Alignment.LEFT, () -> connection.getResource().getName());
        this.connection = connection;
    }

    @Override
    public boolean canMergeWith(@Nonnull MergeableLineMarkerInfo<?> info) {
        return info instanceof ResourceConnectionLineMarkerInfo &&
            Objects.equals(((ResourceConnectionLineMarkerInfo) info).connection, this.connection);
    }

    @Override
    public Icon getCommonIcon(@Nonnull List<? extends MergeableLineMarkerInfo<?>> infos) {
        return infos.stream()
            .filter(i -> i instanceof ResourceConnectionLineMarkerInfo)
            .map(i -> (ResourceConnectionLineMarkerInfo) i)
            .map(ResourceConnectionLineMarkerInfo::getIcon)
            .findFirst().orElse(null);
    }

    @Override
    public GutterIconRenderer createGutterRenderer() {
        return new ResourceConnectionGutterIconRender(this, connection);
    }

    private static class ResourceConnectionGutterIconRender extends LineMarkerInfo.LineMarkerGutterIconRenderer<PsiElement> {
        private final Connection<? extends AzResource, ?> connection;
        @Getter
        private AnAction clickAction;
        private AnAction editConnectionAction;
        private AnAction editEnvAction;
        @Getter
        private ActionGroup popupMenuActions;

        public ResourceConnectionGutterIconRender(@Nonnull final ResourceConnectionLineMarkerInfo info, @Nonnull final Connection<? extends AzResource, ?> connection) {
            super(info);
            this.connection = connection;
            this.initActions();
        }

        private void initActions() {
            this.clickAction = new AnAction("Navigate", "Navigate to resource in explorer", this.getIcon()) {
                @Override
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    if (Objects.isNull(e.getProject()) || e.getProject().isDisposed()) {
                        return;
                    }
                    AbstractAzureFacetNode.selectConnectedResource(connection, true);
                }
            };
            this.editConnectionAction = new AnAction("Edit Connection", "Edit resource connection for current resource", AllIcons.Actions.Edit) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    AzureActionManager.getInstance().getAction(ResourceConnectionActionsContributor.EDIT_CONNECTION).handle(connection, e);
                }
            };
            this.editEnvAction = new AnAction("Edit Environment Variables", "Edit environment variables for current resource", AllIcons.Actions.Edit) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    AzureActionManager.getInstance().getAction(ResourceConnectionActionsContributor.EDIT_ENV_FILE_IN_EDITOR).handle(connection, e);
                }
            };
            this.popupMenuActions = new ActionGroup() {
                @Override
                public AnAction[] getChildren(@Nullable AnActionEvent e) {
                    return new AnAction[]{editConnectionAction, editEnvAction};
                }
            };
        }

        @Override
        public boolean isNavigateAction() {
            return true;
        }
    }
}
