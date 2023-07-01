/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.intellij.ide.projectView.*;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionTopics;
import com.microsoft.azure.toolkit.intellij.connector.DeploymentTargetTopics;
import com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.microsoft.azure.toolkit.intellij.connector.ConnectionTopics.CONNECTION_CHANGED;
import static com.microsoft.azure.toolkit.intellij.connector.DeploymentTargetTopics.TARGET_APP_CHANGED;

@Slf4j
public class AzureFacetRootNode extends ProjectViewNode<AzureModule> implements IAzureFacetNode {
    @Getter
    @Setter
    private boolean disposed;
    private final AzureEventBus.EventListener eventListener;

    public AzureFacetRootNode(final AzureModule module, ViewSettings settings) {
        super(module.getProject(), module, settings);
        this.eventListener = new AzureEventBus.EventListener(this::onEvent);
        AzureEventBus.on("account.logged_in.account", eventListener);
        AzureEventBus.on("connector.refreshed.module_root", eventListener);
        final MessageBusConnection connection = module.getProject().getMessageBus().connect();
        connection.subscribe(CONNECTION_CHANGED, (ConnectionTopics.ConnectionChanged) (p, conn, action) -> {
            if (conn.getConsumer().getId().equalsIgnoreCase(module.getName())) {
                rerender(true);
            }
        });
        connection.subscribe(TARGET_APP_CHANGED, (DeploymentTargetTopics.TargetAppChanged) (m, app, action) -> {
            if (m.getName().equalsIgnoreCase(module.getName())) {
                rerender(true);
            }
        });
    }

    private void onEvent(@Nonnull final AzureEvent azureEvent) {
        switch (azureEvent.getType()) {
            case "account.logged_in.account" -> this.rerender(true);
            case "connector.refreshed.module_root" -> {
                if (Objects.equals(azureEvent.getSource(), this.getValue())) {
                    this.rerender(true);
                }
            }
            default -> {
            }
        }
    }

    @Override
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
        final AzureModule module = this.getValue();
        final Profile profile = module.getDefaultProfile();
        if (this.isDisposed() || Objects.isNull(profile)) {
            return Collections.emptyList();
        }
//        final ArrayList<AbstractTreeNode<?>> result = new ArrayList<>();
//        final List<Connection<?, ?>> connections = Optional.ofNullable(module.getDefaultProfile()).map(Profile::getConnections).orElse(Collections.emptyList());
//        final List<String> appIds = Optional.ofNullable(module.getDefaultProfile()).map(Profile::getTargetAppIds).orElse(Collections.emptyList());
//        if (CollectionUtils.isNotEmpty(connections)) { // add back .azure file node
//            final VirtualFile virtualFile = Optional.ofNullable(getValue()).map(AzureModule::getDotAzureDir).flatMap(op -> op).orElse(null);
//            Optional.ofNullable(virtualFile)
//                .map(dir -> PsiManagerEx.getInstanceEx(getProject()).findDirectory(dir))
//                .map(dir -> new PsiDirectoryNode(getProject(), dir, viewSettings)).ifPresent(result::add);
//        }
//        if (!appIds.isEmpty()) {
//            result.add(new DeploymentTargetsNode(module));
//        }
//        result.add(new ConnectionsNode(module));
        return new ConnectionsNode(this.getProject(), profile.getConnectionManager()).getChildren();
    }

    @Override
    protected void update(@Nonnull final PresentationData presentation) {
        try {
            final AzureModule module = getValue();
            final List<Connection<?, ?>> connections = Optional.ofNullable(module.getDefaultProfile())
                .map(Profile::getConnections).orElse(Collections.emptyList());
            final boolean connected = CollectionUtils.isNotEmpty(connections);
            final boolean isConnectionValid = connections.stream().allMatch(Connection::isValidConnection);
            presentation.addText("Azure", getTextAttributes(isConnectionValid));
            presentation.setTooltip(isConnectionValid ? "Manage connected Azure resources here." : "Invalid connections found.");
            presentation.setIcon(connected ? IntelliJAzureIcons.getIcon("/icons/Common/AzureResourceConnector.svg") : IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE));
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    public static SimpleTextAttributes getTextAttributes(boolean isValid) {
        final SimpleTextAttributes regularAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
        return isValid ? regularAttributes : new SimpleTextAttributes(regularAttributes.getBgColor(),
                regularAttributes.getFgColor(), JBUI.CurrentTheme.Focus.warningColor(true), SimpleTextAttributes.STYLE_WAVED);
    }

    @Override
    @Nullable
    public Object getData(@Nonnull String dataId) {
        if (StringUtils.equalsIgnoreCase(dataId, Action.SOURCE)) {
            return this.getValue();
        } else if (StringUtils.equalsIgnoreCase(dataId, CommonDataKeys.VIRTUAL_FILE.getName())) {
            return Optional.ofNullable(getValue()).map(AzureModule::getDotAzureDir).flatMap(op -> op).orElse(null);
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public IActionGroup getActionGroup() {
        return AzureActionManager.getInstance().getGroup(ResourceConnectionActionsContributor.EXPLORER_MODULE_ROOT_ACTIONS);
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public NodeSortOrder getSortOrder(NodeSortSettings settings) {
        return NodeSortOrder.FOLDER;
    }

    @Override
    public boolean contains(VirtualFile file) {
        return false;
    }

    @Override
    public String toString() {
        return "Azure";
    }

    @Override
    public void dispose() {
        IAzureFacetNode.super.dispose();
        AzureEventBus.off("account.logged_in.account", eventListener);
        AzureEventBus.off("connector.refreshed.module_root", eventListener);
    }
}
