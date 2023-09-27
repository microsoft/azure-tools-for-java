/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.projectexplorer;

import com.intellij.ide.projectView.NodeSortOrder;
import com.intellij.ide.projectView.NodeSortSettings;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.tree.LeafState;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.component.TreeUtils;
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
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.common.action.IntellijActionsContributor.ACTIONS_DEPLOY_TO_AZURE;
import static com.microsoft.azure.toolkit.intellij.connector.ConnectionTopics.CONNECTION_CHANGED;
import static com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionActionsContributor.CONNECT_TO_MODULE;
import static com.microsoft.azure.toolkit.intellij.connector.projectexplorer.AbstractAzureFacetNode.FOCUS_KEY;

@Slf4j
public class AzureFacetRootNode extends AbstractProjectNode<AzureModule> implements IAzureFacetNode {
    private final AzureEventBus.EventListener eventListener;

    public AzureFacetRootNode(final AzureModule module, ViewSettings settings) {
        super(module.getProject(), module, settings);
        this.eventListener = new AzureEventBus.EventListener(this::onEvent);
        AzureEventBus.on("account.logged_in.account", eventListener);
        AzureEventBus.on("account.logged_out.account", eventListener);
        AzureEventBus.on("connector.refreshed.module_root", eventListener);
        final MessageBusConnection connection = module.getProject().getMessageBus().connect();
        connection.subscribe(CONNECTION_CHANGED, (ConnectionTopics.ConnectionChanged) (project, conn, action) -> {
            final JTree tree = this.getTree();
            if (conn.getConsumer().getId().equalsIgnoreCase(module.getName()) && tree != null) {
                TreeUtils.addClientProperty(tree, FOCUS_KEY, conn);
                updateChildren();
                AzureTaskManager.getInstance().runLater(() -> expandChild(1));
            }
        });
        connection.subscribe(DeploymentTargetTopics.TARGET_APP_CHANGED, (DeploymentTargetTopics.TargetAppChanged) (m, app, action) -> {
            final JTree tree = this.getTree();
            if (m.getName().equalsIgnoreCase(module.getName()) && tree != null) {
                TreeUtils.addClientProperty(tree, FOCUS_KEY, app);
                updateChildren();
                AzureTaskManager.getInstance().runLater(() -> expandChild(0));
            }
        });
    }

    private void expandChild(int childIndex) {
        this.expandNode(this.getPath()).thenAsync(p -> {
            final DefaultMutableTreeNode thisNode = (DefaultMutableTreeNode) p.getLastPathComponent();
            final DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) thisNode.getChildAt(childIndex);
            if (Objects.nonNull(childNode)) {
                final TreePath childNodePath = new TreePath(childNode.getPath());
                return this.expandNode(childNodePath);
            }
            return this.expandNode(p);
        });
    }

    private void onEvent(@Nonnull final AzureEvent azureEvent) {
        switch (azureEvent.getType()) {
            case "account.logged_in.account":
            case "account.logged_out.account":
                this.updateChildren();
                break;
            case "connector.refreshed.module_root":
                if (Objects.equals(azureEvent.getSource(), this.getValue())) {
                    this.updateChildren();
                }
                break;
            default:
                break;
        }
    }

    public Collection<? extends AbstractAzureFacetNode<?>> buildChildren() {
        final ArrayList<AbstractAzureFacetNode<?>> nodes = new ArrayList<>();
        final AzureModule module = this.getValue();
        final Profile profile = module.getDefaultProfile();
        if (Objects.isNull(profile)) {
            nodes.add(new ActionNode<>(this.getProject(), CONNECT_TO_MODULE, module));
            nodes.add(new ActionNode<>(this.getProject(), Action.Id.of(ACTIONS_DEPLOY_TO_AZURE), module.getModule()));
            return nodes;
        }
        nodes.add(new DeploymentTargetsNode(this.getProject(), profile.getDeploymentTargetManager()));
        nodes.add(new ConnectionsNode(this.getProject(), profile.getConnectionManager()));
        return nodes;
    }

    @Override
    protected void buildView(@Nonnull final PresentationData presentation) {
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
    public String toString() {
        return "Azure";
    }

    @Override
    public void dispose() {
        super.dispose();
        AzureEventBus.off("account.logged_in.account", eventListener);
        AzureEventBus.off("account.logged_out.account", eventListener);
        AzureEventBus.off("connector.refreshed.module_root", eventListener);
    }

    @Override
    public @Nonnull LeafState getLeafState() {
        return LeafState.NEVER;
    }
}
