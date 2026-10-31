/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.integration.services;

import com.intellij.execution.services.ServiceViewDescriptor;
import com.intellij.ide.DataManager;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.microsoft.azure.toolkit.ide.common.component.ActionNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.component.TreeUtils;
import com.microsoft.azure.toolkit.intellij.explorer.TypeGroupedServicesRootNode;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.integration.services.AzureResourceActionsContributor.REMOVE_RESOURCE;

@RequiredArgsConstructor
public class NodeViewDescriptor implements ServiceViewDescriptor {
    private final Project project;
    private final Node<?> node;
    private final NodeViewContributor contributor;

    @Override
    public @Nonnull ItemPresentation getPresentation() {
        final Node.View view = node.getView();
        final PresentationData presentation = new PresentationData();
        if (Objects.nonNull(view.getIcon())) {
            presentation.setIcon(IntelliJAzureIcons.getIcon(view.getIcon()));
        }
        if (node instanceof ActionNode<?>) {
            final SimpleTextAttributes attributes = view.isEnabled() && view.isVisible() ?
                SimpleTextAttributes.LINK_ATTRIBUTES : SimpleTextAttributes.GRAYED_ATTRIBUTES;
            presentation.addText(StringUtils.capitalize(view.getLabel()), attributes);
            presentation.setTooltip(view.getDescription());
            presentation.addText(" (Double click)", SimpleTextAttributes.GRAYED_ATTRIBUTES);
        } else {
            final SimpleTextAttributes attributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
            presentation.addText(view.getLabel(), attributes);
            presentation.setTooltip(view.getTips());
            Optional.ofNullable(view.getDescription()).ifPresent(d -> presentation.addText(" " + d, SimpleTextAttributes.GRAYED_ATTRIBUTES));
        }
        return presentation;
    }

    @Override
    public @Nullable ActionGroup getPopupActions() {
        return Objects.nonNull(node.getActions()) ? TreeUtils.toIntellijActionGroup(node.getActions()) : null;
    }

    @Override
    public @Nullable ActionGroup getToolbarActions() {
        if (this.node instanceof TypeGroupedServicesRootNode) {
            return Objects.nonNull(node.getActions()) ? TreeUtils.toIntellijActionGroup(node.getActions()) : null;
        } else if (this.contributor.isService() && !(node instanceof ActionNode<?>)) {
            return TreeUtils.toIntellijActionGroup(new com.microsoft.azure.toolkit.lib.common.action.ActionGroup(REMOVE_RESOURCE));
        }
        return null;
    }

    @Override
    public @Nullable DataProvider getDataProvider() {
        return dataId -> {
            if (StringUtils.equalsIgnoreCase(dataId, Action.SOURCE)) {
                return Optional.ofNullable(node).map(Node::getValue).orElse(null);
            }
            return null;
        };
    }

    @Override
    public boolean handleDoubleClick(@Nonnull final MouseEvent e) {
        final DataContext context = DataManager.getInstance().getDataContext(e.getComponent());
        final AnActionEvent event = AnActionEvent.createEvent(context, new Presentation(), "ServicesNode.click", ActionUiKind.NONE, e);
        if (node instanceof ActionNode<?>) {
            this.node.click(event);
        } else {
            this.node.doubleClick(event);
        }
        return true;
    }
}
